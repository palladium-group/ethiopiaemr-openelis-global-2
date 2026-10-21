import React from "react";
import { act, fireEvent, render, screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import CaseSupplyOrders, { lineIsValid } from "./CaseSupplyOrders";
import messages from "../../languages/en.json";

jest.mock("../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
  postToOpenElisServerJsonResponse: jest.fn(),
}));

jest.mock("../layout/Layout", () => {
  const React = require("react");
  return { ConfigurationContext: React.createContext({}) };
});

const {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} = require("../utils/Utils");
const { ConfigurationContext } = require("../layout/Layout");

const REQUESTS = [
  {
    id: 3,
    requestedAt: 1792570000000,
    requestedBy: "Abebe Kebede",
    status: "FAILED",
    failureReason: "OpenMRS is unreachable",
    items: [
      {
        conceptName: "Xylene",
        quantity: 1,
        quantityUnitsName: "Litre",
        urgency: "STAT",
      },
    ],
  },
  {
    id: 2,
    requestedBy: "Abebe Kebede",
    status: "SENT",
    daguStatus: "FAILED",
    daguReason: "Item not in Dagu catalog",
    items: [{ conceptName: "Slides", quantity: 50, urgency: "ROUTINE" }],
  },
  {
    id: 1,
    requestedBy: "Abebe Kebede",
    status: "SENT",
    daguStatus: "SUCCESS",
    items: [{ conceptName: "Gloves", quantity: 2, urgency: "ROUTINE" }],
  },
  {
    id: 4,
    requestedBy: "Abebe Kebede",
    status: "SENT",
    items: [{ conceptName: "Formalin", quantity: 1, urgency: "ROUTINE" }],
  },
];

const respondTo = (routes) => {
  getFromOpenElisServer.mockImplementation((url, callback) => {
    const match = Object.keys(routes).find((prefix) => url.startsWith(prefix));
    callback(match ? routes[match] : undefined);
  });
};

const renderPanel = ({ enabled = "true", caseClosed = false } = {}) =>
  render(
    <ConfigurationContext.Provider
      value={{
        configurationProperties: { PATHOLOGY_SUPPLY_ORDER_ENABLED: enabled },
      }}
    >
      <IntlProvider locale="en" messages={messages}>
        <CaseSupplyOrders
          caseType="pathology"
          caseId={7}
          caseClosed={caseClosed}
        />
      </IntlProvider>
    </ConfigurationContext.Provider>,
  );

describe("CaseSupplyOrders", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    respondTo({
      "/rest/pathology/caseView/7/supplyRequests": {
        fromOpenMrs: true,
        requests: REQUESTS,
      },
      "/rest/pathology/supplyOrder/units": [{ uuid: "u-box", display: "Box" }],
      "/rest/pathology/supplyOrder/catalog": [
        { uuid: "c-gloves", display: "Gloves" },
      ],
    });
  });

  test("renders nothing when supply ordering is turned off", () => {
    const { container } = renderPanel({ enabled: "false" });

    expect(container.innerHTML).toBe("");
    expect(getFromOpenElisServer).not.toHaveBeenCalled();
  });

  test("lists the case's requests with their OpenMRS and Dagu status", () => {
    renderPanel();

    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/pathology/caseView/7/supplyRequests",
      expect.any(Function),
    );
    expect(screen.getByText("Medical supplies (4)")).toBeInTheDocument();
    expect(screen.getByText("Not sent to OpenMRS")).toBeInTheDocument();
    expect(screen.getByText("OpenMRS is unreachable")).toBeInTheDocument();
    expect(screen.getByText("Rejected by Dagu")).toBeInTheDocument();
    expect(screen.getByText("Item not in Dagu catalog")).toBeInTheDocument();
    expect(screen.getByText("Received by Dagu")).toBeInTheDocument();
    expect(screen.getByText("Awaiting Dagu")).toBeInTheDocument();
    expect(screen.getByText(/Xylene × 1 Litre/)).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Order supplies" }),
    ).toBeEnabled();
  });

  test("hides ordering for cases that did not come from OpenMRS", () => {
    respondTo({
      "/rest/pathology/caseView/7/supplyRequests": {
        fromOpenMrs: false,
        requests: [],
      },
    });

    renderPanel();

    expect(
      screen.getByText(
        "Supplies can be ordered only for cases that came from OpenMRS.",
      ),
    ).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Order supplies" })).toBeNull();
  });

  test("a closed case cannot order or retry", () => {
    renderPanel({ caseClosed: true });

    expect(
      screen.getByRole("button", { name: "Order supplies" }),
    ).toBeDisabled();
    expect(screen.queryByRole("button", { name: "Retry" })).toBeNull();
  });

  test("retry re-sends the request and shows the new status", () => {
    postToOpenElisServerJsonResponse.mockImplementation((url, body, callback) =>
      callback({ ...REQUESTS[0], status: "SENT", failureReason: null }),
    );
    renderPanel();

    fireEvent.click(screen.getByRole("button", { name: "Retry" }));

    expect(postToOpenElisServerJsonResponse).toHaveBeenCalledWith(
      "/rest/pathology/caseView/7/supplyRequests/3/retry",
      "{}",
      expect.any(Function),
    );
    expect(screen.queryByText("Not sent to OpenMRS")).toBeNull();
    expect(screen.getAllByText("Awaiting Dagu")).toHaveLength(2);
  });

  test("orders a searched supply and lists it", () => {
    jest.useFakeTimers();
    postToOpenElisServerJsonResponse.mockImplementation((url, body, callback) =>
      callback({
        id: 9,
        requestedBy: "Abebe Kebede",
        status: "SENT",
        items: [
          {
            conceptName: "Gloves",
            quantity: 3,
            quantityUnitsName: "Box",
            urgency: "ROUTINE",
          },
        ],
      }),
    );
    renderPanel();
    fireEvent.click(screen.getByRole("button", { name: "Order supplies" }));
    const dialog = screen.getByRole("dialog");

    fireEvent.change(within(dialog).getByRole("combobox"), {
      target: { value: "glo" },
    });
    act(() => {
      jest.advanceTimersByTime(400);
    });
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/pathology/supplyOrder/catalog?q=glo",
      expect.any(Function),
    );
    fireEvent.click(within(dialog).getByText("Gloves"));

    fireEvent.change(within(dialog).getByLabelText("Quantity"), {
      target: { value: "3" },
    });
    fireEvent.change(within(dialog).getByLabelText("Unit"), {
      target: { value: "u-box" },
    });
    fireEvent.click(
      within(dialog).getByRole("button", { name: "Submit order (1 item)" }),
    );

    const [url, body] = postToOpenElisServerJsonResponse.mock.calls[0];
    expect(url).toBe("/rest/pathology/caseView/7/supplyRequests");
    expect(JSON.parse(body)).toEqual({
      items: [
        {
          conceptUuid: "c-gloves",
          conceptName: "Gloves",
          quantity: 3,
          quantityUnitsUuid: "u-box",
          quantityUnitsName: "Box",
          urgency: "ROUTINE",
          instructions: null,
        },
      ],
    });
    expect(screen.getByText("Medical supplies (5)")).toBeInTheDocument();
    expect(
      screen.getByText("Order sent to OpenMRS. It goes to Dagu from there."),
    ).toBeInTheDocument();
    jest.useRealTimers();
  });

  test("explains why OpenMRS refused the order", () => {
    jest.useFakeTimers();
    postToOpenElisServerJsonResponse.mockImplementation((url, body, callback) =>
      callback({
        code: "NO_ACTIVE_VISIT",
        message: "no visit",
        status: 409,
        statusCode: 409,
      }),
    );
    renderPanel();
    fireEvent.click(screen.getByRole("button", { name: "Order supplies" }));
    const dialog = screen.getByRole("dialog");
    fireEvent.change(within(dialog).getByRole("combobox"), {
      target: { value: "glo" },
    });
    act(() => {
      jest.advanceTimersByTime(400);
    });
    fireEvent.click(within(dialog).getByText("Gloves"));

    fireEvent.click(
      within(dialog).getByRole("button", { name: "Submit order (1 item)" }),
    );

    expect(
      within(dialog).getByText(
        "The patient has no active visit in OpenMRS. Ask the clinic to start a visit, then try again.",
      ),
    ).toBeInTheDocument();
    expect(screen.getByText("Medical supplies (4)")).toBeInTheDocument();
    jest.useRealTimers();
  });

  test("quantity steppers change the value without crashing", () => {
    jest.useFakeTimers();
    renderPanel();
    fireEvent.click(screen.getByRole("button", { name: "Order supplies" }));
    const dialog = screen.getByRole("dialog");
    fireEvent.change(within(dialog).getByRole("combobox"), {
      target: { value: "glo" },
    });
    act(() => {
      jest.advanceTimersByTime(400);
    });
    fireEvent.click(within(dialog).getByText("Gloves"));

    fireEvent.click(within(dialog).getAllByTitle(/increment/i)[0]);
    expect(within(dialog).getByLabelText("Quantity")).toHaveValue(2);

    fireEvent.change(within(dialog).getByLabelText("Quantity"), {
      target: { value: "" },
    });
    fireEvent.click(within(dialog).getAllByTitle(/increment/i)[0]);
    expect(
      within(dialog).getByRole("button", { name: /Submit order/ }),
    ).toBeDisabled();
    jest.useRealTimers();
  });

  test("a line needs a whole quantity of at least 1 and a known priority", () => {
    const line = { quantity: 1, urgency: "ROUTINE", instructions: "" };
    expect(lineIsValid(line)).toBe(true);
    expect(lineIsValid({ ...line, quantity: 0 })).toBe(false);
    expect(lineIsValid({ ...line, quantity: 1.5 })).toBe(false);
    expect(lineIsValid({ ...line, quantity: null })).toBe(false);
    expect(lineIsValid({ ...line, urgency: "ON_SCHEDULED_DATE" })).toBe(false);
    expect(lineIsValid({ ...line, instructions: "x".repeat(501) })).toBe(false);
  });
});
