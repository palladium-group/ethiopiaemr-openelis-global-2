class MenuConfigPage {
  constructor() {
    this.selectors = {
      nonConformMenu: "#menu_nonconformity",
      nonConformReport: "#menu_non_conforming_report",
      nonConformView: "#menu_non_conforming_view",
      correctiveAction: "#menu_non_conforming_corrective_actions",
      toggleText: ".cds--toggle_text",
      toggleOn: "div.cds--toggle__switch",
      toggleOff: "div.cds--toggle label div > div",
    };
  }

  // This method is used to visit the page
  visit() {
    cy.visit("/administration#globalMenuManagement");
  }

  turnOffToggleSwitch() {
    cy.get(this.selectors.toggleOff).click();
  }

  turnOnToggleSwitch() {
    cy.get(this.selectors.toggleOn).should("be.visible").click();
  }

  validateToggleStatus(value) {
    cy.contains(this.selectors.toggleText, value);
  }

  validateNonConformOff() {
    cy.get(this.selectors.nonConformMenu).should("not.be.visible");
  }

  validateNonConformOn() {
    cy.get(this.selectors.nonConformMenu).should("be.visible").click();
    cy.get(this.selectors.nonConformReport).should("be.visible");
    cy.get(this.selectors.nonConformView).should("be.visible");
    cy.get(this.selectors.correctiveAction).should("be.visible");
  }

  submitButton() {
    cy.contains("button", "Submit").click();
  }

  checkMenuItem = function (menuItem) {
    // Map of menu items to their respective checkboxes
    const menuItems = {
      home: "#menu_home_checkbox",
      order: "#menu_sample_checkbox",
      immunoChem: "#menu_immunochem_checkbox",
      cytology: "#menu_cytology_checkbox",
      results: "#menu_results_checkbox",
      validation: "#menu_resultvalidation_checkbox",
      reports: "#menu_reports_checkbox",
      study: "#menu_reports_study_checkbox",
      billing: "#menu_billing_checkbox",
      admin: "#menu_administration_checkbox",
      help: "#menu_help_checkbox",
      patient: "#menu_patient_checkbox",
      nonConform: "#menu_nonconformity_checkbox",
      reportNCE: "#menu_non_conforming_report_checkbox",
      viewNCE: "#menu_non_conforming_view_checkbox",
      correctiveAction: "#menu_non_conforming_corrective_actions_checkbox",
      workplan: "#menu_workplan_checkbox",
      pathology: "#menu_pathology_checkbox",
    };

    // Get the corresponding checkbox selector based on the passed menuItem
    const checkboxSelector = menuItems[menuItem];

    if (checkboxSelector) {
      // Perform the check action, forcing it to check even if covered
      cy.get(checkboxSelector).check({ force: true });
    } else {
      // If no valid menuItem is passed, log an error
      cy.log("Invalid menu item");
    }
  };
}

export default MenuConfigPage;
