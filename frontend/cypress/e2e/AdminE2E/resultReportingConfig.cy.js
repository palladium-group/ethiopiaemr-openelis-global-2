import LoginPage from "../../pages/LoginPage";

describe("Result Reporting Configuration", function () {
  let homePage, loginPage, adminPage, resultReportConfigPage;

  before(() => {
    loginPage = new LoginPage();
    loginPage.visit();
    homePage = loginPage.goToHomePage();
    adminPage = homePage.goToAdminPage();
  });

  it("Navigate to Result Reporting Configuration Page", () => {
    resultReportConfigPage = adminPage.goToResultReportingConfigurationPage();
    resultReportConfigPage.validatePageTitle();
  });

  it("Enable and Enter URL", () => {
    resultReportConfigPage.clickEnable();
    resultReportConfigPage.typeURL("0");
    resultReportConfigPage.clickEnable();
    resultReportConfigPage.typeURL("1");
    resultReportConfigPage.clickEnable();
    resultReportConfigPage.typeURL("2");
  });

  it("Save Entry", () => {
    resultReportConfigPage.saveEntry();
  });
});
