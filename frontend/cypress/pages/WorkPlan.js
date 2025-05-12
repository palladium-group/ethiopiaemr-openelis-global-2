class WorkPlan {
  constructor() {
    this.selectors = {
      filterTitle: "h3",
      dropdown: "select#select-1",
      printButton: "button",
      accessionLink: "a.cds--link",
      resultsTable: '[data-cy="workplanResultsTable"]',
    };
  }

  visit() {
    cy.visit("/WorkplanByTest");
  }

  getWorkPlanFilterTitle(tiles) {
    cy.contains(this.selectors.filterTitle, tiles).should("be.visible");
  }

  selectDropdownOption(option) {
    cy.get(this.selectors.dropdown).should("be.visible").select(option);
  }

  getPrintWorkPlanButton() {
    cy.contains(this.selectors.printButton, "Print Workplan").should(
      "be.visible",
    );
  }

  clickAccessionNumber(labNo) {
    cy.contains(this.selectors.accessionLink, labNo).click();
  }

  getWorkPlanResultsTable() {
    return cy.get(this.selectors.resultsTable);
  }
}

export default WorkPlan;
