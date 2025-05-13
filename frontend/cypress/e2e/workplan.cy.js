import LoginPage from "../pages/LoginPage";
import ProviderManagementPage from "../pages/ProviderManagementPage";
import AdminPage from "../pages/AdminPage";
import Result from "../pages/ResultsPage";

let homePage = null;
let loginPage = null;
let workplan = null;
let providerManagementPage = new ProviderManagementPage();
let adminPage = new AdminPage();
let result = new Result();

before("login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
});

describe("Add requester details first", function () {
  it("Navidates to admin", function () {
    homePage = loginPage.goToHomePage();
    workplan = homePage.goToAdminPageProgram();
    workplan = adminPage.goToProviderManagementPage();
  });

  it("Adds and saves requester", function () {
    providerManagementPage.clickAddProviderButton();
    providerManagementPage.enterProviderLastName();
    providerManagementPage.enterProviderFirstName();
    providerManagementPage.clickActiveDropdown();
    providerManagementPage.addProvider();
  });
});

describe("Workplan by Test Type", function () {
  it("User selects workplan by test type from main menu drop-down list.", function () {
    homePage = loginPage.goToHomePage();
    workplan = homePage.goToWorkPlanPlanByTest();
    cy.fixture("workplan").then((options) => {
      workplan.getWorkPlanFilterTitle(options.testTile);
    });
  });

  it("User selects test from drop-down options", () => {
    cy.fixture("workplan").then((options) => {
      workplan.selectDropdownOption(options.testName);
      workplan.getPrintWorkPlanButton();
    });
  });

  it("Validate and select Accession Number", () => {
    cy.fixture("Patient").then((options) => {
      workplan
        .getWorkPlanResultsTable()
        .find("tr")
        .then((row) => {
          expect(row.text()).contains(options.labNo);
        });
      workplan.clickAccessionNumber(options.labNo);
    });
  });

  it("Expand sample details and save the result", function () {
    result.expandSampleDetails();
    result.submitResults();
  });
});

describe("Workplan by Panel", function () {
  it("User can select work plan by test from main menu drop-down. Workplan by panel page appears.", function () {
    homePage = loginPage.goToHomePage();
    workplan = homePage.goToWorkPlanPlanByPanel();
    cy.fixture("workplan").then((options) => {
      workplan.getWorkPlanFilterTitle(options.panelTile);
    });
  });

  it("User should select panel from drop-down selector option", () => {
    cy.fixture("workplan").then((options) => {
      workplan.selectDropdownOption(options.bilanPanelType);
      workplan.getPrintWorkPlanButton();
    });
  });

  it("Validate and select Accession Number", () => {
    cy.fixture("Patient").then((options) => {
      workplan
        .getWorkPlanResultsTable()
        .find("tr")
        .then((row) => {
          expect(row.text()).contains(options.labNo);
        });
      workplan.clickAccessionNumber(options.labNo);
    });
  });

  it("Expand sample details and save the result", function () {
    result.expandSampleDetails();
    result.submitResults();
  });
});

describe("Workplan by Unit", function () {
  it("Navigates to Workplan By Unit.", function () {
    homePage = loginPage.goToHomePage();
    workplan = homePage.goToWorkPlanPlanByUnit();
    cy.fixture("workplan").then((options) => {
      workplan.getWorkPlanFilterTitle(options.unitTile);
    });
  });

  it("Search by Unit and click accession Number", function () {
    cy.fixture("workplan").then((order) => {
      workplan.selectDropdownOption(order.unitType);
    });
  });

  it("Validate and select Accession Number", () => {
    cy.fixture("Patient").then((options) => {
      workplan
        .getWorkPlanResultsTable()
        .find("tr")
        .then((row) => {
          expect(row.text()).contains(options.labNo);
        });
      workplan.clickAccessionNumber(options.labNo);
    });
  });

  it("Expand sample details and save the result", function () {
    result.expandSampleDetails();
    result.submitResults();
  });
});

describe("Workplan by Priority", function () {
  it("User selects workplan By Priority from main menu drop-down list.", function () {
    homePage = loginPage.goToHomePage();
    workplan = homePage.goToWorkPlanPlanByPriority();
    cy.fixture("workplan").then((options) => {
      workplan.getWorkPlanFilterTitle(options.priorityTile);
    });
  });

  it("User selects Priority from drop-down list", () => {
    cy.fixture("workplan").then((options) => {
      workplan.selectDropdownOption(options.priority);
      workplan.getPrintWorkPlanButton();
    });
  });

  it("Validate and select Accession Number", () => {
    cy.fixture("Patient").then((options) => {
      workplan
        .getWorkPlanResultsTable()
        .find("tr")
        .then((row) => {
          expect(row.text()).contains(options.labNo);
        });
      workplan.clickAccessionNumber(options.labNo);
    });
  });

  it("Expand sample details and save the result", function () {
    result.expandSampleDetails();
    result.submitResults();
  });
});
