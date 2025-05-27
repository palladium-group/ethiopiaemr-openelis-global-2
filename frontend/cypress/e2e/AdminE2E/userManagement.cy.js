import LoginPage from "../../pages/LoginPage";

let loginPage = null;
let homePage = null;
let adminPage = null;
let userMgmnt = null;

before(() => {
  // Initialize LoginPage object and navigate to Admin Page
  loginPage = new LoginPage();
  loginPage.visit();

  homePage = loginPage.goToHomePage();
  adminPage = homePage.goToAdminPage();
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
      cy.fixture("UserManagement").then((users) => {
        userMgmnt.typeLoginName(users.username);
        userMgmnt.passwordExpiryDate(users.passwordExpiryDate);
        userMgmnt.typeLoginPassword(users.password);
        userMgmnt.repeatPassword(users.password);
        userMgmnt.enterFirstName(users.fName);
        userMgmnt.enterLastName(users.lName);
        userMgmnt.enterUserTimeout(users.userTimeout);
      });
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

  describe("Add Users and Save", function () {
    it("Add First User", function () {
      userMgmnt.clickAddButton();
      userMgmnt.validatePageTitle();
    });

    it("Enter USer details", function () {
      cy.fixture("UserManagement").then((users) => {
        userMgmnt.typeLoginName(users.username);
        userMgmnt.passwordExpiryDate(users.passwordExpiryDate);
        userMgmnt.typeLoginPassword(users.password);
        userMgmnt.repeatPassword(users.password);
        userMgmnt.enterFirstName(users.fName);
        userMgmnt.enterLastName(users.lName);
        userMgmnt.enterUserTimeout(users.userTimeout);
      });
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

    it("Add Second User", function () {
      userMgmnt = adminPage.goToUserManagementPage();
      userMgmnt.verifyPageTitle();
      userMgmnt.clickAddButton();
      userMgmnt.validatePageTitle();
    });

    it("Enter USer details", function () {
      cy.fixture("UserManagement").then((users) => {
        userMgmnt.typeLoginName(users.usernameT);
        userMgmnt.passwordExpiryDate(users.passwordExpiryDateT);
        userMgmnt.typeLoginPassword(users.passwordT);
        userMgmnt.repeatPassword(users.passwordT);
        userMgmnt.enterFirstName(users.fNameT);
        userMgmnt.enterLastName(users.lNameT);
        userMgmnt.enterUserTimeout(users.userTimeoutT);
      });
      userMgmnt.checkAccountLocked();
      userMgmnt.checkAccountDisabled();
      userMgmnt.checkNotActive();
      userMgmnt.checkAccountNotLocked();
      userMgmnt.checkAccountEnabled();
      userMgmnt.checkActive();
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

  describe("Validate added Users", function () {
    it("Search by Username", function () {
      userMgmnt = adminPage.goToUserManagementPage();
      userMgmnt.verifyPageTitle();
      cy.fixture("UserManagement").then((users) => {
        userMgmnt.searchUser(users.username);
        userMgmnt.validateUser(users.username);
        cy.reload();
        userMgmnt.searchUser(users.usernameT);
        userMgmnt.validateUser(users.usernameT);
        cy.reload();
      });
    });

    it("Search by First Name", function () {
      cy.fixture("UserManagement").then((users) => {
        userMgmnt.searchUser(users.fName);
        userMgmnt.validateUser(users.fName);
        cy.reload();
        userMgmnt.searchUser(users.fNameT);
        userMgmnt.validateUser(users.fNameT);
        cy.reload();
      });
    });

    it("Search by Last Name", function () {
      cy.fixture("UserManagement").then((users) => {
        userMgmnt.searchUser(users.lName);
        userMgmnt.validateUser(users.lName);
        cy.reload();
        userMgmnt.searchUser(users.lNameT);
        userMgmnt.validateUser(users.lNameT);
        cy.reload();
      });
    });

    it("Validate active/inactive users", function () {
      userMgmnt.activeUser(); //checks active users
      cy.fixture("UserManagement").then((users) => {
        userMgmnt.inactiveUser(users.fName);
        userMgmnt.activeUser(users.fNameT);
        cy.reload();
      });
    });

    it("Search by Only Administrator", function () {
      userMgmnt.adminUser();
      cy.fixture("UserManagement").then((users) => {
        userMgmnt.validateUser(users.fName);
        userMgmnt.validateUser(users.fNameT);
        cy.reload();
      });
    });
  });

  describe("Modify First User", function () {
    it("Check user to modify", function () {
      cy.fixture("UserManagement").then((users) => {
        userMgmnt.checkUser(users.fName);
      });
    });

    it("Modify User and Save", function () {
      userMgmnt.modifyUser();
      cy.fixture("UserManagement").then((users) => {
        userMgmnt.typeLoginPassword(users.password);
        userMgmnt.repeatPassword(users.password);
      });
      userMgmnt.checkAccountNotLocked();
      userMgmnt.checkAccountEnabled();
      userMgmnt.checkActive();
      userMgmnt.saveChanges();
    });

    it("Validate user is activated", function () {
      userMgmnt = adminPage.goToUserManagementPage();
      userMgmnt.verifyPageTitle();
      userMgmnt.activeUser();
      cy.fixture("UserManagement").then((users) => {
        userMgmnt.activeUser(users.fName);
      });
    });
  });

  //describe("Deactivate User", function () {
  //it("Check User and deactivate", function () {
  //userMgmnt.checkUser(usersData[1].fName);
  //userMgmnt.deactivateUser();
  //});
  //it("Validate deactivated user", ()=>{});
  //});

  describe("Signout, use active/deactivated user to login", () => {
    // it("Login with Deactivated user", () => {
    // userMgmnt = loginPage.signOut();
    // loginPage.enterUsername(usersData[1].username);
    // loginPage.enterPassword(usersData[1].password);
    // loginPage.signIn();
    // cy.contains("Username or Password are incorrect").should("be.visible");
    //userMgmnt.incorrectCredentials();
    // });

    it("Login with Active user", () => {
      userMgmnt = loginPage.signOut();
      cy.fixture("UserManagement").then((users) => {
        loginPage.enterUsername(users.username);
        loginPage.enterPassword(users.password); //BUG:there is an auto password change(@@@@@@)
      });
      loginPage.signIn();
    });

    it("Navigate back to User Management", () => {
      cy.fixture("UserManagement").then((users) => {
        userMgmnt.enterLoginName(users.username);
        userMgmnt.enterPassword(users.password);
      });
      adminPage = homePage.goToAdminPage();
      userMgmnt = adminPage.goToUserManagementPage();
    });
  });
});
