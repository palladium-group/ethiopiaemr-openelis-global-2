import LoginPage from "../pages/LoginPage";

let homePage = null;
let loginPage = null;
let adminPage = null;
let providerManagementPage = null;

const loginAndNavigateToHome = () => {
  loginPage = new LoginPage();
  loginPage.visit();
  homePage = loginPage.goToHomePage();
};

describe("Add requester first", function () {
  before("Navigate to homepage", () => {
    loginAndNavigateToHome();
  });
  it("Navidates to Admin", function () {
    providerManagementPage = homePage.goToAdminPageProgram();
    providerManagementPage = adminPage.goToProviderManagementPage();
  });

  it("Adds and saves requester", function () {
    providerManagementPage.clickAddProviderButton();
    providerManagementPage.enterProviderLastName();
    providerManagementPage.enterProviderFirstName();
    providerManagementPage.clickActiveDropdown();
    providerManagementPage.addProvider();
    cy.reload();
  });

  it("Validate added requester", function () {
    providerManagementPage.searchRequester();
    providerManagementPage.confirmRequester();
  });
});
