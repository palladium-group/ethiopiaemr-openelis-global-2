import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Checkbox,
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
} from "@carbon/react";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { Search } from "@carbon/react";
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

function PathologyDashboard() {
  const componentMounted = useRef(false);

  const intl = useIntl();

  const { notificationVisible } = useContext(NotificationContext);
  const { userSessionDetails } = useContext(UserSessionDetailsContext);

  const [statuses, setStatuses] = useState([]);
  const [pathologyEntries, setPathologyEntries] = useState([]);
  const [pathologists, setPathologists] = useState([]);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(100);
  const [filters, setFilters] = useState({
    searchTerm: "",
    myCases: false,
    // Default Reception queue: no pathologist assigned (not merely RECEIVED status)
    unassignedOnly: true,
    statuses: [],
  });

  const [inProgressStatuses, setInProgressStatuses] = useState([]);

  const [counts, setCounts] = useState({
    unassigned: 0,
    inProgress: 0,
    awaitingReview: 0,
    additionalRequests: 0,
    complete: 0,
  });
  const [loading, setLoading] = useState(true);
  const [inProgressStatusObjects, setInProgressStatusObjects] = useState([]);
  const setStatusList = (statusList) => {
    if (componentMounted.current) {
      setStatuses(statusList);

      const filteredStatuses = statusList
        .filter(
          (status) =>
            status.id !== "COMPLETED" && status.id !== "RECEIVED",
        )
        .map((status) => status.id);

      setInProgressStatuses(filteredStatuses);
      setInProgressStatusObjects(
        filteredStatuses.map((statusId) => ({ id: statusId })),
      );
    }
  };

  const assignPathologist = (event, pathologySampleId, pathologistId) => {
    event.stopPropagation();
    if (!pathologistId) {
      return;
    }
    postToOpenElisServerFullResponse(
      "/rest/pathology/assignPathologist?pathologySampleId=" +
        pathologySampleId +
        "&pathologistId=" +
        encodeURIComponent(pathologistId),
      {},
      () => {
        refreshItems();
        refreshCounts();
        getFromOpenElisServer(
          "/rest/pathology/pathologists",
          setPathologistsList,
        );
      },
    );
  };

  const assignCurrentUserAsPathologist = (event, pathologySampleId) => {
    event.stopPropagation();
    postToOpenElisServerFullResponse(
      "/rest/pathology/assignPathologist?pathologySampleId=" +
        pathologySampleId,
      {},
      () => {
        refreshItems();
        refreshCounts();
      },
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
    var status = row.cells.find((e) => e.info.header === "status").value;
    var pathologySampleId = row.id;
    var assignedPathologist = row.cells.find(
      (e) => e.info.header === "assignedPathologist",
    )?.value;

    // Reception Step 1: inline pathologist dropdown with caseload for unassigned cases
    if (cell.info.header === "assignedPathologist" && !assignedPathologist) {
      return (
        <TableCell key={cell.id} onClick={(e) => e.stopPropagation()}>
          <Select
            id={"assign-pathologist-" + pathologySampleId}
            labelText=""
            hideLabel
            size="sm"
            defaultValue=""
            onClick={(e) => e.stopPropagation()}
            onChange={(e) =>
              assignPathologist(e, pathologySampleId, e.target.value)
            }
          >
            <SelectItem
              value=""
              text={intl.formatMessage({
                id: "pathology.label.assignPathologist",
              })}
            />
            {pathologists.map((p) => (
              <SelectItem
                key={p.id}
                value={p.id}
                text={`${p.value} (${p.openCaseload ?? 0})`}
              />
            ))}
          </Select>
          {status === "READY_PATHOLOGIST" &&
            hasRole(userSessionDetails, "Pathologist") && (
              <Button
                type="button"
                size="sm"
                kind="ghost"
                className="assign-self-button"
                onClick={(e) =>
                  assignCurrentUserAsPathologist(e, pathologySampleId)
                }
              >
                <FormattedMessage id="label.button.start" />
              </Button>
            )}
        </TableCell>
      );
    }

    return <TableCell key={cell.id}>{cell.value}</TableCell>;
  };

  const setPathologyEntriesWithIds = (entries) => {
    if (componentMounted.current) {
      if (entries && entries.length > 0) {
        setPathologyEntries(
          entries.map((entry) => {
            return { ...entry, id: "" + entry.pathologySampleId };
          }),
        );
      } else {
        setPathologyEntries([]);
      }
      setLoading(false);
    }
  };

  const setPathologistsList = (list) => {
    if (componentMounted.current && Array.isArray(list)) {
      setPathologists(list);
    }
  };

  const setStatusFilter = (event) => {
    const { value } = event.target;

    if (value === "UNASSIGNED") {
      setFilters({ ...filters, unassignedOnly: true, statuses: [] });
    } else if (value === "All") {
      setFilters({
        ...filters,
        unassignedOnly: false,
        statuses: statuses.map((s) => ({ id: s.id })),
      });
    } else if (value === "IN_PROGRESS") {
      setFilters({
        ...filters,
        unassignedOnly: false,
        statuses: inProgressStatusObjects,
      });
    } else {
      setFilters({
        ...filters,
        unassignedOnly: false,
        statuses: [{ id: value }],
      });
    }
  };

  const getSelectedValue = () => {
    if (filters.unassignedOnly) {
      return "UNASSIGNED";
    }
    const selectedValue =
      filters.statuses.length === inProgressStatuses.length &&
      filters.statuses.every((status) => inProgressStatuses.includes(status.id))
        ? "IN_PROGRESS"
        : filters.statuses.length > 1
          ? "All"
          : filters.statuses[0]?.id;

    return selectedValue;
  };

  const filtersToParameters = () => {
    if (filters.unassignedOnly) {
      return (
        "unassigned=true&searchTerm=" + encodeURIComponent(filters.searchTerm || "")
      );
    }
    return (
      "statuses=" +
      filters.statuses
        .map((entry) => {
          return entry.id;
        })
        .join(",") +
      "&searchTerm=" +
      encodeURIComponent(filters.searchTerm || "")
    );
  };

  const refreshItems = () => {
    getFromOpenElisServer(
      "/rest/pathology/dashboard?" + filtersToParameters(),
      setPathologyEntriesWithIds,
    );
  };

  const refreshCounts = () => {
    getFromOpenElisServer("/rest/pathology/dashboard/count", loadCounts);
  };

  const openCaseView = (id) => {
    window.location.href = "/PathologyCaseView/" + id;
  };

  const filterByTile = (tileKey) => {
    if (tileKey === "unassigned") {
      setFilters({ ...filters, unassignedOnly: true, statuses: [] });
    } else if (tileKey === "inProgress") {
      setFilters({
        ...filters,
        unassignedOnly: false,
        statuses: inProgressStatusObjects,
      });
    } else if (tileKey === "awaitingReview") {
      setFilters({
        ...filters,
        unassignedOnly: false,
        statuses: [{ id: "READY_PATHOLOGIST" }],
      });
    } else if (tileKey === "additionalRequests") {
      setFilters({
        ...filters,
        unassignedOnly: false,
        statuses: [{ id: "ADDITIONAL_REQUEST" }],
      });
    } else if (tileKey === "complete") {
      setFilters({
        ...filters,
        unassignedOnly: false,
        statuses: [{ id: "COMPLETED" }],
      });
    }
  };

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/displayList/PATHOLOGY_STATUS", setStatusList);
    getFromOpenElisServer("/rest/pathology/dashboard/count", loadCounts);
    getFromOpenElisServer("/rest/pathology/pathologists", setPathologistsList);

    return () => {
      componentMounted.current = false;
    };
  }, []);

  const loadCounts = (data) => {
    setCounts({
      unassigned: data?.unassigned ?? 0,
      inProgress: data?.inProgress ?? 0,
      awaitingReview: data?.awaitingReview ?? 0,
      additionalRequests: data?.additionalRequests ?? 0,
      complete: data?.complete ?? 0,
    });
  };

  function formatDateToDDMMYYYY(date) {
    var day = date.getDate();
    var month = date.getMonth() + 1;
    var year = date.getFullYear();

    var formattedDay = (day < 10 ? "0" : "") + day;
    var formattedMonth = (month < 10 ? "0" : "") + month;

    var formattedDate = formattedDay + "/" + formattedMonth + "/" + year;
    return formattedDate;
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
      title: <FormattedMessage id="pathology.label.unassigned" />,
      count: counts.unassigned,
      className: "dashboard-tile unassigned-tile",
    },
    {
      key: "inProgress",
      title: <FormattedMessage id="pathology.label.casesInProgress" />,
      count: counts.inProgress,
      className: "dashboard-tile",
    },
    {
      key: "awaitingReview",
      title: <FormattedMessage id="pathology.label.review" />,
      count: counts.awaitingReview,
      className: "dashboard-tile",
    },
    {
      key: "additionalRequests",
      title: <FormattedMessage id="pathology.label.requests" />,
      count: counts.additionalRequests,
      className: "dashboard-tile",
    },
    {
      key: "complete",
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
    refreshItems();
    return () => {
      componentMounted.current = false;
    };
  }, [filters]);

  let breadcrumbs = [{ label: "home.label", link: "/" }];

  const rowClassName = (row) => {
    const status = row.cells.find((e) => e.info.header === "status")?.value;
    return status === "RECEIVED" ? "received-row" : undefined;
  };

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
      <div className="dashboard-container dashboard-container-5">
        {tileList.map((tile) => (
          <Tile
            key={tile.key}
            className={tile.className}
            onClick={() => filterByTile(tile.key)}
            style={{ cursor: "pointer" }}
          >
            <h3 className="tile-title">{tile.title}</h3>
            <p className="tile-value">{tile.count}</p>
          </Tile>
        ))}
      </div>
      <div className="orderLegendBody">
        <Grid fullWidth={true} className="gridBoundary">
          <Column lg={8} md={4} sm={2}>
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
          <Column lg={8} md={4} sm={2}>
            <div className="inlineDivBlock">
              <div>Filters:</div>
              <Checkbox
                labelText={intl.formatMessage({ id: "label.filters.mycases" })}
                id="filterMyCases"
                value={filters.myCases}
                onChange={(e) =>
                  setFilters({ ...filters, myCases: e.target.checked })
                }
              />
              <Select
                id="statusFilter"
                name="statusFilter"
                labelText={intl.formatMessage({ id: "label.filters.status" })}
                value={getSelectedValue()}
                onChange={setStatusFilter}
                noLabel
              >
                <SelectItem disabled value="placeholder" text="Status" />
                <SelectItem
                  text={intl.formatMessage({
                    id: "pathology.label.unassigned",
                  })}
                  value="UNASSIGNED"
                />
                <SelectItem text="All" value="All" />
                <SelectItem text="In Progress" value="IN_PROGRESS" />
                {statuses.map((status, index) => (
                  <SelectItem
                    key={index}
                    text={status.value}
                    value={status.id}
                  />
                ))}
              </Select>
            </div>
          </Column>

          <Column lg={16} md={8} sm={4}>
            <DataTable
              rows={pathologyEntries.slice(
                (page - 1) * pageSize,
                page * pageSize,
              )}
              headers={[
                {
                  key: "requestDate",
                  header: <FormattedMessage id="sample.requestDate" />,
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
                  key: "assignedPathologist",
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
                              openCaseView(row.id);
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
              totalItems={pathologyEntries.length}
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

export default PathologyDashboard;
