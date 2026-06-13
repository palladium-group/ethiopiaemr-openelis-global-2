import React, { useEffect, useState } from "react";
import {
  Button,
  Column,
  DataTable,
  Grid,
  Loading,
  Link,
  Pagination,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  TextInput,
} from "@carbon/react";
import { Copy } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  convertAlphaNumLabNumForDisplay,
  getFromOpenElisServer,
} from "../utils/Utils";

const DEFAULT_PAGE_SIZE = 25;

const DashboardQueueView = ({ listType, systemUserId }) => {
  const intl = useIntl();
  const [patientQuery, setPatientQuery] = useState("");
  const [labNumber, setLabNumber] = useState("");
  const [appliedPatientQuery, setAppliedPatientQuery] = useState("");
  const [appliedLabNumber, setAppliedLabNumber] = useState("");
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [totalItems, setTotalItems] = useState(0);
  const [loading, setLoading] = useState(true);

  const headers = [
    {
      key: "priority",
      header: intl.formatMessage({ id: "eorder.priority" }),
    },
    {
      key: "orderDate",
      header: intl.formatMessage({ id: "sample.label.orderdate" }),
    },
    {
      key: "patient",
      header: intl.formatMessage({ id: "patient.label.name" }),
    },
    {
      key: "labNumber",
      header: intl.formatMessage({ id: "eorder.labNumber" }),
    },
    {
      key: "testCount",
      header: intl.formatMessage({
        id: "result.entry.table.header.testCount",
      }),
    },
    {
      key: "testNames",
      header: intl.formatMessage({ id: "eorder.test.name" }),
    },
  ];

  const loadQueue = (pageNumber, size, patientFilter, labFilter) => {
    setLoading(true);
    const params = new URLSearchParams({
      page: String(pageNumber),
      pageSize: String(size),
    });
    if (patientFilter.trim()) {
      params.set("patientQuery", patientFilter.trim());
    }
    if (labFilter.trim()) {
      params.set("labNumber", labFilter.trim());
    }
    if (systemUserId != null && systemUserId !== "") {
      params.set("systemUserId", String(systemUserId));
    }
    getFromOpenElisServer(
      "/rest/home-dashboard/" + listType + "?" + params.toString(),
      (response) => {
        const queue = response?.queue || {};
        setItems(queue.items || []);
        setTotalItems(queue.totalItems || 0);
        setPage(queue.page || pageNumber);
        setPageSize(queue.pageSize || size);
        setLoading(false);
      },
    );
  };

  useEffect(() => {
    setPatientQuery("");
    setLabNumber("");
    setAppliedPatientQuery("");
    setAppliedLabNumber("");
    setPage(1);
    setPageSize(DEFAULT_PAGE_SIZE);
    loadQueue(1, DEFAULT_PAGE_SIZE, "", "");
  }, [listType, systemUserId]);

  const applySearch = () => {
    setAppliedPatientQuery(patientQuery);
    setAppliedLabNumber(labNumber);
    setPage(1);
    loadQueue(1, pageSize, patientQuery, labNumber);
  };

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

  const getAccessionHref = (accessionNumber) => {
    if (listType === "ORDERS_IN_PROGRESS") {
      return (
        "/result?type=order&doRange=false&accessionNumber=" + accessionNumber
      );
    }
    if (listType === "ORDERS_READY_FOR_VALIDATION") {
      return "validation?type=order&accessionNumber=" + accessionNumber;
    }
    if (listType === "PENDING_RECEPTION") {
      return "/Reception?accessionNumber=" + accessionNumber;
    }
    return null;
  };

  const rows = items.map((item) => ({
    id: item.accessionNumber || item.id,
    priority: item.priority || "",
    orderDate: item.orderDate || "",
    patient: item.patientName || "",
    subjectNumber: item.subjectNumber || "",
    accessionNumber: item.accessionNumber || "",
    labNumber: convertAlphaNumLabNumForDisplay(item.accessionNumber || ""),
    testCount: item.testCount ?? "",
    testNames: item.testNames || "",
  }));

  return (
    <>
      <Grid fullWidth={true}>
        <Column lg={5} md={4} sm={4}>
          <TextInput
            id={"dashboard-queue-patient-" + listType}
            labelText={intl.formatMessage({
              id: "dashboard.queue.search.patient",
            })}
            value={patientQuery}
            onChange={(event) => setPatientQuery(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter") {
                applySearch();
              }
            }}
          />
        </Column>
        <Column lg={5} md={4} sm={4}>
          <TextInput
            id={"dashboard-queue-lab-" + listType}
            labelText={intl.formatMessage({
              id: "reception.search.accession",
            })}
            value={labNumber}
            onChange={(event) => setLabNumber(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter") {
                applySearch();
              }
            }}
          />
        </Column>
        <Column lg={2} md={4} sm={4} style={{ marginTop: "1.5rem" }}>
          <Button onClick={applySearch}>
            <FormattedMessage id="label.button.search" />
          </Button>
        </Column>
      </Grid>

      {loading ? (
        <Loading withOverlay={false} small />
      ) : (
        <DataTable rows={rows} headers={headers} isSortable={false}>
          {({ rows, headers, getTableProps, getHeaderProps, getRowProps }) => (
            <TableContainer>
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
                  {rows.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={headers.length}>
                        <FormattedMessage id="dashboard.queue.noResults" />
                      </TableCell>
                    </TableRow>
                  ) : (
                    rows.map((row) => {
                      const sourceItem = items.find(
                        (item) => (item.accessionNumber || item.id) === row.id,
                      );
                      const accessionNumber = sourceItem?.accessionNumber || "";
                      const accessionHref = getAccessionHref(accessionNumber);
                      const subjectNumber = sourceItem?.subjectNumber || "";

                      return (
                        <TableRow key={row.id} {...getRowProps({ row })}>
                          {row.cells.map((cell) => {
                            if (cell.info.header === "patient") {
                              return (
                                <TableCell key={cell.id}>
                                  <div>{cell.value}</div>
                                  {subjectNumber ? (
                                    <div className="dashboard-queue-subject">
                                      {subjectNumber}
                                    </div>
                                  ) : null}
                                </TableCell>
                              );
                            }
                            if (cell.info.header === "labNumber") {
                              return (
                                <TableCell key={cell.id}>
                                  <div
                                    style={{
                                      display: "flex",
                                      alignItems: "center",
                                    }}
                                  >
                                    <Button
                                      onClick={() =>
                                        copyLabNumber(accessionNumber)
                                      }
                                      kind="ghost"
                                      iconDescription={intl.formatMessage({
                                        id: "instructions.copy.labnum",
                                      })}
                                      hasIconOnly
                                      renderIcon={Copy}
                                    />
                                    {accessionHref ? (
                                      <Link href={accessionHref}>
                                        {cell.value}
                                      </Link>
                                    ) : (
                                      cell.value
                                    )}
                                  </div>
                                </TableCell>
                              );
                            }
                            return (
                              <TableCell key={cell.id}>{cell.value}</TableCell>
                            );
                          })}
                        </TableRow>
                      );
                    })
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </DataTable>
      )}

      <Pagination
        page={page}
        pageSize={pageSize}
        pageSizes={[10, 25, 50, 100]}
        totalItems={totalItems}
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
          intl.formatMessage({ id: "pagination.item" }, { min: min, max: max })
        }
        pageNumberText={intl.formatMessage({ id: "pagination.page-number" })}
        pageRangeText={(_current, total) =>
          intl.formatMessage({ id: "pagination.page-range" }, { total: total })
        }
        pageText={(pageNumber, pagesUnknown) =>
          intl.formatMessage(
            { id: "pagination.page" },
            { page: pagesUnknown ? "" : pageNumber },
          )
        }
        onChange={({ page: nextPage, pageSize: nextPageSize }) => {
          setPage(nextPage);
          setPageSize(nextPageSize);
          loadQueue(
            nextPage,
            nextPageSize,
            appliedPatientQuery,
            appliedLabNumber,
          );
        }}
      />
    </>
  );
};

export default DashboardQueueView;
