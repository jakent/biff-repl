Feature: REPL Core Evaluation
  As a visitor to the Clojure REPL
  I want to evaluate Clojure expressions
  So that I can experiment with the language interactively

  Background:
    Given I am on the REPL page

  Scenario: Evaluate simple arithmetic
    When I evaluate "(+ 1 2 3)"
    Then I should see the result "6"

  Scenario: Evaluate string operations
    When I evaluate "(str \"hello\" \" \" \"world\")"
    Then I should see the result "hello world"

  Scenario: Evaluate collection operations
    When I evaluate "(map inc [1 2 3])"
    Then I should see the result "(2 3 4)"

  Scenario: Invalid syntax shows error
    When I evaluate "(+ 1 2"
    Then I should see an error

  Scenario: Runtime exception shows error
    When I evaluate "(/ 1 0)"
    Then I should see an error

  Scenario: Empty input produces no result
    When I type "" in the input
    And I click the Eval button
    Then no new result should appear

  Scenario: Multiple sequential evaluations
    When I evaluate "(+ 1 1)"
    And I evaluate "(+ 2 2)"
    Then I should see the result "4"
    And I should see the code echo "> (+ 1 1)"
