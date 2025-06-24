class ProviderManagementPage {
  constructor() {
    this.selectors = {
      addButton: "[data-cy='add-Button']",
      lastNameInput: "#lastName",
      firstNameInput: "#firstName",
      activeDropdown: "#isActive",
      provderSearchBar: "#provider-search-bar",
      modalAddButton: "div.cds--modal button:contains('Add')",
    };
  }

  clickAddProviderButton() {
    cy.get(this.selectors.addButton).should("be.visible").click();
    cy.wait(200);
  }

  enterProviderLastName() {
    cy.get(this.selectors.lastNameInput).type("Prime");
  }

  enterProviderFirstName() {
    cy.get(this.selectors.firstNameInput).type("Optimus");
  }

  clickActiveDropdown() {
    cy.get(this.selectors.activeDropdown).contains("Yes").click();
  }

  addProvider() {
    cy.get(this.selectors.modalAddButton).click();
    cy.wait(200);
  }

  searchRequester(value) {
    cy.get(this.selectors.provderSearchBar).clear().type(value);
  }

  confirmRequester(value) {
    cy.contains("td", value).should("exist");
  }
}

export default ProviderManagementPage;
