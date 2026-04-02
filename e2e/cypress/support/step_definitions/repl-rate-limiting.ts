import { When, Then } from "@badeball/cypress-cucumber-preprocessor";

let lastStatusCode: number;

When("I send {int} rapid evaluations via direct POST", (count: number) => {
  // Extract the CSRF token from the form's hx-headers attribute
  cy.get("#repl-form")
    .invoke("attr", "hx-headers")
    .then((hxHeaders) => {
      const headers = JSON.parse(hxHeaders as string);
      const csrfToken = headers["x-csrf-token"];

      // Send evaluations sequentially to track the last status code
      const sendEval = (i: number): Cypress.Chainable => {
        return cy.request({
          method: "POST",
          url: "/repl/eval",
          form: true,
          body: { code: `(+ 1 ${i})` },
          headers: { "x-csrf-token": csrfToken },
          failOnStatusCode: false,
        }).then((response) => {
          lastStatusCode = response.status;
          if (i < count) {
            return sendEval(i + 1);
          }
        });
      };

      sendEval(1);
    });
});

When("I evaluate {int} expressions normally", (count: number) => {
  for (let i = 0; i < count; i++) {
    cy.evalCode(`(+ 1 ${i})`);
  }
});

Then("the last response should have status {int}", (status: number) => {
  expect(lastStatusCode).to.equal(status);
});

Then("no request should be rate limited", () => {
  // All evals should have produced successful results (green text)
  cy.get("#repl-output .result-entry .error, #repl-output .result-entry .text-red-400").should(
    "not.contain.text",
    "Rate limit"
  );
});
