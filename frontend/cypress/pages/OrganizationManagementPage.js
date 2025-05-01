class OrganizationManagementPage {
  constructor() {
    this.selectors = {
      // Buttons
      addButton: "[data-cy='add-button']",
      saveButton: "#saveButton",

      // Input fields
      orgName: "#org-name",
      orgPrefix: "#org-prefix",
      isActive: "#is-active",
      parentOrgName: "#parentOrgName",
      orgSearchBar: "#org-name-search-bar",

      // Checkboxes
      referringClinic: '[id="5:select"]',
      referralLab: '[id="6:select"]',

      // Table selectors
      orgTableRow: ".cds--data-table > tbody:nth-child(2)",
    };
  }

  clickAddOrganization() {
    cy.get(this.selectors.addButton).should("be.visible").click();
  }

  addOrgName() {
    cy.get(this.selectors.orgName).should("be.visible").type("CAMES MAN");
  }

  addInstituteName() {
    cy.get(this.selectors.orgName).should("be.visible").type("CEDRES");
  }

  activateOrganization() {
    cy.get(this.selectors.isActive).clear().type("Y");
  }

  addPrefix() {
    cy.get(this.selectors.orgPrefix).should("be.visible").type("279");
  }

  addInstitutePrefix() {
    cy.get(this.selectors.orgPrefix).should("be.visible").clear().type("");
  }

  checkReferringClinic() {
    cy.get(this.selectors.referringClinic).check({ force: true });
  }

  checkReferalLab() {
    cy.get(this.selectors.referralLab).check({ force: true });
  }

  addParentOrg() {
    cy.get(this.selectors.parentOrgName).should("be.visible").type("CAMESM AN");
  }

  saveOrganization() {
    cy.get(this.selectors.saveButton).should("be.visible").click();
  }

  searchOrganzation() {
    cy.get(this.selectors.orgSearchBar).should("be.visible").type("CAMES MAN");
  }

  searchOInstitute() {
    cy.get(this.selectors.orgSearchBar).should("be.visible").type("CEDRES");
  }

  confirmOrganization() {
    cy.get(this.selectors.orgTableRow)
      .contains("CAMES MAN")
      .should("be.visible");
  }

  confirmInstitute() {
    cy.get(this.selectors.orgTableRow).contains("CEDRES").should("be.visible");
  }
}

export default OrganizationManagementPage;
