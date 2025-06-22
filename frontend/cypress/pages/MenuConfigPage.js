class MenuConfigPage {
  constructor() {
    this.selectors = {
      menuButton: "[data-cy='menuButton']",
      nonConformMenu: "#menu_nonconformity",
      nonConformCheck: "#menu_nonconformity_checkbox",
      nonConformReport: "#menu_non_conforming_report",
      nonConformView: "#menu_non_conforming_view",
      correctiveAction: "#menu_non_conforming_corrective_actions",
      patientMenu: "#menu_patient",
      addEditPatient: "#menu_patient_add_or_edit",
      patientHistory: "#menu_patienthistory",
      studyPatient: "#menu_patient_create",
      enterBillingAddress: "#billing_address",
      billingMenu: "#menu_billing",
      patientCheck: "#menu_patient_checkbox",
      billingMenuCheck: "#billing_active",
      toggleText: ".cds--toggle__text",
      toggleOn: "div.cds--toggle__switch",
      toggleOff: "div.cds--toggle label div > div",
    };
  }

  // This method is used to visit the page
  visit() {
    cy.visit("/administration#globalMenuManagement");
  }

  navigateToMainMenu() {
    cy.get(this.selectors.menuButton).click();
    cy.wait(2000);
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

  uncheckNonConform() {
    cy.get(this.selectors.nonConformCheck).uncheck({ force: true });
  }

  validateNonConformOff() {
    cy.get(this.selectors.nonConformMenu).should("not.exist");
  }

  validateNonConformOn() {
    cy.get(this.selectors.nonConformMenu).should("be.visible").click();
    cy.get(this.selectors.nonConformReport).should("be.visible");
    cy.get(this.selectors.nonConformView).should("be.visible");
    cy.get(this.selectors.correctiveAction).should("be.visible");
  }

  uncheckPatientMenu() {
    cy.get(this.selectors.patientCheck).uncheck({ force: true });
  }

  validatePatientMenuOff() {
    cy.get(this.selectors.patientMenu).should("not.exist");
  }

  validatePatientMenuOn() {
    cy.get(this.selectors.patientMenu).should("be.visible").click();
    cy.get(this.selectors.addEditPatient).should("be.visible");
    cy.get(this.selectors.patientHistory).should("be.visible");
    cy.get(this.selectors.studyPatient).should("be.visible");
  }

  validateBillingMenuOn() {
    cy.get(this.selectors.billingMenu).should("be.visible");
  }

  validateBillingMenuOff() {
    cy.get(this.selectors.billingMenu).should("not.exist");
  }

  billingAddress(value) {
    cy.get(this.selectors.enterBillingAddress).type(value);
  }

  uncheckBillingMenu() {
    cy.get(this.selectors.billingMenuCheck).uncheck({ force: true });
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
      studyConfig: "#menu_study_checkbox",
      studySample: "#menu_sample_create_checkbox",
      studyReports: "#menu_reports_study_checkbox",
      billing: "#menu_billing_checkbox",
      billingMenu: "#billing_active",
      admin: "#menu_administration_checkbox",
      help: "#menu_help_checkbox",
      patient: "#menu_patient_checkbox",
      addEditPatient: "#menu_patient_add_or_edit_checkbox",
      patientHistory: "#menu_patienthistory_checkbox",
      studyPatient: "#menu_patient_create_checkbox",
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
