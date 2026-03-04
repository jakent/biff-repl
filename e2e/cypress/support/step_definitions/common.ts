import { Given, When, Then } from "@badeball/cypress-cucumber-preprocessor";

// ── Navigation ──────────────────────────────────

Given("I am on the REPL page", () => {
  cy.visit("/repl");
});

// ── Evaluation ──────────────────────────────────

When("I evaluate {string}", (code: string) => {
  cy.evalCode(code);
});

When("I type {string} in the input", (code: string) => {
  cy.get("#code-input").clear().type(code, { parseSpecialCharSequences: false });
});

When("I click the Eval button", () => {
  cy.get('button[type="submit"]').click();
});

When("I submit the form", () => {
  cy.get('button[type="submit"]').click();
});

// ── Result assertions ───────────────────────────

Then("I should see the result {string}", (expected: string) => {
  cy.lastResult().find(".result, .text-green-300").should("contain.text", expected);
});

Then("I should see an error", () => {
  cy.lastResult().find(".error, .text-red-400").should("exist");
});

Then("I should see an error containing {string}", (text: string) => {
  cy.lastResult().find(".error, .text-red-400").should("contain.text", text);
});

Then("I should not see any file contents", () => {
  cy.lastResult().should("not.contain.text", "root:");
  cy.lastResult().should("not.contain.text", "/bin/");
});

Then("I should see the code echo {string}", (echo: string) => {
  cy.lastResult().find(".text-gray-400, .code-input").should("contain.text", echo);
});

Then("the last result should contain {string}", (text: string) => {
  cy.lastResult().should("contain.text", text);
});

Then("the result should not be empty", () => {
  cy.lastResult().find(".result, .text-green-300").invoke("text").should("not.be.empty");
});

Then("no new result should appear", () => {
  // The welcome message is the only .result-entry
  cy.get("#repl-output .result-entry").should("have.length", 1);
});
