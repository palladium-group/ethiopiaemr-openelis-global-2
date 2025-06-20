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

  it("Navidate to organisation Management", function () {
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

  it("Validates the added site/organization", function () {
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

  it("Validates the added institute", function () {
    orgMgmnt = adminPage.goToOrganizationManagement();
    orgMgmnt.searchInstitute();
    orgMgmnt.confirmInstitute();
  });
});
