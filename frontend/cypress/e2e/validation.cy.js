import LoginPage from "../pages/LoginPage";

let homePage = null;
let loginPage = null;
let validation = null;

before("login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
});

const navigateToValidationPage = (validationType) => {
  homePage = loginPage.goToHomePage();
  validation = homePage[`goToValidationBy${validationType}`]();
};

describe("Validation By Routine", function () {
  before("Navigate to Validation Page", function () {
    navigateToValidationPage("Routine");
  });

  it("Navigate to Validation Page", function () {
    validation.checkForHeading();
  });

  it("Select Test Unit From Drop-Down", function () {
    cy.fixture("workplan").then((order) => {
      validation.selectTestUnit(order.unitType);
    });
  });

  it("Enter data and Save the results", function () {
    validation.saveAllResults();
    validation.typeNotes();
    validation.saveResults();
  });
});

describe("Validation By Order", function () {
  before("Navigate to Validation Page", function () {
    navigateToValidationPage("Order");
  });

  it("Navigate to Validation Page", function () {
    validation.checkForHeading();
  });

  it("Enter Lab Number and search", function () {
    cy.fixture("Patient").then((order) => {
      validation.enterLabNumberAndSearch(order.labNo);
    });
  });

  it("Enter data and Save the results", function () {
    validation.saveAllResults();
    validation.typeNotes();
    validation.saveResults();
  });
});

describe("Validation By Range Of Order", function () {
  before("Navigate to Validation Page", function () {
    navigateToValidationPage("RangeOrder");
  });

  it("Navigate to Validation Page", function () {
    validation.checkForHeading();
  });

  it("Enter Lab Number and search", function () {
    cy.fixture("Patient").then((order) => {
      validation.enterLabNumberAndSearch(order.labNo);
    });
  });

  it("Enter data and Save the results", function () {
    validation.saveAllResults();
    validation.typeNotes();
    validation.saveResults();
  });
});

describe("Validation By Date", function () {
  before("Navigate to Validation Page", function () {
    navigateToValidationPage("Date");
  });

  it("Navigate to Validation Page", function () {
    validation.checkForHeading();
  });

  it("Enter Date and search", function () {
    validation.enterDateAndSearch("01/05/2025");
  });

  it("Save the results", function () {
    validation.saveResults();
  });
});
