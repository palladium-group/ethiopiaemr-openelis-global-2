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
      userMgmnt.enterUserTimeout("500");
      userMgmnt.checkAccountLocked();
      userMgmnt.checkAccountDisabled();
      userMgmnt.checkNotActive();
    });

    it("Apply Roles and Permissions", function () {
      //userMgmnt.copyPermisionsFromUser();
      //userMgmnt.applyChanges();
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
      userMgmnt.verifyPageTitle();
      userMgmnt.searchUser(usersData[4].username);
      userMgmnt.validateUser(usersData[4].username);
      cy.reload();
    });

    it("Search by First Name", function () {
      userMgmnt.searchUser("Warren");
      userMgmnt.validateUser("Warren");
      cy.reload();
    });

    it("Search by Last Name", function () {
      userMgmnt.searchUser("Buffet");
      userMgmnt.validateUser("Buffet");
      cy.reload();
    });

    it("User should not be active", function () {
      userMgmnt.activeUser(); //checks active users
      userMgmnt.inactiveUser("Warren");
      cy.reload();
    });

    it("Search by Only Administrator", function () {
      userMgmnt.adminUser();
      userMgmnt.validateUser("Warren");
    });
  });

  describe("Modify added User", function () {
    it("Search and check user", function () {
      userMgmnt.searchUser("Warren");
      userMgmnt.validateUser("Warren");
      userMgmnt.checkUser("Warren");
    });

    it("Modify User and Save", function () {
      userMgmnt.modifyUser();
      userMgmnt.checkNotAccountLocked();
      userMgmnt.checkAccountEnabled();
      userMgmnt.checkActive();
      userMgmnt.saveChanges();
    });

    it("Validate user is activated", function () {
      userMgmnt.activeUser();
      userMgmnt.activeUser("Warren");
    });
  });
});
