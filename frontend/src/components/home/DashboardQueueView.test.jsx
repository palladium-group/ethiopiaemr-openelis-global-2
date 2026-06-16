import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import DashboardQueueView from "./DashboardQueueView";
import messages from "../../languages/en.json";
import { getFromOpenElisServer } from "../utils/Utils";
import { NotificationContext } from "../layout/Layout";
import { NotificationKinds } from "../common/CustomNotification";

jest.mock("../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
  convertAlphaNumLabNumForDisplay: (value) => value,
}));

const mockNotificationContext = {
  notificationVisible: false,
  setNotificationVisible: jest.fn(),
  addNotification: jest.fn(),
};

const queueResponse = {
  queue: {
    items: [
      {
        id: "analysis-1",
        accessionNumber: "2026-001234",
        patientName: "Jane Doe",
        subjectNumber: "SUB-001",
        priority: "R",
        orderDate: "06/13/2026",
        testCount: 2,
        testNames: "Glucose",
      },
    ],
    page: 1,
    pageSize: 25,
    totalItems: 1,
    totalPages: 1,
  },
};

const renderQueueView = (component) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <NotificationContext.Provider value={mockNotificationContext}>
        {component}
      </NotificationContext.Provider>
    </IntlProvider>,
  );

describe("DashboardQueueView", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback(queueResponse);
    });
  });

  test("loadsQueueFromHomeDashboardApi", async () => {
    renderQueueView(<DashboardQueueView listType="ORDERS_IN_PROGRESS" />);

    await screen.findByText("Jane Doe");

    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/home-dashboard/ORDERS_IN_PROGRESS?page=1&pageSize=25",
      expect.any(Function),
      expect.anything(),
    );
  });

  test("rendersPatientNameAndSubjectNumberInPatientCell", async () => {
    renderQueueView(<DashboardQueueView listType="ORDERS_IN_PROGRESS" />);

    expect(await screen.findByText("Jane Doe")).toBeInTheDocument();

    const subjectNumber = await screen.findByText("SUB-001");
    expect(subjectNumber).toHaveClass("dashboard-queue-subject");
  });

  test("rendersPatientAndAccessionSearchFields", async () => {
    renderQueueView(<DashboardQueueView listType="ORDERS_REJECTED_TODAY" />);

    expect(
      screen.getByLabelText("Patient name, subject number, or ID"),
    ).toBeInTheDocument();
    expect(screen.getByLabelText(/accession/i)).toBeInTheDocument();
    expect(
      document.getElementById("dashboard-queue-patient-ORDERS_REJECTED_TODAY"),
    ).toBeInTheDocument();
  });

  test("showsNoResultsWhenQueueIsEmpty", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback({
        queue: {
          items: [],
          page: 1,
          pageSize: 25,
          totalItems: 0,
          totalPages: 0,
        },
      });
    });

    renderQueueView(<DashboardQueueView listType="ORDERS_COMPLETED_TODAY" />);

    expect(await screen.findByText("No orders found")).toBeInTheDocument();
  });

  test("showsLoadErrorAndNotificationWhenApiFails", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      callback(undefined);
    });

    renderQueueView(<DashboardQueueView listType="ORDERS_IN_PROGRESS" />);

    expect(
      await screen.findByText("Unable to load orders. Please try again."),
    ).toBeInTheDocument();
    expect(screen.queryByText("No orders found")).not.toBeInTheDocument();
    expect(mockNotificationContext.setNotificationVisible).toHaveBeenCalledWith(
      true,
    );
    expect(mockNotificationContext.addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        kind: NotificationKinds.error,
        message: "Oops, Server error please contact administrator",
      }),
    );
  });

  test("passesSystemUserIdForPerUserOrdersTile", async () => {
    renderQueueView(
      <DashboardQueueView listType="ORDERS_FOR_USER" systemUserId="user-42" />,
    );

    await screen.findByText("Jane Doe");

    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/home-dashboard/ORDERS_FOR_USER?page=1&pageSize=25&systemUserId=user-42",
      expect.any(Function),
      expect.anything(),
    );
  });

  test("passesTestSectionIdWhenUserHasAssignedSections", async () => {
    renderQueueView(
      <DashboardQueueView
        listType="ORDERS_IN_PROGRESS"
        testSections={[{ id: "9001", value: "Chemistry" }]}
        showAllTestSectionTab={false}
      />,
    );

    await screen.findByText("Jane Doe");

    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/home-dashboard/ORDERS_IN_PROGRESS?page=1&pageSize=25&testSectionId=9001",
      expect.any(Function),
      expect.anything(),
    );
  });

  test("rendersTestSectionTabsForQueueTiles", async () => {
    renderQueueView(
      <DashboardQueueView
        listType="ORDERS_IN_PROGRESS"
        testSections={[
          { id: "9001", value: "Chemistry" },
          { id: "9002", value: "Hematology" },
        ]}
        showAllTestSectionTab={true}
      />,
    );

    await screen.findByText("Jane Doe");

    expect(screen.getByText("All")).toBeInTheDocument();
    expect(screen.getByText("Chemistry")).toBeInTheDocument();
    expect(screen.getByText("Hematology")).toBeInTheDocument();
  });
});
