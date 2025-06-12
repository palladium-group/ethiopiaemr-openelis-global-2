import LoginPage from "../../pages/LoginPage";

let loginPage = null;
let homePage = null;
let adminPage = null;
let reflexTestsConfigPage = null;

const navigateToReflexTestsManagement = () => {
  loginPage = new LoginPage();
  loginPage.visit();
  homePage = loginPage.goToHomePage();
  adminPage = homePage.goToAdminPageProgram();
  reflexTestsConfigPage = adminPage.goToReflexTestsManagement();
  reflexTestsConfigPage.verifyPageLoads();
};

before(() => {
  navigateToReflexTestsManagement();
});
