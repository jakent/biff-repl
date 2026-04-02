Feature: REPL UI Interactions
  As a visitor to the Clojure REPL
  I want the UI to behave responsively
  So that I have a smooth interactive experience

  Background:
    Given I am on the REPL page

  Scenario: Page loads with welcome message and focused textarea
    Then I should see a welcome message
    And the textarea should be focused

  Scenario: Ctrl+Enter keyboard shortcut submits code
    When I type "(+ 10 20)" in the input
    And I press Ctrl+Enter
    Then I should see the result "30"

  Scenario: Input is cleared after eval
    When I evaluate "(+ 1 2)"
    Then the textarea should be empty

  Scenario: Code echo appears before result
    When I evaluate "(+ 1 2)"
    Then I should see the code echo "> (+ 1 2)"

  Scenario: Output auto-scrolls to bottom on new results
    When I evaluate "(+ 1 1)"
    And I evaluate "(+ 2 2)"
    And I evaluate "(+ 3 3)"
    Then the output should be scrolled to the bottom

  Scenario: Page title contains REPL
    Then the page title should contain "REPL"
