declare global {
  namespace Cypress {
    interface Chainable {
      /**
       * Types code into the REPL textarea, clicks Eval, and waits for a new result.
       */
      evalCode(code: string): Chainable<void>;
      /**
       * Returns the last .result-entry element in #repl-output.
       */
      lastResult(): Chainable<JQuery<HTMLElement>>;
    }
  }
}

Cypress.Commands.add("evalCode", (code: string) => {
  // Count existing results so we can detect the new one
  cy.get("#repl-output .result-entry").then(($entries) => {
    const countBefore = $entries.length;

    cy.get("#code-input").clear().type(code, { parseSpecialCharSequences: false });
    cy.get('button[type="submit"]').click();

    // Wait for HTMX to append a new result entry
    cy.get("#repl-output .result-entry").should("have.length.greaterThan", countBefore);
  });
});

Cypress.Commands.add("lastResult", () => {
  return cy.get("#repl-output .result-entry").last();
});

export {};
