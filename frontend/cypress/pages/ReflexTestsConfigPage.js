class ReflexTestsConfigPage {
  constructor() {
    this.selectors = {
      ruleName: '[id="0_rulename"]',
      toggleSwitch: ".cds--toggle__switch",
      validateToggle: ".cds--toggle__text",
      overAllOption: '[id="0 _overall"]',
    };
  }

  verifyPageLoads() {
    cy.contains("h2", "Reflex Tests Management").should("be.visible");
  }
}
export default ReflexTestsConfigPage;
