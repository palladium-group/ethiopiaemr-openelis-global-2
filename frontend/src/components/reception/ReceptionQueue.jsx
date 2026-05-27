import React, { useEffect, useState } from "react";
import {
  Button,
  Column,
  DataTable,
  Grid,
  Loading,
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
import { FormattedMessage, useIntl } from "react-intl";
import { useLocation } from "react-router-dom";
import CustomDatePicker from "../common/CustomDatePicker";
import {
  convertAlphaNumLabNumForDisplay,
  getFromOpenElisServer,
} from "../utils/Utils";
import ReceptionActionModal from "./ReceptionActionModal";

const ReceptionQueue = () => {
  const intl = useIntl();
  const location = useLocation();
  const [accessionNumber, setAccessionNumber] = useState("");
  const [receivedDateFrom, setReceivedDateFrom] = useState("");
  const [receivedDateTo, setReceivedDateTo] = useState("");
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(25);
  const [totalItems, setTotalItems] = useState(0);
  const [loading, setLoading] = useState(false);
  const [selectedAccession, setSelectedAccession] = useState("");
  const [modalOpen, setModalOpen] = useState(false);

  const headers = [
    {
      key: "accessionNumber",
      header: intl.formatMessage({ id: "reception.table.accession" }),
    },
    {
      key: "patientName",
      header: intl.formatMessage({ id: "patient.label.name" }),
    },
    {
      key: "patientNationalId",
      header: intl.formatMessage({ id: "patient.id" }),
    },
    {
      key: "referringId",
      header: intl.formatMessage({ id: "reception.table.orderId" }),
    },
    {
      key: "priority",
      header: intl.formatMessage({ id: "eorder.priority" }),
    },
    {
      key: "receivedDate",
      header: intl.formatMessage({ id: "reception.table.receivedDate" }),
    },
    {
      key: "pendingTestCount",
      header: intl.formatMessage({ id: "reception.table.pendingTests" }),
    },
    {
      key: "actions",
      header: intl.formatMessage({ id: "label.actions" }),
    },
  ];

  const loadQueue = (pageNumber = page, size = pageSize) => {
    setLoading(true);
    const params = new URLSearchParams({
      page: String(pageNumber),
      pageSize: String(size),
    });
    if (accessionNumber.trim()) {
      params.set("accessionNumber", accessionNumber.trim());
    }
    if (receivedDateFrom) {
      params.set("receivedDateFrom", receivedDateFrom);
    }
    if (receivedDateTo) {
      params.set("receivedDateTo", receivedDateTo);
    }
    getFromOpenElisServer(
      "/rest/reception/queue?" + params.toString(),
      (response) => {
        setItems(response?.items || []);
        setTotalItems(response?.totalItems || 0);
        setPage(response?.page || pageNumber);
        setLoading(false);
      },
    );
  };

  useEffect(() => {
    loadQueue(1);
  }, []);

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const accessionFromUrl = params.get("accessionNumber");
    if (accessionFromUrl) {
      setAccessionNumber(accessionFromUrl);
      setSelectedAccession(accessionFromUrl);
      setModalOpen(true);
    }
  }, [location.search]);

  const openModal = (accession) => {
    setSelectedAccession(accession);
    setModalOpen(true);
  };

  const rows = items.map((item) => ({
    id: item.accessionNumber,
    accessionNumber: convertAlphaNumLabNumForDisplay(item.accessionNumber),
    patientName: item.patientName,
    patientNationalId: item.patientNationalId,
    referringId: item.referringId,
    priority: item.priority,
    receivedDate: item.receivedDate,
    pendingTestCount: item.pendingTestCount,
    actions: item.accessionNumber,
  }));

  return (
    <>
      <Grid fullWidth={true}>
        <Column lg={4} md={4} sm={4}>
          <TextInput
            id="reception-accession-filter"
            labelText={intl.formatMessage({ id: "reception.search.accession" })}
            value={accessionNumber}
            onChange={(event) => setAccessionNumber(event.target.value)}
          />
        </Column>
        <Column lg={3} md={4} sm={4}>
          <CustomDatePicker
            id="reception-received-from"
            labelText={intl.formatMessage({
              id: "reception.search.receivedFrom",
            })}
            value={receivedDateFrom}
            onChange={setReceivedDateFrom}
          />
        </Column>
        <Column lg={3} md={4} sm={4}>
          <CustomDatePicker
            id="reception-received-to"
            labelText={intl.formatMessage({
              id: "reception.search.receivedTo",
            })}
            value={receivedDateTo}
            onChange={setReceivedDateTo}
          />
        </Column>
        <Column lg={2} md={4} sm={4} style={{ marginTop: "1.5rem" }}>
          <Button onClick={() => loadQueue(1)}>
            <FormattedMessage id="label.button.search" />
          </Button>
        </Column>
      </Grid>

      {loading ? (
        <Loading withOverlay={false} small />
      ) : (
        <DataTable rows={rows} headers={headers} isSortable>
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
                        <FormattedMessage id="reception.search.noResults" />
                      </TableCell>
                    </TableRow>
                  ) : (
                    rows.map((row) => (
                      <TableRow key={row.id} {...getRowProps({ row })}>
                        {row.cells.map((cell) => {
                          if (cell.info.header === "actions") {
                            return (
                              <TableCell key={cell.id}>
                                <Button
                                  kind="ghost"
                                  size="sm"
                                  onClick={() => openModal(cell.value)}
                                >
                                  <FormattedMessage id="reception.button.review" />
                                </Button>
                              </TableCell>
                            );
                          }
                          return (
                            <TableCell key={cell.id}>{cell.value}</TableCell>
                          );
                        })}
                      </TableRow>
                    ))
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
        pageSizes={[10, 25, 50]}
        totalItems={totalItems}
        onChange={({ page: nextPage, pageSize: nextPageSize }) => {
          setPageSize(nextPageSize);
          setPage(nextPage);
          loadQueue(nextPage, nextPageSize);
        }}
      />

      <ReceptionActionModal
        open={modalOpen}
        accessionNumber={selectedAccession}
        onClose={() => setModalOpen(false)}
        onComplete={() => loadQueue(page, pageSize)}
      />
    </>
  );
};

export default ReceptionQueue;
