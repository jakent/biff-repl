(ns biff_repl.repl-commands
  "Custom REPL commands available to users in the web REPL."
  (:require [clojure.string :as str]))

(def ^:private command-registry
  "Registry of all available commands with their metadata."
  (atom {}))

(defmacro defcommand
  "Defines a REPL command with metadata for help text.

   Usage:
   (defcommand my-cmd
     {:doc \"Description of command\"
      :usage \"(my-cmd arg1 arg2)\"}
     [arg1 arg2]
     (body...))"
  [name opts args & body]
  `(do
     (defn ~name ~args ~@body)
     (swap! command-registry assoc
            '~name {:fn ~name
                    :doc ~(:doc opts)
                    :usage ~(:usage opts)
                    :source ~(pr-str (list* 'defn name args body))})))

(defcommand help
  {:doc "Display available commands and their descriptions"
   :usage "(help)"}
  []
  (str "Available commands:\n\n"
       (str/join "\n"
                 (for [[cmd-name {:keys [doc usage]}] (sort-by key @command-registry)]
                   (str "  " usage "\n    " doc)))
       "\n\nYou can also use standard Clojure expressions:\n"
       "  (+ 1 2 3)           ; arithmetic\n"
       "  (map inc [1 2 3])   ; collections\n"
       "  (str/upper-case \"hello\")  ; clojure.string"))

(defcommand about-me
  {:doc "Display information about the REPL environment"
   :usage "(about-me)"}
  []
  {:interpreter "SCI (Small Clojure Interpreter)"
   :version "0.8.42"
   :allowed-namespaces ["clojure.core" "clojure.string" "clojure.set"
                        "clojure.walk" "clojure.edn"]
   :rate-limit "30 evaluations per minute"
   :timeout "2 seconds per evaluation"
   :author "Your Name Here"
   :source "https://github.com/yourusername/biff-repl"})

(defcommand source
  {:doc "Show the source code of a REPL command"
   :usage "(source cmd-name)"}
  [cmd-name]
  (if-let [{:keys [source]} (get @command-registry cmd-name)]
    source
    (str "No source available for: " cmd-name)))

(defcommand projects
  {:doc "List featured projects and demos"
   :usage "(projects)"}
  []
  [{:name "Web REPL"
    :description "This interactive Clojure REPL running in your browser"
    :tech ["Clojure" "SCI" "HTMX" "CodeMirror"]}
   {:name "More coming soon..."
    :description "Check back for additional projects"
    :tech []}])

(defcommand random-quote
  {:doc "Get a random programming quote"
   :usage "(random-quote)"}
  []
  (let [quotes ["\"Programs must be written for people to read, and only incidentally for machines to execute.\" - Harold Abelson"
                "\"Simplicity is prerequisite for reliability.\" - Edsger Dijkstra"
                "\"First, solve the problem. Then, write the code.\" - John Johnson"
                "\"Code is like humor. When you have to explain it, it's bad.\" - Cory House"
                "\"The best error message is the one that never shows up.\" - Thomas Fuchs"
                "\"In Clojure, we don't have objects. We have data, functions, and state.\" - Stuart Halloway"
                "\"Make it work, make it right, make it fast.\" - Kent Beck"]]
    (rand-nth quotes)))

(defcommand fibonacci
  {:doc "Calculate the nth Fibonacci number (n <= 40)"
   :usage "(fibonacci n)"}
  [n]
  (when (or (not (integer? n)) (< n 0) (> n 40))
    (throw (ex-info "n must be an integer between 0 and 40" {:n n})))
  (loop [a 0 b 1 i n]
    (if (zero? i)
      a
      (recur b (+ a b) (dec i)))))

(defcommand factorial
  {:doc "Calculate the factorial of n (n <= 20)"
   :usage "(factorial n)"}
  [n]
  (when (or (not (integer? n)) (< n 0) (> n 20))
    (throw (ex-info "n must be an integer between 0 and 20" {:n n})))
  (reduce * 1 (range 1 (inc n))))

(def commands-ns
  "Map of command symbols to their functions for use in SCI sandbox."
  {'help help
   'about-me about-me
   'source source
   'projects projects
   'random-quote random-quote
   'fibonacci fibonacci
   'factorial factorial})
