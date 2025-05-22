import { use } from "react";
import LoginPage from "../../pages/LoginPage";

let loginPage = null;
let homePage = null;
let adminPage = null;
let userMgmnt = null;
let usersData;

before(() => {
  // Initialize LoginPage object and navigate to Admin Page
  loginPage = new LoginPage();
  loginPage.visit();

  homePage = loginPage.goToHomePage();
  adminPage = homePage.goToAdminPage();

  cy.fixture("Users").then((users) => {
    usersData = users;
  });
});

describe("User Management", function () {
  it("Navigate to User Management Page", function () {
    userMgmnt = adminPage.goToUserManagementPage();
    userMgmnt.verifyPageTitle();
  });

  describe("Add User and Exit", function () {
    it("Add User", function () {
      userMgmnt.clickAddButton();
      userMgmnt.validatePageTitle();
    });

    it("Enter USer details", function () {
      userMgmnt.typeLoginName(usersData[4].username);
      userMgmnt.passwordExpiryDate("01/01/2035");
      userMgmnt.typeLoginPassword(usersData[4].password);
      userMgmnt.repeatPassword(usersData[4].password);
      userMgmnt.enterFirstName("Warren");
      userMgmnt.enterLastName("Buffet");
      userMgmnt.enterUserTimeout("480");
    });

    it("Add and Remove Lab Unit Roles", function () {
      userMgmnt.addNewPermission();
      userMgmnt.allPermissions();
      userMgmnt.removePermission();
    });

    it("Apply Roles and Permissions", function () {
      //userMgmnt.copyPermisionsFromUser();
      //userMgmnt.applyChanges();
      userMgmnt.analyzerImport();
      userMgmnt.auditTrail();
      userMgmnt.cytopathologist();
      userMgmnt.globalAdministrator();
      userMgmnt.addNewPermission();
      userMgmnt.allPermissions();
    });

    it("Exit", function () {
      userMgmnt.exitChanges();
    });
  });

  describe("Add User and Save", function () {
    it("Add User", function () {
      userMgmnt.clickAddButton();
      userMgmnt.validatePageTitle();
    });

    it("Enter USer details", function () {
      userMgmnt.typeLoginName(usersData[4].username);
      userMgmnt.passwordExpiryDate("01/01/2035");
      userMgmnt.typeLoginPassword(usersData[4].password);
      userMgmnt.repeatPassword(usersData[4].password);
      userMgmnt.enterFirstName("Warren");
      userMgmnt.enterLastName("Buffet");
      userMgmnt.enterUserTimeout("480");
    });

    it("Add and Remove Lab Unit Roles", function () {
      userMgmnt.addNewPermission();
      userMgmnt.allPermissions();
      userMgmnt.removePermission();
    });

    it("Apply Roles and Permissions", function () {
      //userMgmnt.copyPermisionsFromUser();
      //userMgmnt.applyChanges();
      userMgmnt.analyzerImport();
      userMgmnt.auditTrail();
      userMgmnt.cytopathologist();
      userMgmnt.globalAdministrator();
      userMgmnt.addNewPermission();
      userMgmnt.allPermissions();
    });

    it("Save Changes", function () {
      userMgmnt.saveChanges();
    });
  });

  describe("Validate added User", function () {
    it("Search by Username", function () {
      userMgmnt = adminPage.goToUserManagementPage();
      userMgmnt.validatePageTitle();
      userMgmnt.searchUser(usersData[4].username);
      //add validations
      cy.reload();
    });

    it("Search by First Name", function () {
      userMgmnt.searchUser("Warren");
      //add validations
      cy.reload();
    });

    it("Search by Last Name", function () {
      userMgmnt.searchUser("Buffet");
      //add validations
      cy.reload();
    });

    it("Search by Only Active", function () {
      userMgmnt.activeUser();
      //add validations
      cy.reload();
    });

    it("Search by Only Administrator", function () {
      userMgmnt.adminUser();
      //add validations
      cy.reload();
    });
  });
});
