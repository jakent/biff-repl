Feature: REPL Sandbox Security
  As a site owner
  I want the REPL sandbox to block dangerous operations
  So that visitors cannot access the filesystem, network, or JVM internals

  Background:
    Given I am on the REPL page

  Scenario: slurp is blocked and no file contents are leaked
    When I evaluate "(slurp \"/etc/passwd\")"
    Then I should see an error
    And I should not see any file contents

  Scenario: spit is blocked
    When I evaluate "(spit \"/tmp/hack.txt\" \"pwned\")"
    Then I should see an error

  Scenario: System/exit is blocked and REPL stays alive
    When I evaluate "(System/exit 0)"
    Then I should see an error
    And the REPL should still be responsive

  Scenario: System/getenv is blocked
    When I evaluate "(System/getenv \"PATH\")"
    Then I should see an error
    And no environment variable should be leaked

  Scenario: System/getProperty is blocked
    When I evaluate "(System/getProperty \"user.home\")"
    Then I should see an error

  Scenario: eval is blocked
    When I evaluate "(eval '(+ 1 2))"
    Then I should see an error

  Scenario: Runtime/getRuntime is blocked
    When I evaluate "(Runtime/getRuntime)"
    Then I should see an error

  Scenario: Thread/sleep is blocked
    When I evaluate "(Thread/sleep 1000)"
    Then I should see an error

  Scenario: future is blocked
    When I evaluate "(future (+ 1 2))"
    Then I should see an error

  Scenario: agent is blocked
    When I evaluate "(agent 0)"
    Then I should see an error

  Scenario: Server remains functional after all blocked operations
    When I evaluate "(slurp \"/etc/passwd\")"
    And I evaluate "(System/exit 0)"
    And I evaluate "(eval '(+ 1 2))"
    Then the REPL should still be responsive
