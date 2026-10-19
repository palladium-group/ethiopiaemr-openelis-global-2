import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import PathologyCaseWorkflowRail from "./PathologyCaseWorkflowRail";
import messages from "../../languages/en.json";

jest.mock("../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
  postToOpenElisServerFullResponse: jest.fn(),
}));

const { postToOpenElisServerFullResponse } = require("../utils/Utils");

const renderRail = (info) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <PathologyCaseWorkflowRail
        pathologySampleId={7}
        pathologySampleInfo={info}
        onCaseUpdated={() => {}}
      />
    </IntlProvider>,
  );

describe("PathologyCaseWorkflowRail payment hold", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("shows the payment hold instead of Grossing while awaiting payment", () => {
    renderRail({
      status: "AWAITING_PAYMENT",
      paymentStatus: "PENDING",
      labNumber: "26-000123",
    });

    expect(
      screen.getByText("OpenMRS payment status: Pending"),
    ).toBeInTheDocument();
    expect(screen.queryByText("Macroscopic description")).toBeNull();
    expect(postToOpenElisServerFullResponse).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole("button", { name: "Check payment" }));
    expect(postToOpenElisServerFullResponse).toHaveBeenCalledWith(
      "/rest/pathology/caseView/7/checkPayment",
      "{}",
      expect.any(Function),
    );
  });

  test("re-checks payment when a held case is opened", () => {
    renderRail({ status: "AWAITING_PAYMENT", labNumber: "26-000123" });

    expect(postToOpenElisServerFullResponse).toHaveBeenCalledWith(
      "/rest/pathology/caseView/7/checkPayment",
      "{}",
      expect.any(Function),
    );
    expect(screen.getByText("Checking payment status…")).toBeInTheDocument();
  });

  test("shows no payment hold for a case in Grossing", () => {
    renderRail({ status: "GROSSING", labNumber: "26-000123" });

    expect(screen.queryByRole("button", { name: "Check payment" })).toBeNull();
    expect(postToOpenElisServerFullResponse).not.toHaveBeenCalled();
  });
});
