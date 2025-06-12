class ReflexTestsConfigPage {
  constructor() {
    this.selectors = {
      ruleName: '[id="0_rulename"]',
      toggleSwitch: ".cds--toggle__switch",
      validateToggle: ".cds--toggle__text",
      overAllOption: '[id="0_overall"]',
      selectSample: '[id="0_0_sample"]',
      searchTest: '[id="0_0_conditionTestId"]',
      relation: '[id="0_0_relation"]',
      numericValue: '[id="0_0_value"]',
      addIntNote: '[id="0_0_inote"]',
      addExtNote: '[id="0_0_xnote"]',
      reflexTest: '[id="0_0_reflexTestId"]',
      submit: '[id="submit_0"]',
      secondSample: "[data-cy='selectSample']",
      addRule: "[data-cy='rule']",
      autosuggestion: ".suggestion-active",
    };
  }

  verifyPageLoads() {
    cy.contains("h2", "Reflex Tests Management").should("be.visible");
  }

  enterRuleName(value) {
    cy.get(this.selectors.ruleName).type(value);
  }

  validateRuleName(value) {
    cy.contains(this.selectors.ruleName, value);
  }

  validateToggleStatus(value) {
    cy.contains(this.selectors.validateToggle, value);
  }

  selectOverAllOptions(value) {
    cy.get(this.selectors.overAllOption).select(value);
  }

  selectSample(value) {
    cy.get(this.selectors.selectSample).select(value);
  }

  searchTest(value) {
    cy.get(this.selectors.searchTest).type(value);
    cy.contains(this.selectors.autosuggestion, value).click();
  }

  selectRelation(value) {
    cy.get(this.selectors.relation).select(value);
  }

  enterNumericValue(value) {
    cy.get(this.selectors.numericValue).type(value);
  }

  selectSecondSample(value) {
    cy.get(this.selectors.secondSample).select(value);
  }

  searchReflexTest(value) {
    cy.get(this.selectors.reflexTest).type(value);
  }

  addInternatNote(value) {
    cy.get(this.selectors.addIntNote).type(value);
  }

  addExternatNote(value) {
    cy.get(this.selectors.addExtNote).type(value);
  }

  submitButton() {
    cy.get(this.selectors.submit).click();
  }
}
export default ReflexTestsConfigPage;
