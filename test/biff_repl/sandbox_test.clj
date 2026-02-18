(ns biff_repl.sandbox-test
  "Security tests for the SCI sandbox.
   Verifies that safe operations work and dangerous operations are blocked."
  (:require [clojure.test :refer [deftest is testing]]
            [biff_repl.sandbox :as sandbox]
            [biff_repl.repl-commands :as commands]))

(def ctx (sandbox/create-context commands/commands-ns))

;; Helper to check if evaluation succeeds
(defn eval-ok? [code]
  (let [result (sandbox/eval-string ctx code)]
    (contains? result :result)))

;; Helper to check if evaluation returns an error
(defn eval-error? [code]
  (let [result (sandbox/eval-string ctx code)]
    (contains? result :error)))

;; Helper to get the result value
(defn eval-result [code]
  (:result (sandbox/eval-string ctx code)))

;; Safe Operations Tests

(deftest basic-arithmetic-test
  (testing "Basic arithmetic operations work"
    (is (= 6 (eval-result "(+ 1 2 3)")))
    (is (= 24 (eval-result "(* 2 3 4)")))
    (is (= 2 (eval-result "(- 10 8)")))
    (is (= 5 (eval-result "(/ 10 2)")))))

(deftest collection-operations-test
  (testing "Collection operations work"
    (is (= [2 3 4] (eval-result "(map inc [1 2 3])")))
    (is (= [2 4] (eval-result "(filter even? [1 2 3 4])")))
    (is (= 10 (eval-result "(reduce + [1 2 3 4])")))
    (is (= {:a 1 :b 2} (eval-result "{:a 1 :b 2}")))
    (is (= #{1 2 3} (eval-result "#{1 2 3}")))))

(deftest string-operations-test
  (testing "clojure.string operations work"
    (is (= "HELLO" (eval-result "(clojure.string/upper-case \"hello\")")))
    (is (= ["a" "b" "c"] (eval-result "(clojure.string/split \"a,b,c\" #\",\")")))
    (is (= "hello world" (eval-result "(clojure.string/join \" \" [\"hello\" \"world\"])")))))

(deftest set-operations-test
  (testing "clojure.set operations work"
    (is (= #{1 2 3 4} (eval-result "(clojure.set/union #{1 2} #{3 4})")))
    (is (= #{2} (eval-result "(clojure.set/intersection #{1 2} #{2 3})")))
    (is (= #{1} (eval-result "(clojure.set/difference #{1 2} #{2 3})")))))

(deftest walk-operations-test
  (testing "clojure.walk operations work"
    (is (= {:a 1 :b 2} (eval-result "(clojure.walk/keywordize-keys {\"a\" 1 \"b\" 2})")))
    (is (= [2 3 4] (eval-result "(clojure.walk/postwalk #(if (number? %) (inc %) %) [1 2 3])")))))

(deftest edn-operations-test
  (testing "clojure.edn read-string works"
    (is (= {:a 1} (eval-result "(clojure.edn/read-string \"{:a 1}\")")))
    (is (= [1 2 3] (eval-result "(clojure.edn/read-string \"[1 2 3]\")")))))

(deftest math-operations-test
  (testing "Math class methods work"
    (is (= 2.0 (eval-result "(Math/sqrt 4)")))
    (is (= 8.0 (eval-result "(Math/pow 2 3)")))
    (is (= 5 (eval-result "(Math/abs -5)")))))

;; Custom Commands Tests

(deftest help-command-test
  (testing "(help) command returns help text"
    (let [result (eval-result "(help)")]
      (is (string? result))
      (is (.contains result "Available commands")))))

(deftest about-me-command-test
  (testing "(about-me) command returns environment info"
    (let [result (eval-result "(about-me)")]
      (is (map? result))
      (is (contains? result :interpreter)))))

(deftest fibonacci-command-test
  (testing "(fibonacci n) calculates correctly"
    (is (= 0 (eval-result "(fibonacci 0)")))
    (is (= 1 (eval-result "(fibonacci 1)")))
    (is (= 55 (eval-result "(fibonacci 10)")))
    (is (eval-error? "(fibonacci 50)")))) ; Should error for n > 40

(deftest factorial-command-test
  (testing "(factorial n) calculates correctly"
    (is (= 1 (eval-result "(factorial 0)")))
    (is (= 120 (eval-result "(factorial 5)")))
    (is (eval-error? "(factorial 25)")))) ; Should error for n > 20

;; Dangerous Operations Tests (MUST be blocked)

(deftest file-io-blocked-test
  (testing "File I/O operations are blocked"
    (is (eval-error? "(slurp \"/etc/passwd\")"))
    (is (eval-error? "(spit \"/tmp/test.txt\" \"data\")"))
    (is (eval-error? "(load-file \"/etc/passwd\")"))))

(deftest system-access-blocked-test
  (testing "System access is blocked"
    (is (eval-error? "(System/exit 0)"))
    (is (eval-error? "(System/getenv \"PATH\")"))
    (is (eval-error? "(System/getProperty \"user.home\")"))))

(deftest eval-blocked-test
  (testing "eval and resolve are blocked"
    (is (eval-error? "(eval '(+ 1 2))"))
    (is (eval-error? "(resolve 'slurp)"))
    (is (eval-error? "(ns-resolve 'clojure.core 'slurp)"))))

(deftest concurrency-blocked-test
  (testing "Concurrency primitives are blocked"
    (is (eval-error? "(future (+ 1 2))"))
    (is (eval-error? "(agent 0)"))
    (is (eval-error? "(pmap inc [1 2 3])"))))

(deftest shell-access-blocked-test
  (testing "Shell access patterns are blocked"
    ;; These rely on the sandbox not having shell commands available
    (is (eval-error? "(Runtime/getRuntime)"))
    (is (eval-error? "(.exec (Runtime/getRuntime) \"ls\")"))))

(deftest thread-blocked-test
  (testing "Thread creation is blocked"
    (is (eval-error? "(Thread/sleep 1000)"))
    (is (eval-error? "(Thread. (fn [] nil))"))))

;; Error Handling Tests

(deftest syntax-error-test
  (testing "Syntax errors return error messages"
    (is (eval-error? "(+ 1 2"))
    (is (eval-error? "((("))))

(deftest runtime-error-test
  (testing "Runtime errors return error messages"
    (is (eval-error? "(/ 1 0)"))
    (is (eval-error? "(nth [] 10)"))))

;; Output Formatting Tests

(deftest format-result-test
  (testing "Results are formatted correctly"
    (is (= "42" (sandbox/format-result 42)))
    (is (= "\"hello\"" (sandbox/format-result "hello")))
    (is (= "[1 2 3]" (sandbox/format-result [1 2 3])))))

(deftest format-truncation-test
  (testing "Long results are truncated"
    (let [long-vec (vec (range 10000))
          result (sandbox/format-result long-vec)]
      (is (<= (count result) 10100))))) ; 10000 + some buffer
