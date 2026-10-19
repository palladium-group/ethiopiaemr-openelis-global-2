import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import CytologyCaseWorkflowRail from "./CytologyCaseWorkflowRail";
import messages from "../../languages/en.json";

jest.mock("../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
  postToOpenElisServerFullResponse: jest.fn(),
}));

const { postToOpenElisServerFullResponse } = require("../utils/Utils");

const renderRail = (info) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <CytologyCaseWorkflowRail
        cytologySampleId={9}
        pathologySampleInfo={info}
        onCaseUpdated={() => {}}
      />
    </IntlProvider>,
  );

describe("CytologyCaseWorkflowRail payment hold", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("holds a Fluid case on the Cell block step until paid", () => {
    renderRail({
      status: "AWAITING_PAYMENT",
      paymentStatus: "UNKNOWN",
      subtype: "FLUID",
      labNumber: "26-000124",
    });

    expect(
      screen.getByText("OpenMRS payment status: Unknown"),
    ).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Check payment" }));
    expect(postToOpenElisServerFullResponse).toHaveBeenCalledWith(
      "/rest/cytology/caseView/9/checkPayment",
      "{}",
      expect.any(Function),
    );
  });

  test("re-checks payment when a held case is opened", () => {
    renderRail({
      status: "AWAITING_PAYMENT",
      subtype: "FNAC",
      labNumber: "26-000124",
    });

    expect(postToOpenElisServerFullResponse).toHaveBeenCalledWith(
      "/rest/cytology/caseView/9/checkPayment",
      "{}",
      expect.any(Function),
    );
  });
});
