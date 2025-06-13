import LoginPage from "../../pages/LoginPage";

let loginPage = null;
let homePage = null;
let adminPage = null;
let reflexTestsConfigPage = null;

const navigateToCalculatedValueTestsManagement = () => {
  loginPage = new LoginPage();
  loginPage.visit();
  homePage = loginPage.goToHomePage();
  adminPage = homePage.goToAdminPageProgram();
  reflexTestsConfigPage = adminPage.goToCalculatedValueTestsManagement();
};

before(() => {
  navigateToCalculatedValueTestsManagement();
});

describe("Calculated Value Tests Management", () => {
  it("Add Test Result", () => {
    cy.fixture("ReflexTestsConfig").then((test) => {
      reflexTestsConfigPage.verifyPageLoads(test.calcValue);
      reflexTestsConfigPage.validateToggleStatus(test.toggleOn);
      reflexTestsConfigPage.enterCalcName(test.ruleName);
      reflexTestsConfigPage.verifyRemoveOperationButton();
      reflexTestsConfigPage.selectFourthSample(test.sample);
      reflexTestsConfigPage.searchNumTest(test.searchTest);
    });
  });

  it("Add Mathematical Function", () => {
    cy.fixture("ReflexTestsConfig").then((test) => {
      reflexTestsConfigPage.clickMathFunctionButton();
      reflexTestsConfigPage.mathFunction(test.mtcFunction);
    });
  });

  it("Add Integer", () => {
    cy.fixture("ReflexTestsConfig").then((test) => {
      reflexTestsConfigPage.clickIntegerButton();
      reflexTestsConfigPage.enterInteger(test.numericValue);
    });
  });

  it("Add Patient Attribute", () => {
    cy.fixture("ReflexTestsConfig").then((test) => {
      reflexTestsConfigPage.clickPatientAttributeButton();
      reflexTestsConfigPage.selectPatientAttribute(test.patientAttribute);
    });
  });

  it("Enter Final Result and Submit", () => {
    cy.fixture("ReflexTestsConfig").then((test) => {
      reflexTestsConfigPage.selectThirdSample(test.sample);
      reflexTestsConfigPage.enterFinalResult(test.finalResult);
      reflexTestsConfigPage.addFinalExternatNote(test.externalNote);
      reflexTestsConfigPage.submitButton();
    });
  });

  it("Validate Added Rule", () => {
    reflexTestsConfigPage.reflexMgnt();
    reflexTestsConfigPage.calcPage();
    cy.fixture("ReflexTestsConfig").then((test) => {
      reflexTestsConfigPage.validateToggleStatus(test.toggleOff);
      reflexTestsConfigPage.validateCalcName(test.ruleName);
    });
  });
});
