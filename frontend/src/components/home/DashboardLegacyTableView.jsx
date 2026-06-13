import React from "react";
import {
  Button,
  Column,
  DataTable,
  Grid,
  Link,
  Pagination,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
} from "@carbon/react";
import { ArrowLeft, ArrowRight } from "@carbon/react/icons";
import { Copy } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { convertAlphaNumLabNumForDisplay } from "../utils/Utils";

const DashboardLegacyTableView = ({
  selectedTileType,
  data,
  pagination,
  currentApiPage,
  totalApiPages,
  previousPage,
  nextPage,
  onLoadPreviousPage,
  onLoadNextPage,
  page,
  pageSize,
  onPageChange,
  onViewUserOrders,
}) => {
  const intl = useIntl();
  const isUserSummary = selectedTileType === "ORDERS_ENTERED_BY_USER_TODAY";

  const orderHeaders = [
    {
      key: "priority",
      header: <FormattedMessage id="eorder.priority" />,
    },
    {
      key: "orderDate",
      header: <FormattedMessage id="sample.label.orderdate" />,
    },
    {
      key: "patientId",
      header: <FormattedMessage id="patient.id" />,
    },
    {
      key: "labNumber",
      header: <FormattedMessage id="eorder.labNumber" />,
    },
    {
      key: "testName",
      header: <FormattedMessage id="eorder.test.name" />,
    },
  ];

  const userHeaders = [
    {
      key: "userFirstName",
      header: "First Name",
    },
    {
      key: "userLastName",
      header: "Last Name",
    },
    {
      key: "countOfOrdersEntered",
      header: "Orders Entered",
    },
  ];

  const copyLabNumber = async (value) => {
    if (!value) {
      return;
    }
    if ("clipboard" in navigator) {
      await navigator.clipboard.writeText(value);
    } else {
      document.execCommand("copy", true, value);
    }
  };

  const renderCell = (cell, row) => {
    if (cell.info.header === "labNumber" && cell.value) {
      return (
        <TableCell key={cell.id}>
          <div style={{ display: "flex", alignItems: "center" }}>
            <Button
              onClick={() => copyLabNumber(cell.value)}
              kind="ghost"
              iconDescription={intl.formatMessage({
                id: "instructions.copy.labnum",
              })}
              hasIconOnly
              renderIcon={Copy}
            />
            {convertAlphaNumLabNumForDisplay(cell.value)}
          </div>
        </TableCell>
      );
    }
    if (cell.info.header === "countOfOrdersEntered" && cell.value) {
      return (
        <TableCell key={cell.id}>
          <Link style={{ color: "blue" }}>{cell.value}</Link>
        </TableCell>
      );
    }
    return <TableCell key={cell.id}>{cell.value}</TableCell>;
  };

  const rows = data.slice((page - 1) * pageSize, page * pageSize);

  return (
    <Grid>
      <Column lg={16} md={8} sm={4}>
        {pagination && (
          <Grid>
            <Column lg={14} />
            <Column
              lg={2}
              style={{
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                gap: "10px",
                width: "110%",
              }}
            >
              <Link>
                {currentApiPage} / {totalApiPages}
              </Link>
              <div style={{ display: "flex", gap: "10px" }}>
                <Button
                  hasIconOnly
                  id="loadpreviousresults"
                  onClick={onLoadPreviousPage}
                  disabled={previousPage == null}
                  renderIcon={ArrowLeft}
                  iconDescription="previous"
                />
                <Button
                  hasIconOnly
                  id="loadnextresults"
                  onClick={onLoadNextPage}
                  disabled={nextPage == null}
                  renderIcon={ArrowRight}
                  iconDescription="next"
                />
              </div>
            </Column>
          </Grid>
        )}
        <DataTable
          rows={rows}
          headers={isUserSummary ? userHeaders : orderHeaders}
          isSortable
        >
          {({ rows, headers, getHeaderProps, getTableProps }) => (
            <TableContainer title="" description="">
              <Table {...getTableProps()}>
                <TableHead>
                  <TableRow>
                    {headers.map((header) => (
                      <TableHeader
                        key={header.key}
                        {...getHeaderProps({ header })}
                      >
                        {header.header}
                      </TableHeader>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {rows.map((row) => (
                    <TableRow
                      key={row.id}
                      onClick={() => {
                        if (isUserSummary) {
                          onViewUserOrders(row);
                        }
                      }}
                    >
                      {row.cells.map((cell) => renderCell(cell, row))}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </DataTable>
        <Pagination
          onChange={onPageChange}
          page={page}
          pageSize={pageSize}
          pageSizes={[10, 20, 30, 50, 100]}
          totalItems={data.length}
          forwardText={intl.formatMessage({ id: "pagination.forward" })}
          backwardText={intl.formatMessage({ id: "pagination.backward" })}
          itemRangeText={(min, max, total) =>
            intl.formatMessage(
              { id: "pagination.item-range" },
              { min: min, max: max, total: total },
            )
          }
          itemsPerPageText={intl.formatMessage({
            id: "pagination.items-per-page",
          })}
          itemText={(min, max) =>
            intl.formatMessage(
              { id: "pagination.item" },
              { min: min, max: max },
            )
          }
          pageNumberText={intl.formatMessage({ id: "pagination.page-number" })}
          pageRangeText={(_current, total) =>
            intl.formatMessage(
              { id: "pagination.page-range" },
              { total: total },
            )
          }
          pageText={(pageNumber, pagesUnknown) =>
            intl.formatMessage(
              { id: "pagination.page" },
              { page: pagesUnknown ? "" : pageNumber },
            )
          }
        />
      </Column>
    </Grid>
  );
};

export default DashboardLegacyTableView;
