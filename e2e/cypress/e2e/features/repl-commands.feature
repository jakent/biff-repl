Feature: REPL Custom Commands
  As a visitor to the Clojure REPL
  I want to use custom built-in commands
  So that I can learn about the environment and explore demos

  Background:
    Given I am on the REPL page

  Scenario: help command lists available commands
    When I evaluate "(help)"
    Then the last result should contain "Available commands"

  Scenario: about-me command shows environment info
    When I evaluate "(about-me)"
    Then the last result should contain "SCI"

  Scenario: projects command lists featured projects
    When I evaluate "(projects)"
    Then the last result should contain "Web REPL"

  Scenario: fibonacci command calculates correctly
    When I evaluate "(fibonacci 10)"
    Then I should see the result "55"

  Scenario: fibonacci rejects values exceeding limit
    When I evaluate "(fibonacci 50)"
    Then I should see an error containing "40"

  Scenario: factorial command calculates correctly
    When I evaluate "(factorial 5)"
    Then I should see the result "120"

  Scenario: random-quote returns a non-empty result
    When I evaluate "(random-quote)"
    Then the result should not be empty
