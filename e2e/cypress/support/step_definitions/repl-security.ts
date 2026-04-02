import { Then } from "@badeball/cypress-cucumber-preprocessor";

Then("the REPL should still be responsive", () => {
  // Verify the REPL still works after a blocked operation
  cy.evalCode("(+ 1 1)");
  cy.lastResult().find(".result, .text-green-300").should("contain.text", "2");
});

Then("no environment variable should be leaked", () => {
  cy.lastResult().should("not.contain.text", "/usr/");
  cy.lastResult().should("not.contain.text", "/bin");
  cy.lastResult().should("not.contain.text", "/home/");
});
