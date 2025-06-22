// This handles all pages of the admin
import LabNumberManagementPage from "./LabNumberManagementPage";
import MenuConfigPage from "./MenuConfigPage";
import BarcodeConfigPage from "./BarcodeConfigPage";
import ProgramEntryPage from "./ProgramEntryPage";
import ProviderManagementPage from "./ProviderManagementPage";
import OrganizationManagementPage from "./OrganizationManagementPage";
import UserManagementPage from "./UserManagementPage";
import ReflexTestsConfigPage from "./ReflexTestsConfigPage";
import DictionaryMenuPage from "./DictionaryMenu";

class AdminPage {
  constructor() {
    this.selectors = {
      providerManagement: "[data-cy='providerMgmnt']",
      organizationManagement: "[data-cy='orgMgmnt']",
      labNumberManagement: "[data-cy='labNumberMgmnt']",
      globalMenuManagement: "[data-cy='globalMenuMgmnt']",
      barcodeConfig: "[data-cy='barcodeConfig']",
      programEntry: "[data-cy='programEntry']",
      userManagement: "[data-cy='userMgmnt']",
      span: "span",
    };
  }

  visit() {
    cy.visit("/administration");
  }

  goToProviderManagementPage() {
    cy.get(this.selectors.providerManagement).should("be.visible").click();
    cy.url().should("include", "#providerMenu");
    cy.contains("Provider Management").should("be.visible");
    return new ProviderManagementPage();
  }

  goToOrganizationManagement() {
    cy.get(this.selectors.organizationManagement).should("be.visible").click();
    cy.url().should("include", "#organizationManagement");
    cy.contains("Organization Management").should("be.visible");
    return new OrganizationManagementPage();
  }

  goToLabNumberManagementPage() {
    cy.get(this.selectors.labNumberManagement).should("be.visible").click();
    cy.url().should("include", "#labNumber");
    cy.contains("Lab Number Management").should("be.visible");
    return new LabNumberManagementPage();
  }

  goToGlobalMenuConfigPage() {
    cy.contains(this.selectors.span, "Menu Configuration").click();
    cy.get(this.selectors.globalMenuManagement).should("be.visible").click();
    cy.url().should("include", "#globalMenuManagement");
    cy.contains("Global Menu Management").should("be.visible");

    return new MenuConfigPage();
  }

  goToNonConformConfigPage() {
    cy.contains("span", "Menu Configuration").click();
    cy.get("[data-cy='nonConformMenuMgmnt']").click();

    return new MenuConfigPage();
  }

  goToPatientConfigPage() {
    cy.contains("span", "Menu Configuration").click();
    cy.get("[data-cy='patientMenuMgmnt']").click();

    return new MenuConfigPage();
  }

  goToStudyConfigPage() {
    cy.contains("span", "Menu Configuration").click();
    cy.get("[data-cy='studyMenuMgmnt']").click();

    return new MenuConfigPage();
  }

  goToBillingConfigPage() {
    cy.contains("span", "Menu Configuration").click();
    cy.get("[data-cy='billingMenuMgmnt']").click();

    return new MenuConfigPage();
  }

  goToBarcodeConfigPage() {
    cy.get(this.selectors.barcodeConfig).should("be.visible").click();
    return new BarcodeConfigPage();
  }

  goToProgramEntry() {
    cy.get(this.selectors.programEntry).should("be.visible").click();
    return new ProgramEntryPage();
  }

  goToDictionaryMenuPage() {
    cy.get("[data-cy='dictMenu']").should("be.visible").click();
    return new DictionaryMenuPage();
  }


  goToUserManagementPage() {
    cy.get(this.selectors.userManagement).click();
    return new UserManagementPage();
}


  goToReflexTestsManagement() {
    cy.contains("span", "Reflex Tests Configuration").click();
    cy.get("[data-cy='reflex']").click();
    return new ReflexTestsConfigPage();
  }

  goToCalculatedValueTestsManagement() {
    cy.contains("span", "Reflex Tests Configuration").click();
    cy.get("[data-cy='calculatedValue']").click();
    return new ReflexTestsConfigPage();
  }
}
export default AdminPage;
