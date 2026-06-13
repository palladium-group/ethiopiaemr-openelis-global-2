import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import DashboardQueueView from "./DashboardQueueView";
import messages from "../../languages/en.json";
import { getFromOpenElisServer } from "../utils/Utils";

jest.mock("../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
  convertAlphaNumLabNumForDisplay: (value) => value,
}));

const queueResponse = {
  queue: {
    items: [
      {
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

const renderQueueView = (listType = "ORDERS_IN_PROGRESS", systemUserId) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <DashboardQueueView listType={listType} systemUserId={systemUserId} />
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
    renderQueueView("ORDERS_IN_PROGRESS");

    await screen.findByText("Jane Doe");

    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/home-dashboard/ORDERS_IN_PROGRESS?page=1&pageSize=25",
      expect.any(Function),
    );
  });

  test("rendersPatientNameAndSubjectNumberInPatientCell", async () => {
    renderQueueView("ORDERS_IN_PROGRESS");

    expect(await screen.findByText("Jane Doe")).toBeInTheDocument();

    const subjectNumber = await screen.findByText("SUB-001");
    expect(subjectNumber).toHaveClass("dashboard-queue-subject");
  });

  test("rendersPatientAndAccessionSearchFields", async () => {
    renderQueueView("ORDERS_REJECTED_TODAY");

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

    renderQueueView("ORDERS_COMPLETED_TODAY");

    expect(await screen.findByText("No orders found")).toBeInTheDocument();
  });

  test("passesSystemUserIdForPerUserOrdersTile", async () => {
    renderQueueView("ORDERS_FOR_USER", "user-42");

    await screen.findByText("Jane Doe");

    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/home-dashboard/ORDERS_FOR_USER?page=1&pageSize=25&systemUserId=user-42",
      expect.any(Function),
    );
  });
});
