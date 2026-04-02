Feature: REPL Rate Limiting
  As a site owner
  I want the REPL to enforce rate limits
  So that visitors cannot abuse the evaluation endpoint

  Background:
    Given I am on the REPL page

  Scenario: Exceeding 30 evals per minute triggers rate limit
    When I send 31 rapid evaluations via direct POST
    Then the last response should have status 429

  Scenario: Normal usage does not trigger rate limiting
    When I evaluate 3 expressions normally
    Then no request should be rate limited
