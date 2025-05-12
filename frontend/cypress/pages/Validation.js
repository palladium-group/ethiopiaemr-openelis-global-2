class Validation {
  constructor() {
    this.selectors = {
      heading: "section > h3",
      unitType: "#unitType",
      accessionNumber: "#accessionNumber",
      notesInput: "#cell-notes-0",
      datePickerInput: ".cds--date-picker__input",
      searchButton: "[data-testid='Search-btn']",
      saveButton: "[data-testid='Save-btn']",
      saveAllResultsButton: "Save All Results",
      helpIcon: "#user-Help",
      sampleInfo: "[data-testid='sampleInfo']",
    };
  }

  checkForHeading() {
    cy.get(this.selectors.heading, { timeout: 15000 }).should(
      "contain.text",
      "Validation",
    );
  }

  selectTestUnit(unitType) {
    cy.get(this.selectors.unitType).select(unitType);
  }

  validateTestUnit(unitType) {
    cy.get(this.selectors.sampleInfo).should("contain.text", unitType);
  }

  enterLabNumberAndSearch(labNo) {
    cy.get(this.selectors.accessionNumber).type(labNo);
    cy.get(this.selectors.searchButton).click();
  }

  saveAllResults() {
    cy.contains("span", this.selectors.saveAllResultsButton).click();
  }

  typeNotes() {
    cy.get(this.selectors.notesInput).type("Test Notes");
  }

  saveResults() {
    cy.get(this.selectors.saveButton).click();
  }

  enterDateAndSearch(date) {
    cy.get(this.selectors.datePickerInput).type(date);
    //clicking the help icon helps remove the calender cover
    cy.get(this.selectors.helpIcon).click();
    cy.get(this.selectors.helpIcon).click();
    cy.get(this.selectors.searchButton).click();
  }
}

export default Validation;
