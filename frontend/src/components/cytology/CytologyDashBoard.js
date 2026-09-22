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
import "../pathology/PathologyDashboard.css";
import PageBreadCrumb from "../common/PageBreadCrumb";

function CytologyDashboard() {
  const componentMounted = useRef(false);

  const { notificationVisible } = useContext(NotificationContext);
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const [statuses, setStatuses] = useState([]);
  const [pathologyEntries, setPathologyEntries] = useState([]);
  const [cytopathologists, setCytopathologists] = useState([]);
  const [filters, setFilters] = useState({
    searchTerm: "",
    myCases: false,
    unassignedOnly: true,
    statuses: [],
  });
  const [inProgressStatuses, setInProgressStatuses] = useState([]);

  const [counts, setCounts] = useState({
    unassigned: 0,
    inProgress: 0,
    awaitingReview: 0,
    complete: 0,
  });
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(100);
  const intl = useIntl();
  const [inProgressStatusObjects, setInProgressStatusObjects] = useState([]);

  const setStatusList = (statusList) => {
    if (componentMounted.current) {
      setStatuses(statusList);

      const filteredStatuses = statusList
        .filter(
          (status) =>
            status.id !== "COMPLETED" && status.id !== "REJECTED",
        )
        .map((status) => status.id);

      setInProgressStatuses(filteredStatuses);
      setInProgressStatusObjects(
        filteredStatuses.map((statusId) => ({ id: statusId })),
      );
    }
  };

  const assignCytoPathologist = (event, cytologySampleId, pathologistId) => {
    event.stopPropagation();
    if (!pathologistId) {
      return;
    }
    postToOpenElisServerFullResponse(
      "/rest/cytology/assignCytoPathologist?cytologySampleId=" +
        cytologySampleId +
        "&pathologistId=" +
        encodeURIComponent(pathologistId),
      {},
      () => {
        refreshItems();
        refreshCounts();
        getFromOpenElisServer(
          "/rest/cytology/cytopathologists",
          setCytopathologistsList,
        );
      },
    );
  };

  const assignCurrentUserAsPathologist = (event, cytologySampleId) => {
    event.stopPropagation();
    postToOpenElisServerFullResponse(
      "/rest/cytology/assignCytoPathologist?cytologySampleId=" +
        cytologySampleId,
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
    var cytologySampleId = row.id;
    var assignedCytoPathologist = row.cells.find(
      (e) => e.info.header === "assignedCytoPathologist",
    )?.value;

    if (cell.info.header === "assignedCytoPathologist" && !assignedCytoPathologist) {
      return (
        <TableCell key={cell.id} onClick={(e) => e.stopPropagation()}>
          <Select
            id={"assign-cytopathologist-" + cytologySampleId}
            labelText=""
            hideLabel
            size="sm"
            defaultValue=""
            onClick={(e) => e.stopPropagation()}
            onChange={(e) =>
              assignCytoPathologist(e, cytologySampleId, e.target.value)
            }
          >
            <SelectItem
              value=""
              text={intl.formatMessage({
                id: "cytology.label.assignCytopathologist",
              })}
            />
            {cytopathologists.map((p) => (
              <SelectItem
                key={p.id}
                value={p.id}
                text={`${p.value} (${p.openCaseload ?? 0})`}
              />
            ))}
          </Select>
          {status === "READY_FOR_CYTOPATHOLOGIST" &&
            hasRole(userSessionDetails, "Cytopathologist") && (
              <Button
                type="button"
                size="sm"
                kind="ghost"
                className="assign-self-button"
                onClick={(e) =>
                  assignCurrentUserAsPathologist(e, cytologySampleId)
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

  const formatCytologySubtype = (subtype) => {
    if (!subtype) {
      return "";
    }
    const labels = {
      FNAC: "FNAC",
      IMAGE_GUIDED_FNAC: "Image-guided FNAC",
      PAP_SMEAR: "Pap smear",
      FLUID: "Fluid cytology",
    };
    return labels[subtype] || subtype;
  };

  const setPathologyEntriesWithIds = (entries) => {
    if (componentMounted.current) {
      if (entries && entries.length > 0) {
        setPathologyEntries(
          entries.map((entry) => {
            return {
              ...entry,
              id: "" + entry.pathologySampleId,
              subtype: formatCytologySubtype(entry.subtype),
            };
          }),
        );
      } else {
        setPathologyEntries([]);
      }
      setLoading(false);
    }
  };

  const setCytopathologistsList = (list) => {
    if (componentMounted.current && Array.isArray(list)) {
      setCytopathologists(list);
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
    if (
      filters.statuses.length === inProgressStatuses.length &&
      filters.statuses.every((status) => inProgressStatuses.includes(status.id))
    ) {
      return "IN_PROGRESS";
    }
    if (filters.statuses.length > 1) {
      return "All";
    }
    return filters.statuses[0]?.id || "UNASSIGNED";
  };

  const filtersToParameters = () => {
    if (filters.unassignedOnly) {
      return (
        "unassigned=true&searchTerm=" +
        encodeURIComponent(filters.searchTerm || "")
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
      "/rest/cytology/dashboard?" + filtersToParameters(),
      setPathologyEntriesWithIds,
    );
  };

  const refreshCounts = () => {
    getFromOpenElisServer("/rest/cytology/dashboard/count", loadCounts);
  };

  const openCaseView = (id) => {
    window.location.href = "/CytologyCaseView/" + id;
  };

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/displayList/CYTOLOGY_STATUS", setStatusList);
    getFromOpenElisServer(
      "/rest/cytology/cytopathologists",
      setCytopathologistsList,
    );
    refreshCounts();

    return () => {
      componentMounted.current = false;
    };
  }, []);

  const loadCounts = (data) => {
    if (componentMounted.current && data) {
      setCounts({
        unassigned: data?.unassigned ?? 0,
        inProgress: data?.inProgress ?? 0,
        awaitingReview: data?.awaitingReview ?? 0,
        complete: data?.complete ?? 0,
      });
    }
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
      title: intl.formatMessage({ id: "pathology.label.unassigned" }),
      count: counts.unassigned,
      className: "dashboard-tile unassigned-tile",
    },
    {
      key: "inProgress",
      title: intl.formatMessage({ id: "pathology.label.casesInProgress" }),
      count: counts.inProgress,
      className: "dashboard-tile",
    },
    {
      key: "awaitingReview",
      title: intl.formatMessage({ id: "cytology.label.review" }),
      count: counts.awaitingReview,
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

  const selectTile = (tileKey) => {
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
        statuses: [{ id: "READY_FOR_CYTOPATHOLOGIST" }],
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
    refreshItems();
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
                <FormattedMessage id="cytology.label.title" />
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>
      <div className="dashboard-container">
        {tileList.map((tile) => (
          <Tile
            key={tile.key}
            className={tile.className}
            onClick={() => selectTile(tile.key)}
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
              <div>
                <FormattedMessage id="filters.label" />
              </div>
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
                  text={intl.formatMessage({ id: "pathology.label.unassigned" })}
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
                  header: intl.formatMessage({ id: "sample.requestDate" }),
                },
                {
                  key: "status",
                  header: intl.formatMessage({ id: "label.filters.status" }),
                },
                {
                  key: "subtype",
                  header: intl.formatMessage({ id: "label.subtype" }),
                },
                {
                  key: "lastName",
                  header: intl.formatMessage({ id: "patient.last.name" }),
                },
                {
                  key: "firstName",
                  header: intl.formatMessage({ id: "patient.first.name" }),
                },
                {
                  key: "assignedTechnician",
                  header: intl.formatMessage({ id: "assigned.technician.label" }),
                },
                {
                  key: "assignedCytoPathologist",
                  header: intl.formatMessage({
                    id: "assigned.cytopathologist.label",
                  }),
                },
                {
                  key: "labNumber",
                  header: intl.formatMessage({ id: "sample.label.labnumber" }),
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

export default CytologyDashboard;
