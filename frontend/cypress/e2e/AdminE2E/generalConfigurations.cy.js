import LoginPage from "../../pages/LoginPage";

describe("General Configurations", function () {
  let homePage, loginPage, adminPage, generalConfigurationsPage;

  before(() => {
    loginPage = new LoginPage();
    loginPage.visit();
    homePage = loginPage.goToHomePage();
    adminPage = homePage.goToAdminPage();
  });

  // Shared test actions for toggle scenarios
  const testToggleConfiguration = (configName, title) => {
    describe(`${configName} Configuration`, () => {
      before(() => {
        generalConfigurationsPage = adminPage[`goTo${configName}Config`]();
      });

      it(`should toggle ${configName} configuration between True and False`, () => {
        // Test False
        generalConfigurationsPage.validatePageTitle(`${title}`);
        generalConfigurationsPage.selectItem();
        generalConfigurationsPage.clickModifyButton();
        generalConfigurationsPage.validatePageTitle("Edit Record");
        generalConfigurationsPage.checkValue("False");
        generalConfigurationsPage.saveChanges();
        generalConfigurationsPage.validateStatus("False");

        // Test True
        generalConfigurationsPage.validatePageTitle(`${title}`);
        generalConfigurationsPage.selectItem();
        generalConfigurationsPage.clickModifyButton();
        generalConfigurationsPage.validatePageTitle("Edit Record");
        generalConfigurationsPage.checkValue("True");
        generalConfigurationsPage.saveChanges();
        generalConfigurationsPage.validateStatus("True");
      });
    });
  };

  // Configuration tests
  testToggleConfiguration("NonConformity", "NonConformity Configuration");
  testToggleConfiguration("WorkPlan", "WorkPlan Configuration");
  testToggleConfiguration("SiteInformation", "Site Information");
  testToggleConfiguration("ResultEntity", "Result Entry Configuration");
  testToggleConfiguration("PatientEntity", "Patient Entry Configuration");
  testToggleConfiguration("PrintedReport", "Printed Report Configuration");
  testToggleConfiguration("OrderEntity", "Order Entry Configuration");
  testToggleConfiguration("Validation", "Validation Configuration");

  // Special case since there are no options yet
  describe("Menu Statement Configuration", () => {
    it("should navigate to Menu Statement Configuration", () => {
      generalConfigurationsPage = adminPage.goToMenuStatementConfig();
      generalConfigurationsPage.validatePageTitle(
        "Menu Statement Configuration",
      );
    });
  });
});
