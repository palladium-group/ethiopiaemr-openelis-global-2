import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import ReceptionPage from "./Index";
import messages from "../../languages/en.json";

jest.mock("../common/CustomDatePicker", () => () => null);

jest.mock("./ReceptionActionModal", () => () => null);

jest.mock("../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn((url, callback) => {
    if (url.startsWith("/rest/reception/queue")) {
      callback({
        items: [],
        totalItems: 0,
        page: 1,
        pageSize: 25,
        totalPages: 0,
      });
    } else {
      callback([]);
    }
  }),
  convertAlphaNumLabNumForDisplay: (value) => value,
  postToOpenElisServerJsonResponse: jest.fn(),
}));

const renderWithProviders = (component) =>
  render(
    <MemoryRouter>
      <IntlProvider locale="en" messages={messages}>
        {component}
      </IntlProvider>
    </MemoryRouter>,
  );

describe("ReceptionPage", () => {
  test("renders reception header", () => {
    renderWithProviders(<ReceptionPage />);
    expect(screen.getByText("Sample Reception")).toBeInTheDocument();
  });
});
