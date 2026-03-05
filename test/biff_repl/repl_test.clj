(ns biff_repl.repl-test
  "Tests for the balanced? function in the REPL handler."
  (:require [clojure.test :refer [deftest is testing]]
            [biff_repl.repl]))

(def balanced? @#'biff_repl.repl/balanced?)

;; Basic balanced cases

(deftest balanced-empty-string-test
  (is (true? (balanced? ""))))

(deftest balanced-simple-parens-test
  (testing "Simple balanced delimiters"
    (is (true? (balanced? "()")))
    (is (true? (balanced? "[]")))
    (is (true? (balanced? "{}")))
    (is (true? (balanced? "(+ 1 2)")))
    (is (true? (balanced? "[1 2 3]")))
    (is (true? (balanced? "{:a 1}")))))

(deftest balanced-nested-test
  (testing "Nested delimiters"
    (is (true? (balanced? "([])")))
    (is (true? (balanced? "([{}])")))
    (is (true? (balanced? "(defn foo [x] {:val x})")))
    (is (true? (balanced? "(let [a (+ 1 2)] [a (* a a)])")))))

(deftest balanced-multiline-test
  (testing "Multiline expressions"
    (is (true? (balanced? "(defn foo\n  [x]\n  (+ x 1))")))
    (is (true? (balanced? "(let [a 1\n      b 2]\n  (+ a b))")))))

(deftest balanced-no-delimiters-test
  (testing "Plain values with no delimiters"
    (is (true? (balanced? "42")))
    (is (true? (balanced? "hello")))
    (is (true? (balanced? ":keyword")))))

;; Unbalanced cases — open delimiters

(deftest unbalanced-open-paren-test
  (testing "Unclosed parens"
    (is (false? (balanced? "(")))
    (is (false? (balanced? "(+ 1")))
    (is (false? (balanced? "(defn foo")))
    (is (false? (balanced? "(defn foo [x]")))))

(deftest unbalanced-open-bracket-test
  (testing "Unclosed brackets"
    (is (false? (balanced? "[")))
    (is (false? (balanced? "[1 2 3")))))

(deftest unbalanced-open-brace-test
  (testing "Unclosed braces"
    (is (false? (balanced? "{")))
    (is (false? (balanced? "{:a 1")))))

;; Unbalanced cases — extra close delimiters

(deftest unbalanced-extra-close-test
  (testing "Extra closing delimiters"
    (is (false? (balanced? ")")))
    (is (false? (balanced? "]")))
    (is (false? (balanced? "}")))
    (is (false? (balanced? "(+ 1 2))")))
    (is (false? (balanced? "[]]]")))))

;; String context — delimiters inside strings should be ignored

(deftest balanced-string-context-test
  (testing "Delimiters inside strings are ignored"
    (is (true? (balanced? "(println \"(\")")))
    (is (true? (balanced? "(println \")\")")))
    (is (true? (balanced? "(println \"[{]})\")")))
    (is (true? (balanced? "(str \"unbalanced ( parens\")")))))

(deftest unbalanced-open-string-test
  (testing "Unclosed string literal"
    (is (false? (balanced? "(println \"hello)")))
    (is (false? (balanced? "\"just a string")))))

;; Escaped characters inside strings

(deftest balanced-escaped-quote-test
  (testing "Escaped quotes inside strings"
    (is (true? (balanced? "(println \"she said \\\"hi\\\"\")")))
    (is (true? (balanced? "(str \"a\\\"b\")")))))

(deftest balanced-escaped-backslash-test
  (testing "Escaped backslash before closing quote"
    (is (true? (balanced? "(str \"path\\\\\")")))))

;; Mixed complex cases

(deftest balanced-complex-expressions-test
  (testing "Complex real-world expressions"
    (is (true? (balanced? "(-> {:a [1 2]} :a (map inc))")))
    (is (true? (balanced? "(defn greet [name] (str \"Hello, \" name \"!\"))")))
    (is (true? (balanced? "(let [{:keys [a b]} {:a 1 :b 2}] (+ a b))")))))

(deftest unbalanced-mixed-test
  (testing "Mismatched delimiter types still count as unbalanced"
    (is (false? (balanced? "(]")))
    (is (false? (balanced? "[}")))))
