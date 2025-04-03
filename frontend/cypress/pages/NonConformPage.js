class NonConform {
  // Centralized selectors
  selectors = {
    title: "h2",
    searchType: "#type",
    searchField: "[data-cy='fieldName']",
    searchButton: "[data-testid='nce-search-button']",
    searchResult: "[data-testid='nce-search-result']",
    nceNumberResult: "[data-testid='nce-number-result']",
    sampleCheckbox: "[data-testid='nce-sample-checkbox']",
    goToFormButton: "[data-testid='nce-goto-form-button']",
    startDate: "input#startDate",
    reportingUnits: "#reportingUnits",
    description: "#text-area-1",
    suspectedCause: "#text-area-2",
    correctiveAction: "#text-area-3",
    additionalTextArea: "#text-area-10",
    nceCategory: "#nceCategory",
    nceType: "#nceType",
    consequences: "#consequences",
    recurrence: "#recurrence",
    labComponent: "#labComponent",
    discussionDate: "#tdiscussionDate",
    proposedCorrectiveAction: "#text-area-corrective",
    dateCompleted: "#dateCompleted",
    dateCompleted0: ".cds--date-picker-input__wrapper > #dateCompleted-0",
    actionType:
      "div.cds--sm\\:col-span-3:nth-child(30) > div:nth-child(1) > input:nth-child(1)",
    resolutionOption:
      "div.cds--radio-button-wrapper:nth-child(1) > label:nth-child(2)",
    nceRadioButton:
      ".cds--data-table > tbody:nth-child(2) > tr:nth-child(1) > td:nth-child(1) > div:nth-child(1) > label:nth-child(2) > span:nth-child(1)",
    submitButton: "[data-testid='nce-submit-button']",
  };

  // Title validation (kept as-is per request)
  getReportNonConformTitle() {
    return cy.get(this.selectors.title);
  }

  getViewNonConformTitle() {
    return cy.get(this.selectors.title);
  }

  // Form interactions
  selectSearchType(type) {
    cy.get(this.selectors.searchType).select(type);
  }

  enterSearchField(value) {
    cy.get(this.selectors.searchField).type(value);
  }

  clickSearchButton() {
    cy.get(this.selectors.searchButton).should("be.visible").click();
  }

  // Search results validation
  validateSearchResult(expectedValue) {
    cy.get(this.selectors.searchResult)
      .first()
      .invoke("text")
      .should("eq", expectedValue);
  }

  validateLabNoSearchResult(labNo) {
    cy.get(this.selectors.searchResult).invoke("text").should("eq", labNo);
  }

  validateNCESearchResult(NCENo) {
    cy.get(this.selectors.nceNumberResult).invoke("text").should("eq", NCENo);
  }

  // Checkbox and navigation
  clickCheckbox() {
    cy.get(this.selectors.sampleCheckbox, { timeout: 12000 })
      .should("be.visible")
      .check({ force: true });
  }

  clickGoToNceFormButton() {
    cy.get(this.selectors.goToFormButton).should("be.visible").click();
  }

  // Form fields (preserve original IDs)
  enterStartDate(date) {
    cy.get(this.selectors.startDate, { timeout: 10000 }).type(date);
  }

  selectReportingUnit(unit) {
    cy.get(this.selectors.reportingUnits).select(unit);
  }

  enterDescription(description) {
    cy.get(this.selectors.description).type(description);
  }

  enterSuspectedCause(SuspectedCause) {
    cy.get(this.selectors.suspectedCause).type(SuspectedCause);
  }

  enterCorrectiveAction(correctiveaction) {
    cy.get(this.selectors.correctiveAction).type(correctiveaction);
  }

  // Dropdowns
  enterNceCategory(nceCategory) {
    cy.get(this.selectors.nceCategory).select(nceCategory);
  }

  enterNceType(nceType) {
    cy.get(this.selectors.nceType).select(nceType);
  }

  enterConsequences(consequences) {
    cy.get(this.selectors.consequences).select(consequences);
  }

  enterRecurrence(recurrence) {
    cy.get(this.selectors.recurrence).select(recurrence);
  }

  enterLabComponent(labComponent) {
    cy.get(this.selectors.labComponent).select(labComponent);
  }

  // Text areas
  enterDescriptionAndComments(testText) {
    cy.get(this.selectors.additionalTextArea).type(testText);
    cy.get(this.selectors.correctiveAction).type(testText);
    cy.get(this.selectors.suspectedCause).type(testText);
  }

  // Submission
  submitForm() {
    cy.get(this.selectors.submitButton).click();
  }

  // Corrective actions
  enterDiscussionDate(date) {
    cy.get(this.selectors.discussionDate).type(date);
  }

  enterProposedCorrectiveAction(action) {
    cy.get(this.selectors.proposedCorrectiveAction)
      .should("not.be.disabled")
      .type(action, { force: true });
  }

  enterDateCompleted(date) {
    cy.get(this.selectors.dateCompleted).type(date);
  }

  selectActionType() {
    cy.get(this.selectors.actionType).check({ force: true });
  }

  checkResolution() {
    cy.get(this.selectors.resolutionOption).click();
  }

  clickRadioButtonNCE() {
    cy.get(this.selectors.nceRadioButton).click({ force: true });
  }

  enterDateCompleted0(date) {
    cy.get(this.selectors.dateCompleted0).type(date);
  }

  clickSubmitButton() {
    cy.get(this.selectors.submitButton).should("be.visible").click();
  }

  // Data management
  getAndSaveNceNumber() {
    cy.get(this.selectors.nceNumberResult)
      .invoke("text")
      .then((text) => {
        cy.readFile("cypress/fixtures/NonConform.json").then((existingData) => {
          const newData = {
            ...existingData,
            NceNumber: text.trim(),
          };
          cy.writeFile("cypress/fixtures/NonConform.json", newData);
        });
      });
  }
}

export default NonConform;
