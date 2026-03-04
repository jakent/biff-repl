import { Given, When, Then } from "@badeball/cypress-cucumber-preprocessor";

Then("the page title should contain {string}", (text: string) => {
  cy.title().should("contain", text);
});

Then("I should see a welcome message", () => {
  cy.get("#repl-output").should("contain.text", "Welcome to the Clojure REPL");
});

Then("the textarea should be focused", () => {
  cy.get("#code-input").should("have.focus");
});

When("I press Ctrl+Enter", () => {
  cy.get("#code-input").type("{ctrl}{enter}");
});

Then("the textarea should be empty", () => {
  cy.get("#code-input").should("have.value", "");
});

Then("the textarea should have focus", () => {
  cy.get("#code-input").should("have.focus");
});

Then("the output should be scrolled to the bottom", () => {
  cy.get("#repl-output").then(($el) => {
    const el = $el[0];
    // scrollTop + clientHeight should be near scrollHeight
    expect(el.scrollTop + el.clientHeight).to.be.closeTo(el.scrollHeight, 5);
  });
});
