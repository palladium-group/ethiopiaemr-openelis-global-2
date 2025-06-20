import LoginPage from "../../pages/LoginPage";

let homePage = null;
let loginPage = null;
let adminPage = null;
let orgMgmnt = null;

before("login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
});

describe("Add Organization and Institute", function () {
  it("Navigate to Admin Page", function () {
    homePage = loginPage.goToHomePage();
    adminPage = homePage.goToAdminPageProgram();
  });

  it("Navigate to organisation Management", function () {
    orgMgmnt = adminPage.goToOrganizationManagement();
  });

  it("Add organisation/site details", function () {
    orgMgmnt.clickAddOrganization();
    orgMgmnt.addOrgName();
    orgMgmnt.activateOrganization();
    orgMgmnt.addPrefix();
    orgMgmnt.addParentOrg();
    orgMgmnt.checkReferringClinic();
    orgMgmnt.saveOrganization();
  });

  it("Validate added site/organization", function () {
    orgMgmnt = adminPage.goToOrganizationManagement();
    orgMgmnt.searchOrganzation();
    orgMgmnt.confirmOrganization();
  });

  it("Add institute details", function () {
    orgMgmnt.clickAddOrganization();
    orgMgmnt.addInstituteName();
    orgMgmnt.activateOrganization();
    //orgMgmnt.addInstitutePrefix();
    orgMgmnt.addParentOrg();
    orgMgmnt.checkReferalLab();
    orgMgmnt.saveOrganization();
  });

  it("Validate added institute", function () {
    orgMgmnt = adminPage.goToOrganizationManagement();
    orgMgmnt.searchInstitute();
    orgMgmnt.confirmInstitute();
  });
});
