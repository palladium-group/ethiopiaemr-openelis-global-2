import { Search } from "@carbon/react";

class UserManagementPage {
  constructor() {
    this.selectors = {
      pageTitle: "h2",
      userPageTitle: "h3",
      span: "span",
      addButton: "[data-cy='add-button']",
      loginName: "#login-name",
      loginPassword: "#login-password",
      repeatPassword: "#login-repeat-password",
      firstName: "#first-name",
      lastName: "#last-name",
      passwordExpirationDate: "#password-expire-date",
      userTimeOut: "#login-timeout",
      accountLocked: "",
      accountDisabled: "",
      isActive: "",
      copyPermisionsFromUser: "#copy-permissions",
      applyButton: "[data-cy='apply-button']",
      addNewPermission: "[data-cy='addNewPermission']",
      removePermission: "[data-cy='removePermission']",
      saveButton: "[data-cy='saveButton']",
      exitButton: "[data-cy='exitButton']",
      searchBar: "#user-name-search-bar",
    };
  }
  verifyPageTitle() {
    cy.contains(this.selectors.pageTitle, "User Management").should(
      "be.visible",
    );
  }

  validatePageTitle() {
    cy.contains(this.selectors.userPageTitle, "Add User").should("be.visible");
  }
  clickAddButton() {
    cy.get(this.selectors.addButton).click();
  }

  typeLoginName(value) {
    cy.get(this.selectors.loginName).type(value);
  }

  typeLoginPassword(value) {
    cy.get(this.selectors.loginPassword).type(value);
  }

  repeatPassword(value) {
    cy.get(this.selectors.repeatPassword).type(value);
  }

  enterFirstName(value) {
    cy.get(this.selectors.firstName).type(value);
  }

  enterLastName(value) {
    cy.get(this.selectors.lastName).type(value);
  }

  passwordExpiryDate(value) {
    cy.get(this.selectors.passwordExpirationDate).type(value);
  }

  enterUserTimeout(value) {
    cy.get(this.selectors.userTimeOut).type(value);
  }

  copyPermisionsFromUser(value) {
    cy.get(this.selectors.copyPermisionsFromUser).type(value);
  }

  applyChanges() {
    cy.get(this.selectors.applyButton).click();
  }

  removePermission() {
    cy.get(this.selectors.removePermission).click();
  }
  //All Lab Units
  addNewPermission() {
    cy.get(this.selectors.addNewPermission).click();
  }

  allPermissions() {
    cy.contains(this.selectors.span, "All Permissions").click();
  }

  reception() {
    cy.contains(this.selectors.span, "Reception").click();
  }

  reports() {
    cy.contains(this.selectors.span, "Reports").click();
  }

  results() {
    cy.contains(this.selectors.span, "Results").click();
  }

  saveChanges() {
    cy.get(this.selectors.saveButton).click();
  }

  exitChanges() {
    cy.get(this.selectors.exitButton).click();
  }

  //Global Roles
  analyzerImport() {
    cy.contains(this.selectors.span, "Analyser Import").click();
  }

  auditTrail() {
    cy.contains(this.selectors.span, "Audit Trail").click();
  }

  cytopathologist() {
    cy.contains(this.selectors.span, "Cytopathologist").click();
  }

  globalAdministrator() {
    cy.contains(this.selectors.span, "Global Administrator").click();
  }

  pathologist() {
    cy.contains(this.selectors.span, "Pathologist").click();
  }

  userAccountAdmin() {
    cy.contains(this.selectors.span, "User Account Administrator").click();
  }

  searchUser(value) {
    cy.get(this.selectors.searchBar).type(value);
  }

  activeUser() {
    cy.contains(this.selectors.span, "Only Active").click();
  }

  adminUser() {
    cy.contains(this.selectors.span, "Only Administrator").click();
  }
}

export default UserManagementPage;
