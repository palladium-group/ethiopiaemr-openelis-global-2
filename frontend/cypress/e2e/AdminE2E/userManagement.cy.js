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
});

describe("User Management", function () {
  beforeEach(() => {
    cy.fixture("UserManagement").then((users) => {
      usersData = users;
    });
  });

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
      userMgmnt.typeLoginName(usersData[0].username);
      userMgmnt.passwordExpiryDate(usersData[0].passwordExpiryDate);
      userMgmnt.typeLoginPassword(usersData[0].password);
      userMgmnt.repeatPassword(usersData[0].password);
      userMgmnt.enterFirstName(usersData[0].fName);
      userMgmnt.enterLastName(usersData[0].lName);
      userMgmnt.enterUserTimeout(usersData[0].userTimeout);
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
      userMgmnt.typeLoginName(usersData[0].username);
      userMgmnt.passwordExpiryDate(usersData[0].passwordExpiryDate);
      userMgmnt.typeLoginPassword(usersData[0].password);
      userMgmnt.repeatPassword(usersData[0].password);
      userMgmnt.enterFirstName(usersData[0].fName);
      userMgmnt.enterLastName(usersData[0].lName);
      userMgmnt.enterUserTimeout(usersData[0].userTimeout);
      userMgmnt.checkAccountLocked();
      userMgmnt.checkAccountDisabled();
      userMgmnt.checkNotActive();
    });

    it("Apply Roles and Permissions", function () {
      userMgmnt.globalAdministrator();
      userMgmnt.addNewPermission();
      userMgmnt.allPermissions();
    });

    it("Save User", function () {
      userMgmnt.saveChanges();
    });

    it("Add Second User", function () {
      userMgmnt = adminPage.goToUserManagementPage();
      userMgmnt.verifyPageTitle();
      userMgmnt.clickAddButton();
      userMgmnt.validatePageTitle();
      userMgmnt.typeLoginName(usersData[1].username);
      userMgmnt.passwordExpiryDate(usersData[1].passwordExpiryDate);
      userMgmnt.typeLoginPassword(usersData[1].password);
      userMgmnt.repeatPassword(usersData[1].password);
      userMgmnt.enterFirstName(usersData[1].fName);
      userMgmnt.enterLastName(usersData[1].lName);
      userMgmnt.enterUserTimeout(usersData[1].userTimeout);
      userMgmnt.checkAccountLocked();
      userMgmnt.checkAccountDisabled();
      userMgmnt.checkNotActive();
      userMgmnt.checkAccountNotLocked();
      userMgmnt.checkAccountEnabled();
      userMgmnt.checkActive();
    });

    it("Apply Roles and Permissions", function () {
      //userMgmnt.copyPermisionsFromUser(usersData[0].fName);
      //userMgmnt.applyChanges();
      userMgmnt.globalAdministrator();
      userMgmnt.addNewPermission();
      userMgmnt.allPermissions();
      userMgmnt.addNewPermission();
      userMgmnt.allBioPermissions();
      userMgmnt.addNewPermission();
      userMgmnt.allHemaPermissions();
      userMgmnt.addNewPermission();
      userMgmnt.allSeroPermissions();
      userMgmnt.addNewPermission();
      userMgmnt.allImmunoPermissions();
      userMgmnt.addNewPermission();
      userMgmnt.allMolecularPermissions();
      userMgmnt.addNewPermission();
      userMgmnt.allCytoPermissions();
      userMgmnt.addNewPermission();
      userMgmnt.allSerologyPermissions();
      userMgmnt.addNewPermission();
      userMgmnt.allViroPermissions();
      userMgmnt.addNewPermission();
      userMgmnt.allPathoPermissions();
      userMgmnt.addNewPermission();
      userMgmnt.allImmunoHistoPermissions();
    });

    it("Save User", function () {
      userMgmnt.saveChanges();
    });
  });

  describe("Validate added Users", function () {
    it("Search by Username", function () {
      userMgmnt = adminPage.goToUserManagementPage();
      userMgmnt.verifyPageTitle();
      cy.wait(500);
      userMgmnt.searchUser(usersData[0].username);
      userMgmnt.validateUser(usersData[0].username);
      userMgmnt.searchUser(usersData[1].username);
      userMgmnt.validateUser(usersData[1].username);
    });

    it("Search by First Name", function () {
      userMgmnt.searchUser(usersData[0].fName);
      userMgmnt.validateUser(usersData[0].fName);
      userMgmnt.searchUser(usersData[1].fName);
      userMgmnt.validateUser(usersData[1].fName);
    });

    it("Search by Last Name", function () {
      userMgmnt.searchUser(usersData[0].lName);
      userMgmnt.validateUser(usersData[0].lName);
      userMgmnt.searchUser(usersData[1].lName);
      userMgmnt.validateUser(usersData[1].lName);
      userMgmnt.clearSearchBar();
    });

    it("Search by Lab Unit Roles", function () {
      cy.reload();
      userMgmnt.searchByFilters(usersData[1].bioChem);
      userMgmnt.validateUser(usersData[1].fName);
      userMgmnt.searchByFilters(usersData[1].hematology);
      userMgmnt.validateUser(usersData[1].fName);
      userMgmnt.searchByFilters(usersData[1].seroImmuno);
      userMgmnt.validateUser(usersData[1].fName);
      userMgmnt.searchByFilters(usersData[1].immunology);
      userMgmnt.validateUser(usersData[1].fName);
      userMgmnt.searchByFilters(usersData[1].molecularBio);
      userMgmnt.validateUser(usersData[1].fName);
      userMgmnt.searchByFilters(usersData[1].cyto);
      userMgmnt.validateUser(usersData[1].fName);
      userMgmnt.searchByFilters(usersData[1].viro);
      userMgmnt.validateUser(usersData[1].fName);
      userMgmnt.searchByFilters(usersData[1].patho);
      userMgmnt.validateUser(usersData[1].fName);
      userMgmnt.searchByFilters(usersData[1].immunoHisto);
      userMgmnt.validateUser(usersData[1].fName);
      cy.reload();
    });

    it("Validate active/inactive users", function () {
      userMgmnt.activeUser(); //checks active users
      userMgmnt.inactiveUser(usersData[0].fName);
      userMgmnt.activeUser(usersData[1].fName);
      cy.reload();
    });
  });

  describe("Modify First User", function () {
    it("Check user to modify", function () {
      userMgmnt.checkUser(usersData[0].fName);
    });

    it("Modify User and Save", function () {
      userMgmnt.modifyUser();
      userMgmnt.typeLoginPassword(usersData[0].password);
      userMgmnt.repeatPassword(usersData[0].password);
      userMgmnt.checkAccountNotLocked();
      userMgmnt.checkAccountEnabled();
      userMgmnt.checkActive();
      userMgmnt.saveChanges();
    });

    it("Validate user is activated", function () {
      userMgmnt = adminPage.goToUserManagementPage();
      userMgmnt.verifyPageTitle();
      userMgmnt.activeUser();
      userMgmnt.activeUser(usersData[0].fName);
    });

    it("Search by Only Administrator", function () {
      userMgmnt.adminUser(); //adding two admin users?
      userMgmnt.validateUser(usersData[0].defaultAdmin);
      userMgmnt.nonAdminUser(usersData[0].fName);
      userMgmnt.nonAdminUser(usersData[1].fName);
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
      loginPage.enterUsername(usersData[0].username);
      loginPage.enterPassword(usersData[0].password); //BUG:there is a password change
      loginPage.signIn();
    });

    it("Navigate back to User Management", () => {
      adminPage = homePage.goToAdminPage();
      userMgmnt = adminPage.goToUserManagementPage();
    });
  });
});
