(ns tasks
  (:require [com.biffweb.tasks :as tasks]
            [clojure.test :as t]))

(defn hello
  "Says 'Hello'"
  []
  (println "Hello"))

(defn run-tests
  "Runs all tests"
  []
  (require 'biff_repl-test
           'biff_repl.sandbox-test)
  (let [result (t/run-tests 'biff_repl-test 'biff_repl.sandbox-test)]
    (when (or (pos? (:fail result)) (pos? (:error result)))
      (System/exit 1))))

;; Tasks should be vars (#'hello instead of hello) so that `clj -M:dev help` can
;; print their docstrings.
(def custom-tasks
  {"hello" #'hello
   "test" #'run-tests})

(def tasks (merge tasks/tasks custom-tasks))
