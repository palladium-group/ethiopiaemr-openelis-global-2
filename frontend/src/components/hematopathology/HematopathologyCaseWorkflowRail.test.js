import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import HematopathologyCaseWorkflowRail, {
  hematopathologySteps,
  readFieldsFor,
  stepState,
} from "./HematopathologyCaseWorkflowRail";
import messages from "../../languages/en.json";

jest.mock("../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
  postToOpenElisServerFullResponse: jest.fn(),
}));

const { postToOpenElisServerFullResponse } = require("../utils/Utils");

const renderRail = (info) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <HematopathologyCaseWorkflowRail
        caseId={5}
        caseInfo={info}
        onCaseUpdated={() => {}}
      />
    </IntlProvider>,
  );

describe("hematopathology steps", () => {
  test("morphology has three steps, flow cytometry four", () => {
    expect(
      hematopathologySteps("PERIPHERAL_MORPHOLOGY").map((s) => s.id),
    ).toEqual(["collection", "smearPrep", "review"]);
    expect(hematopathologySteps("FLOW_CYTOMETRY").map((s) => s.id)).toEqual([
      "collection",
      "processing",
      "acquisition",
      "review",
    ]);
  });

  test("only bone marrow cases show the bone marrow morphology field", () => {
    expect(readFieldsFor("PERIPHERAL_MORPHOLOGY")).not.toContain(
      "boneMarrowMorphology",
    );
    expect(readFieldsFor("BONE_MARROW_ASPIRATE")).toContain(
      "boneMarrowMorphology",
    );
    expect(readFieldsFor("FLOW_CYTOMETRY")).toContain("gatingFindings");
  });

  test("step states follow the status", () => {
    const flow = hematopathologySteps("FLOW_CYTOMETRY");
    expect(flow.map((s, i) => stepState(s, i, "ACQUISITION"))).toEqual([
      "completed",
      "completed",
      "active",
      "locked",
    ]);
    expect(flow.map((s, i) => stepState(s, i, "AWAITING_PAYMENT"))).toEqual([
      "completed",
      "active",
      "locked",
      "locked",
    ]);
    expect(flow.map((s, i) => stepState(s, i, "REJECTED"))).toEqual([
      "completed",
      "locked",
      "locked",
      "locked",
    ]);
  });
});

describe("HematopathologyCaseWorkflowRail", () => {
  beforeEach(() => jest.clearAllMocks());

  test("confirms collection of a bone marrow case with spicules", () => {
    renderRail({
      status: "RECEIVED",
      subtype: "BONE_MARROW_ASPIRATE",
      labNumber: "HEME26000000001",
    });
    fireEvent.click(screen.getByLabelText("Spicules present in the aspirate"));
    fireEvent.click(screen.getByText("Confirm collection"));
    expect(postToOpenElisServerFullResponse).toHaveBeenCalledWith(
      "/rest/hematopathology/caseView/5/confirmCollection",
      expect.stringContaining('"spiculesPresent":true'),
      expect.any(Function),
    );
  });

  test("holds a paid-pending case on the first lab step", () => {
    renderRail({
      status: "AWAITING_PAYMENT",
      paymentStatus: "PENDING",
      subtype: "PERIPHERAL_MORPHOLOGY",
    });
    expect(screen.getByText("Check payment")).toBeInTheDocument();
  });

  test("sign-out needs a conclusion", () => {
    renderRail({
      status: "READY_FOR_HEMATOPATHOLOGIST",
      subtype: "FLOW_CYTOMETRY",
    });
    expect(
      screen.getByText("Sign out and finalize").closest("button"),
    ).toBeDisabled();
    expect(screen.queryByText("Smear quality")).not.toBeInTheDocument();
    expect(screen.getByText("Antibody panel")).toBeInTheDocument();
  });
});
