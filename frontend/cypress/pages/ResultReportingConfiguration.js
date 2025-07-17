class ResultReportingConfigurationPage {
  constructor() {
    this.selectors = {
      title: "h2",
      span: "span",
      save: "[data-cy='saveButton']",
      cancel: "[data-cy='cancelButton']",
    };
  }

  validatePageTitle() {
    cy.get(this.selectors.title)
      .should("be.visible")
      .and("contain.text", "Result Reporting Configuration");
  }

  clickEnable() {
    cy.contains(this.selectors.span, "Enabled").click({ multiple: true });
  }

  typeURL(urlIndex) {
    cy.get(`#url-${urlIndex}`).clear().type("https://192.168.92.101/openelis");
  }

  saveEntry() {
    cy.get(this.selectors.save).should("be.enabled").click();
  }
}

export default ResultReportingConfigurationPage;
