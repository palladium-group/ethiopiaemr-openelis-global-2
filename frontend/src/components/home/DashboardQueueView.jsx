import React, { useContext, useEffect, useMemo, useState } from "react";
import {
  Button,
  Column,
  DataTable,
  Grid,
  Loading,
  Link,
  Pagination,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tabs,
  TabList,
  TextInput,
} from "@carbon/react";
import { Copy } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  convertAlphaNumLabNumForDisplay,
  getFromOpenElisServer,
} from "../utils/Utils";
import { QUEUE_TILES_WITH_TEST_SECTION_TABS } from "./dashboardConstants";
import { NotificationContext } from "../layout/Layout";
import { NotificationKinds } from "../common/CustomNotification";

const DEFAULT_PAGE_SIZE = 25;

const DashboardQueueView = ({
  listType,
  systemUserId,
  testSections = [],
  showAllTestSectionTab = false,
}) => {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const [patientQuery, setPatientQuery] = useState("");
  const [labNumber, setLabNumber] = useState("");
  const [appliedPatientQuery, setAppliedPatientQuery] = useState("");
  const [appliedLabNumber, setAppliedLabNumber] = useState("");
  const [selectedTestSection, setSelectedTestSection] = useState(null);
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [totalItems, setTotalItems] = useState(0);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);

  const showTestSectionTabs =
    QUEUE_TILES_WITH_TEST_SECTION_TABS.includes(listType) &&
    (showAllTestSectionTab || testSections.length > 0);

  const testSectionTabValues = useMemo(() => {
    if (!showTestSectionTabs) {
      return [];
    }
    if (showAllTestSectionTab) {
      return ["all", ...testSections.map((section) => section.id)];
    }
    return testSections.map((section) => section.id);
  }, [showAllTestSectionTab, showTestSectionTabs, testSections]);

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

  const loadQueue = (
    pageNumber,
    size,
    patientFilter,
    labFilter,
    testSectionFilter,
  ) => {
    setLoading(true);
    setLoadError(false);
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
    if (
      testSectionFilter &&
      testSectionFilter !== "all" &&
      showTestSectionTabs
    ) {
      params.set("testSectionId", testSectionFilter);
    }
    if (systemUserId != null && systemUserId !== "") {
      params.set("systemUserId", String(systemUserId));
    }
    getFromOpenElisServer(
      "/rest/home-dashboard/" + listType + "?" + params.toString(),
      (response) => {
        if (!response || response.queue == null) {
          setLoadError(true);
          setItems([]);
          setTotalItems(0);
          setLoading(false);
          setNotificationVisible(true);
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({ id: "server.error.msg" }),
          });
          return;
        }

        const queue = response.queue;
        setLoadError(false);
        setItems(queue.items || []);
        setTotalItems(queue.totalItems || 0);
        setPage(queue.page || pageNumber);
        setPageSize(queue.pageSize || size);
        setLoading(false);
      },
    );
  };

  useEffect(() => {
    if (!showTestSectionTabs) {
      setSelectedTestSection("all");
      return;
    }
    if (showAllTestSectionTab) {
      setSelectedTestSection("all");
      return;
    }
    if (testSections.length > 0) {
      setSelectedTestSection(testSections[0].id);
    } else {
      setSelectedTestSection(null);
    }
  }, [listType]);

  useEffect(() => {
    if (
      !showTestSectionTabs ||
      showAllTestSectionTab ||
      testSections.length === 0
    ) {
      return;
    }
    setSelectedTestSection((current) => {
      if (
        current === null ||
        !testSections.some((section) => section.id === current)
      ) {
        return testSections[0].id;
      }
      return current;
    });
  }, [showAllTestSectionTab, showTestSectionTabs, testSections]);

  useEffect(() => {
    if (selectedTestSection == null) {
      return;
    }
    setPatientQuery("");
    setLabNumber("");
    setAppliedPatientQuery("");
    setAppliedLabNumber("");
    setPage(1);
    setPageSize(DEFAULT_PAGE_SIZE);
    loadQueue(1, DEFAULT_PAGE_SIZE, "", "", selectedTestSection);
  }, [listType, systemUserId, selectedTestSection]);

  const applySearch = () => {
    setAppliedPatientQuery(patientQuery);
    setAppliedLabNumber(labNumber);
    setPage(1);
    loadQueue(1, pageSize, patientQuery, labNumber, selectedTestSection);
  };

  const handleTestSectionTabChange = ({ selectedIndex }) => {
    const nextSection = testSectionTabValues[selectedIndex];
    if (nextSection && nextSection !== selectedTestSection) {
      setSelectedTestSection(nextSection);
    }
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
    id: item.id || item.accessionNumber,
    priority: item.priority || "",
    orderDate: item.orderDate || "",
    patient: item.patientName || "",
    subjectNumber: item.subjectNumber || "",
    accessionNumber: item.accessionNumber || "",
    labNumber: convertAlphaNumLabNumForDisplay(item.accessionNumber || ""),
    testCount: item.testCount ?? "",
    testNames: item.testNames || "",
  }));

  const selectedTestSectionIndex = testSectionTabValues.indexOf(
    selectedTestSection ?? "",
  );

  return (
    <>
      {showTestSectionTabs ? (
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Tabs
              selectedIndex={
                selectedTestSectionIndex >= 0 ? selectedTestSectionIndex : 0
              }
              onChange={handleTestSectionTabChange}
            >
              {showAllTestSectionTab ? (
                <TabList aria-label="Test section tabs" contained>
                  <Tab>
                    <FormattedMessage id="all.label" />
                  </Tab>
                  {testSections.map((section) => (
                    <Tab key={section.id}>{section.value}</Tab>
                  ))}
                </TabList>
              ) : (
                <TabList aria-label="Test section tabs" contained>
                  {testSections.map((section) => (
                    <Tab key={section.id}>{section.value}</Tab>
                  ))}
                </TabList>
              )}
            </Tabs>
          </Column>
        </Grid>
      ) : null}

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
                        {loadError ? (
                          <FormattedMessage id="dashboard.queue.loadError" />
                        ) : (
                          <FormattedMessage id="dashboard.queue.noResults" />
                        )}
                      </TableCell>
                    </TableRow>
                  ) : (
                    rows.map((row) => {
                      const sourceItem = items.find(
                        (item) => item.id === row.id,
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
            selectedTestSection,
          );
        }}
      />
    </>
  );
};

export default DashboardQueueView;
