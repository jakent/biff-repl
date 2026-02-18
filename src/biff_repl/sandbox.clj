(ns biff_repl.sandbox
  "Sandboxed Clojure evaluation using SCI (Small Clojure Interpreter).
   Provides a safe environment for executing user-submitted code."
  (:require [sci.core :as sci]
            [clojure.string :as str]
            [clojure.set :as set]
            [clojure.walk :as walk]
            [clojure.edn :as edn]))

(def ^:private timeout-ms 2000)

(defn- create-namespaces
  "Creates SCI namespace bindings for allowlisted namespaces."
  [commands-ns]
  {'clojure.string {'blank? str/blank?
                    'capitalize str/capitalize
                    'ends-with? str/ends-with?
                    'escape str/escape
                    'includes? str/includes?
                    'index-of str/index-of
                    'join str/join
                    'last-index-of str/last-index-of
                    'lower-case str/lower-case
                    'replace str/replace
                    'replace-first str/replace-first
                    'reverse str/reverse
                    'split str/split
                    'split-lines str/split-lines
                    'starts-with? str/starts-with?
                    'trim str/trim
                    'trim-newline str/trim-newline
                    'triml str/triml
                    'trimr str/trimr
                    'upper-case str/upper-case}
   'clojure.set {'difference set/difference
                 'index set/index
                 'intersection set/intersection
                 'join set/join
                 'map-invert set/map-invert
                 'project set/project
                 'rename set/rename
                 'rename-keys set/rename-keys
                 'select set/select
                 'subset? set/subset?
                 'superset? set/superset?
                 'union set/union}
   'clojure.walk {'walk walk/walk
                  'postwalk walk/postwalk
                  'prewalk walk/prewalk
                  'postwalk-replace walk/postwalk-replace
                  'prewalk-replace walk/prewalk-replace
                  'keywordize-keys walk/keywordize-keys
                  'stringify-keys walk/stringify-keys}
   'clojure.edn {'read-string edn/read-string}
   'user commands-ns})

(defn create-context
  "Creates a locked-down SCI context for safe code evaluation.

   The context allows:
   - Safe clojure.core functions (math, collections, sequences, etc.)
   - clojure.string, clojure.set, clojure.walk, clojure.edn
   - Custom REPL commands from commands-ns

   The context blocks:
   - File I/O (slurp, spit, etc.)
   - Shell access and system calls
   - eval, resolve, future, agent, and other meta functions
   - Java interop beyond safe classes
   - Reflection"
  [commands-ns]
  (sci/init
   {:namespaces (create-namespaces commands-ns)
    :bindings commands-ns
    :classes {'Math java.lang.Math
              'Integer java.lang.Integer
              'Long java.lang.Long
              'Double java.lang.Double
              'String java.lang.String
              'Boolean java.lang.Boolean}
    :deny ['slurp 'spit 'load-file 'load-reader 'load-string
           'eval 'resolve 'ns-resolve 'requiring-resolve
           'future 'future-call 'future-cancel 'future-cancelled? 'future-done? 'future?
           'agent 'send 'send-off 'send-via 'await 'await-for
           'pmap 'pcalls 'pvalues
           'ref 'ref-set 'alter 'commute 'ensure 'dosync
           'add-watch 'remove-watch
           'clojure.core/eval 'clojure.core/resolve 'clojure.core/ns-resolve
           'clojure.core/slurp 'clojure.core/spit
           '*ns* '*file* '*command-line-args*
           'shutdown-agents 'System/exit 'System/getenv 'System/getProperty
           'Runtime/getRuntime
           'Thread 'Thread/sleep]}))

(defn eval-string
  "Evaluates a string of Clojure code in the sandboxed SCI context.

   Arguments:
   - ctx: The SCI context created by create-context
   - code: String of Clojure code to evaluate

   Returns a map with either:
   - {:result <value>} on success
   - {:error <message>} on failure (syntax error, timeout, security violation, etc.)"
  [ctx code]
  (let [result (promise)
        eval-thread (Thread.
                     (fn []
                       (try
                         (deliver result {:result (sci/eval-string* ctx code)})
                         (catch Exception e
                           (deliver result {:error (or (ex-message e)
                                                       (str (type e)))})))))]
    (.start eval-thread)
    (if (deref result timeout-ms nil)
      @result
      (do
        (.interrupt eval-thread)
        {:error (str "Evaluation timed out after " timeout-ms "ms")}))))

(defn format-result
  "Formats a result value for display.
   Truncates very long output to prevent abuse."
  [result]
  (let [max-length 10000
        s (pr-str result)]
    (if (> (count s) max-length)
      (str (subs s 0 max-length) "\n... (output truncated)")
      s)))
