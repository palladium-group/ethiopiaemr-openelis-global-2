import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Heading,
  Select,
  SelectItem,
  Button,
  Grid,
  Column,
  Section,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Tile,
  Loading,
  Pagination,
  Search,
  Checkbox,
} from "@carbon/react";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
  hasRole,
} from "../utils/Utils";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog } from "../common/CustomNotification";
import { FormattedMessage, useIntl } from "react-intl";
import "./PathologyDashboard.css";
import PageBreadCrumb from "../common/PageBreadCrumb";

const HISTOPATHOLOGY = "PATH";
const CYTOPATHOLOGY = "CYTO";

/**
 * Per-service-category wiring. Histopathology and cytopathology keep their own case views,
 * assignment endpoints, specialist lists and review status — only the queue is shared.
 */
const CATEGORY_CONFIG = {
  [HISTOPATHOLOGY]: {
    filterValue: "HISTOPATHOLOGY",
    caseViewPath: "/PathologyCaseView/",
    specialistsUrl: "/rest/pathology/pathologists",
    assignUrl: (caseId) =>
      "/rest/pathology/assignPathologist?pathologySampleId=" + caseId,
    reviewStatusCode: "READY_PATHOLOGIST",
    specialistRole: "Pathologist",
  },
  [CYTOPATHOLOGY]: {
    filterValue: "CYTOPATHOLOGY",
    caseViewPath: "/CytologyCaseView/",
    specialistsUrl: "/rest/cytology/cytopathologists",
    assignUrl: (caseId) =>
      "/rest/cytology/assignCytoPathologist?cytologySampleId=" + caseId,
    reviewStatusCode: "READY_FOR_CYTOPATHOLOGIST",
    specialistRole: "Cytopathologist",
  },
};

/**
 * One reception queue for every pathology service category. Rows carry their own category, so the
 * table can mix histopathology and cytopathology cases while each one still opens its own case
 * view and assigns its own kind of specialist.
 */
function PathologyReceptionDashboard() {
  const componentMounted = useRef(false);
  const searchRequestId = useRef(0);
  const intl = useIntl();

  const { notificationVisible } = useContext(NotificationContext);
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const specialistDefaultsApplied = useRef(false);

  const [entries, setEntries] = useState([]);
  const [specialists, setSpecialists] = useState({
    [HISTOPATHOLOGY]: [],
    [CYTOPATHOLOGY]: [],
  });
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(100);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState({
    searchTerm: "",
    myCases: false,
    // Reception opens on unassigned; specialists are switched to Received + My cases below.
    bucket: "UNASSIGNED",
    serviceCategory: "ALL",
  });
  const [counts, setCounts] = useState({
    unassigned: 0,
    received: 0,
    inProgress: 0,
    awaitingReview: 0,
    additionalRequests: 0,
    complete: 0,
  });

  useEffect(() => {
    if (specialistDefaultsApplied.current || !userSessionDetails?.roles) {
      return;
    }
    specialistDefaultsApplied.current = true;
    const isSpecialist =
      hasRole(userSessionDetails, "Pathologist") ||
      hasRole(userSessionDetails, "Cytopathologist");
    if (isSpecialist) {
      setFilters((prev) => ({
        ...prev,
        myCases: true,
        bucket: "RECEIVED",
      }));
    }
  }, [userSessionDetails]);

  const categoryParameters = () => {
    if (filters.serviceCategory === "ALL") {
      return "";
    }
    return "&serviceCategories=" + filters.serviceCategory;
  };

  const filtersToParameters = () => {
    return (
      "bucket=" +
      filters.bucket +
      "&searchTerm=" +
      encodeURIComponent(filters.searchTerm || "") +
      "&assignedToMe=" +
      (filters.myCases ? "true" : "false") +
      categoryParameters()
    );
  };

  const setEntriesWithIds = (data, requestId) => {
    if (!componentMounted.current) {
      return;
    }
    if (requestId !== searchRequestId.current) {
      return;
    }
    if (Array.isArray(data) && data.length > 0) {
      // rowId is category + case id: the two case tables have independent id sequences.
      setEntries(data.map((entry) => ({ ...entry, id: entry.rowId })));
    } else {
      setEntries([]);
    }
    setLoading(false);
  };

  const loadCounts = (data) => {
    if (!componentMounted.current || !data) {
      return;
    }
    setCounts({
      unassigned: data.unassigned ?? 0,
      received: data.received ?? 0,
      inProgress: data.inProgress ?? 0,
      awaitingReview: data.awaitingReview ?? 0,
      additionalRequests: data.additionalRequests ?? 0,
      complete: data.complete ?? 0,
    });
  };

  const refreshItems = () => {
    const requestId = ++searchRequestId.current;
    getFromOpenElisServer(
      "/rest/pathology/reception/dashboard?" + filtersToParameters(),
      (data) => setEntriesWithIds(data, requestId),
    );
  };

  const refreshCounts = () => {
    getFromOpenElisServer(
      "/rest/pathology/reception/dashboard/count?" +
        "assignedToMe=" +
        (filters.myCases ? "true" : "false") +
        categoryParameters(),
      loadCounts,
    );
  };

  const refreshSpecialists = () => {
    Object.entries(CATEGORY_CONFIG).forEach(([code, config]) => {
      getFromOpenElisServer(config.specialistsUrl, (list) => {
        if (componentMounted.current && Array.isArray(list)) {
          setSpecialists((previous) => ({ ...previous, [code]: list }));
        }
      });
    });
  };

  const entryForRow = (row) => entries.find((entry) => entry.rowId === row.id);

  const openCaseView = (row) => {
    const entry = entryForRow(row);
    const config = entry && CATEGORY_CONFIG[entry.serviceCategoryCode];
    if (!config) {
      return;
    }
    window.location.href = config.caseViewPath + entry.caseId;
  };

  const afterAssignment = () => {
    refreshItems();
    refreshCounts();
    refreshSpecialists();
  };

  const assignSpecialist = (event, entry, specialistId) => {
    event.stopPropagation();
    if (!specialistId) {
      return;
    }
    const config = CATEGORY_CONFIG[entry.serviceCategoryCode];
    postToOpenElisServerFullResponse(
      config.assignUrl(entry.caseId) +
        "&pathologistId=" +
        encodeURIComponent(specialistId),
      {},
      afterAssignment,
    );
  };

  const assignCurrentUser = (event, entry) => {
    event.stopPropagation();
    const config = CATEGORY_CONFIG[entry.serviceCategoryCode];
    postToOpenElisServerFullResponse(
      config.assignUrl(entry.caseId),
      {},
      afterAssignment,
    );
  };

  const handlePageChange = (pageInfo) => {
    if (page != pageInfo.page) {
      setPage(pageInfo.page);
    }
    if (pageSize != pageInfo.pageSize) {
      setPageSize(pageInfo.pageSize);
    }
  };

  const renderCell = (cell, row) => {
    const entry = entryForRow(row);

    if (cell.info.header === "assignedSpecialist" && entry && !cell.value) {
      const config = CATEGORY_CONFIG[entry.serviceCategoryCode];
      const options = specialists[entry.serviceCategoryCode] || [];
      return (
        <TableCell key={cell.id} onClick={(e) => e.stopPropagation()}>
          <Select
            id={"assign-specialist-" + entry.rowId}
            labelText=""
            hideLabel
            size="sm"
            defaultValue=""
            onClick={(e) => e.stopPropagation()}
            onChange={(e) => assignSpecialist(e, entry, e.target.value)}
          >
            <SelectItem
              value=""
              text={intl.formatMessage({
                id: "pathology.label.assignPathologist",
              })}
            />
            {options.map((option) => (
              <SelectItem
                key={option.id}
                value={option.id}
                text={`${option.value} (${option.openCaseload ?? 0})`}
              />
            ))}
          </Select>
          {entry.statusCode === config.reviewStatusCode &&
            hasRole(userSessionDetails, config.specialistRole) && (
              <Button
                type="button"
                size="sm"
                kind="ghost"
                className="assign-self-button"
                onClick={(e) => assignCurrentUser(e, entry)}
              >
                <FormattedMessage id="label.button.start" />
              </Button>
            )}
        </TableCell>
      );
    }

    return <TableCell key={cell.id}>{cell.value}</TableCell>;
  };

  const rowClassName = (row) => {
    const entry = entryForRow(row);
    return entry?.statusCode === "RECEIVED" ? "received-row" : undefined;
  };

  function formatDateToDDMMYYYY(date) {
    var day = date.getDate();
    var month = date.getMonth() + 1;
    var year = date.getFullYear();

    var formattedDay = (day < 10 ? "0" : "") + day;
    var formattedMonth = (month < 10 ? "0" : "") + month;

    return formattedDay + "/" + formattedMonth + "/" + year;
  }

  const getPastWeek = () => {
    var currentDate = new Date();
    var pastWeekDate = new Date(currentDate);
    pastWeekDate.setDate(currentDate.getDate() - 7);

    return (
      formatDateToDDMMYYYY(pastWeekDate) +
      " - " +
      formatDateToDDMMYYYY(currentDate)
    );
  };

  const tileList = [
    {
      key: "unassigned",
      bucket: "UNASSIGNED",
      title: <FormattedMessage id="pathology.label.unassigned" />,
      count: counts.unassigned,
      className: "dashboard-tile unassigned-tile",
    },
    {
      key: "received",
      bucket: "RECEIVED",
      title: <FormattedMessage id="pathology.label.received" />,
      count: counts.received,
      className: "dashboard-tile received-tile",
    },
    {
      key: "inProgress",
      bucket: "IN_PROGRESS",
      title: <FormattedMessage id="pathology.label.casesInProgress" />,
      count: counts.inProgress,
      className: "dashboard-tile",
    },
    {
      key: "awaitingReview",
      bucket: "AWAITING_REVIEW",
      title: <FormattedMessage id="pathology.reception.awaitingReview" />,
      count: counts.awaitingReview,
      className: "dashboard-tile",
    },
    {
      key: "additionalRequests",
      bucket: "ADDITIONAL_REQUEST",
      title: <FormattedMessage id="pathology.label.requests" />,
      count: counts.additionalRequests,
      className: "dashboard-tile",
    },
    {
      key: "complete",
      bucket: "COMPLETED",
      title:
        intl.formatMessage({ id: "pathology.label.complete" }) +
        "(Week " +
        getPastWeek() +
        " )",
      count: counts.complete,
      className: "dashboard-tile",
    },
  ];

  useEffect(() => {
    componentMounted.current = true;
    refreshSpecialists();
    return () => {
      componentMounted.current = false;
    };
  }, []);

  useEffect(() => {
    componentMounted.current = true;
    setPage(1);
    setLoading(true);
    refreshItems();
    refreshCounts();
    return () => {
      componentMounted.current = false;
    };
  }, [filters]);

  let breadcrumbs = [{ label: "home.label", link: "/" }];

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      {loading && <Loading description="Loading Dasboard..." />}
      <PageBreadCrumb breadcrumbs={breadcrumbs} />

      <Grid fullWidth={true}>
        <Column lg={16}>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage id="pathology.label.title" />
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>
      <div className="dashboard-container dashboard-container-6">
        {tileList.map((tile) => (
          <Tile
            key={tile.key}
            className={tile.className}
            onClick={() => setFilters({ ...filters, bucket: tile.bucket })}
            style={{ cursor: "pointer" }}
          >
            <h3 className="tile-title">{tile.title}</h3>
            <p className="tile-value">{tile.count}</p>
          </Tile>
        ))}
      </div>
      <div className="orderLegendBody">
        <Grid fullWidth={true} className="gridBoundary">
          <Column lg={6} md={4} sm={2}>
            <Search
              size="sm"
              value={filters.searchTerm}
              onChange={(e) =>
                setFilters({ ...filters, searchTerm: e.target.value })
              }
              placeholder={intl.formatMessage({
                id: "label.search.labno.family",
              })}
              labelText={intl.formatMessage({
                id: "label.search.labno.family",
              })}
            />
          </Column>
          <Column lg={10} md={4} sm={2}>
            <div className="inlineDivBlock">
              <div>
                <FormattedMessage id="filters.label" />:
              </div>
              <Checkbox
                labelText={intl.formatMessage({ id: "label.filters.mycases" })}
                id="filterMyCases"
                checked={!!filters.myCases}
                onChange={(e) =>
                  setFilters({
                    ...filters,
                    myCases: e.currentTarget.checked,
                  })
                }
              />
              <Select
                id="serviceCategoryFilter"
                name="serviceCategoryFilter"
                labelText={intl.formatMessage({
                  id: "pathology.label.serviceCategory",
                })}
                value={filters.serviceCategory}
                onChange={(e) =>
                  setFilters({ ...filters, serviceCategory: e.target.value })
                }
                noLabel
              >
                <SelectItem
                  value="ALL"
                  text={intl.formatMessage({
                    id: "pathology.serviceCategory.all",
                  })}
                />
                <SelectItem
                  value={CATEGORY_CONFIG[HISTOPATHOLOGY].filterValue}
                  text={intl.formatMessage({
                    id: "pathology.serviceCategory.histopathology",
                  })}
                />
                <SelectItem
                  value={CATEGORY_CONFIG[CYTOPATHOLOGY].filterValue}
                  text={intl.formatMessage({
                    id: "pathology.serviceCategory.cytopathology",
                  })}
                />
              </Select>
              <Select
                id="statusFilter"
                name="statusFilter"
                labelText={intl.formatMessage({ id: "label.filters.status" })}
                value={filters.bucket}
                onChange={(e) =>
                  setFilters({ ...filters, bucket: e.target.value })
                }
                noLabel
              >
                <SelectItem
                  value="UNASSIGNED"
                  text={intl.formatMessage({
                    id: "pathology.label.unassigned",
                  })}
                />
                <SelectItem
                  value="RECEIVED"
                  text={intl.formatMessage({
                    id: "pathology.label.received",
                  })}
                />
                <SelectItem
                  value="IN_PROGRESS"
                  text={intl.formatMessage({
                    id: "pathology.label.casesInProgress",
                  })}
                />
                <SelectItem
                  value="AWAITING_REVIEW"
                  text={intl.formatMessage({
                    id: "pathology.reception.awaitingReview",
                  })}
                />
                <SelectItem
                  value="ADDITIONAL_REQUEST"
                  text={intl.formatMessage({ id: "pathology.label.requests" })}
                />
                <SelectItem
                  value="COMPLETED"
                  text={intl.formatMessage({ id: "pathology.label.complete" })}
                />
                <SelectItem value="ALL" text="All" />
              </Select>
            </div>
          </Column>

          <Column lg={16} md={8} sm={4}>
            <DataTable
              rows={entries.slice((page - 1) * pageSize, page * pageSize)}
              headers={[
                {
                  key: "requestDate",
                  header: <FormattedMessage id="sample.requestDate" />,
                },
                {
                  key: "serviceCategory",
                  header: (
                    <FormattedMessage id="pathology.label.serviceCategory" />
                  ),
                },
                {
                  key: "subtype",
                  header: <FormattedMessage id="label.subtype" />,
                },
                {
                  key: "status",
                  header: <FormattedMessage id="pathology.label.stage" />,
                },
                {
                  key: "lastName",
                  header: <FormattedMessage id="patient.last.name" />,
                },
                {
                  key: "firstName",
                  header: <FormattedMessage id="patient.first.name" />,
                },
                {
                  key: "requester",
                  header: (
                    <FormattedMessage id="pathology.label.requestingPhysician" />
                  ),
                },
                {
                  key: "assignedTechnician",
                  header: <FormattedMessage id="assigned.technician.label" />,
                },
                {
                  key: "assignedSpecialist",
                  header: <FormattedMessage id="assigned.pathologist.label" />,
                },
                {
                  key: "labNumber",
                  header: <FormattedMessage id="sample.label.labnumber" />,
                },
              ]}
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
                      <>
                        {rows.map((row) => (
                          <TableRow
                            key={row.id}
                            className={rowClassName(row)}
                            onClick={() => {
                              openCaseView(row);
                            }}
                          >
                            {row.cells.map((cell) => renderCell(cell, row))}
                          </TableRow>
                        ))}
                      </>
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </DataTable>
            <Pagination
              onChange={handlePageChange}
              page={page}
              pageSize={pageSize}
              pageSizes={[10, 20, 30, 50, 100]}
              totalItems={entries.length}
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
              pageNumberText={intl.formatMessage({
                id: "pagination.page-number",
              })}
              pageRangeText={(_current, total) =>
                intl.formatMessage(
                  { id: "pagination.page-range" },
                  { total: total },
                )
              }
              pageText={(page, pagesUnknown) =>
                intl.formatMessage(
                  { id: "pagination.page" },
                  { page: pagesUnknown ? "" : page },
                )
              }
            />
          </Column>
        </Grid>
      </div>
    </>
  );
}

export default PathologyReceptionDashboard;
