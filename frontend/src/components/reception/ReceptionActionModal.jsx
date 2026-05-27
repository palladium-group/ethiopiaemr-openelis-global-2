import React, { useContext, useEffect, useState } from "react";
import {
  Button,
  ComposedModal,
  InlineNotification,
  ModalBody,
  ModalFooter,
  ModalHeader,
  Select,
  SelectItem,
  StructuredListBody,
  StructuredListCell,
  StructuredListHead,
  StructuredListRow,
  StructuredListWrapper,
  TextArea,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../layout/Layout";
import { NotificationKinds } from "../common/CustomNotification";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";

const ReceptionActionModal = ({
  open,
  accessionNumber,
  onClose,
  onComplete,
}) => {
  const intl = useIntl();
  const { addNotification } = useContext(NotificationContext);
  const [detail, setDetail] = useState(null);
  const [notes, setNotes] = useState("");
  const [rejectReasonId, setRejectReasonId] = useState("");
  const [rejectReasons, setRejectReasons] = useState([]);
  const [loading, setLoading] = useState(false);
  const [actionInProgress, setActionInProgress] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    if (open && accessionNumber) {
      setLoading(true);
      setErrorMessage("");
      setNotes("");
      setRejectReasonId("");
      getFromOpenElisServer(
        "/rest/reception/" + encodeURIComponent(accessionNumber),
        (response) => {
          if (!response || !response.accessionNumber) {
            setErrorMessage(
              intl.formatMessage({ id: "reception.detail.notFound" }),
            );
            setDetail(null);
          } else {
            setDetail(response);
          }
          setLoading(false);
        },
      );
    }
  }, [open, accessionNumber, intl]);

  useEffect(() => {
    if (open) {
      getFromOpenElisServer("/rest/test-rejection-reasons", (response) => {
        setRejectReasons(response || []);
      });
    }
  }, [open]);

  const handleClose = () => {
    setDetail(null);
    setErrorMessage("");
    onClose();
  };

  const notifySuccess = (message) => {
    addNotification({
      kind: NotificationKinds.success,
      title: intl.formatMessage({ id: "notification.title" }),
      message,
    });
    handleClose();
    if (onComplete) {
      onComplete();
    }
  };

  const handleApprove = () => {
    setActionInProgress(true);
    postToOpenElisServerJsonResponse(
      "/rest/reception/approve",
      JSON.stringify({ accessionNumber, notes }),
      (response) => {
        setActionInProgress(false);
        if (response?.statusCode && response.statusCode >= 400) {
          setErrorMessage(response.message || response.error);
          return;
        }
        notifySuccess(
          response?.message ||
            intl.formatMessage({ id: "reception.approve.success" }),
        );
      },
    );
  };

  const handleReject = () => {
    if (!rejectReasonId) {
      setErrorMessage(
        intl.formatMessage({ id: "reception.reject.reasonRequired" }),
      );
      return;
    }
    setActionInProgress(true);
    postToOpenElisServerJsonResponse(
      "/rest/reception/reject",
      JSON.stringify({ accessionNumber, rejectReasonId, notes }),
      (response) => {
        setActionInProgress(false);
        if (response?.statusCode && response.statusCode >= 400) {
          setErrorMessage(response.message || response.error);
          return;
        }
        let message =
          response?.message ||
          intl.formatMessage({ id: "reception.reject.success" });
        if (response?.recollectionOrderCreated) {
          message +=
            " " +
            intl.formatMessage(
              { id: "reception.reject.recollectionCreated" },
              { externalId: response.newElectronicOrderExternalId },
            );
        }
        notifySuccess(message);
      },
    );
  };

  return (
    <ComposedModal open={open} onClose={handleClose} size="lg">
      <ModalHeader
        title={intl.formatMessage({ id: "reception.modal.title" })}
        label={accessionNumber}
      />
      <ModalBody>
        {errorMessage && (
          <InlineNotification
            kind="error"
            title={intl.formatMessage({ id: "reception.error.title" })}
            subtitle={errorMessage}
            lowContrast
            hideCloseButton
          />
        )}
        {loading && <p>{intl.formatMessage({ id: "loading.description" })}</p>}
        {!loading && detail && (
          <>
            <StructuredListWrapper>
              <StructuredListHead>
                <StructuredListRow head>
                  <StructuredListCell head>
                    <FormattedMessage id="reception.detail.field" />
                  </StructuredListCell>
                  <StructuredListCell head>
                    <FormattedMessage id="reception.detail.value" />
                  </StructuredListCell>
                </StructuredListRow>
              </StructuredListHead>
              <StructuredListBody>
                <StructuredListRow>
                  <StructuredListCell>
                    <FormattedMessage id="patient.label.name" />
                  </StructuredListCell>
                  <StructuredListCell>{detail.patientName}</StructuredListCell>
                </StructuredListRow>
                <StructuredListRow>
                  <StructuredListCell>
                    <FormattedMessage id="patient.id" />
                  </StructuredListCell>
                  <StructuredListCell>
                    {detail.patientNationalId}
                  </StructuredListCell>
                </StructuredListRow>
                <StructuredListRow>
                  <StructuredListCell>
                    <FormattedMessage id="reception.detail.collectionDate" />
                  </StructuredListCell>
                  <StructuredListCell>
                    {detail.collectionDate}
                  </StructuredListCell>
                </StructuredListRow>
                <StructuredListRow>
                  <StructuredListCell>
                    <FormattedMessage id="reception.detail.receivedDate" />
                  </StructuredListCell>
                  <StructuredListCell>
                    {detail.receivedDate} {detail.receivedTime}
                  </StructuredListCell>
                </StructuredListRow>
                <StructuredListRow>
                  <StructuredListCell>
                    <FormattedMessage id="reception.table.pendingTests" />
                  </StructuredListCell>
                  <StructuredListCell>
                    {detail.pendingTestCount}
                  </StructuredListCell>
                </StructuredListRow>
              </StructuredListBody>
            </StructuredListWrapper>
            {detail.tests?.length > 0 && (
              <StructuredListWrapper style={{ marginTop: "1rem" }}>
                <StructuredListHead>
                  <StructuredListRow head>
                    <StructuredListCell head>
                      <FormattedMessage id="test.name" />
                    </StructuredListCell>
                    <StructuredListCell head>
                      <FormattedMessage id="sample.type" />
                    </StructuredListCell>
                  </StructuredListRow>
                </StructuredListHead>
                <StructuredListBody>
                  {detail.tests.map((test) => (
                    <StructuredListRow key={test.analysisId}>
                      <StructuredListCell>{test.testName}</StructuredListCell>
                      <StructuredListCell>{test.sampleType}</StructuredListCell>
                    </StructuredListRow>
                  ))}
                </StructuredListBody>
              </StructuredListWrapper>
            )}
            <TextArea
              id="reception-notes"
              labelText={intl.formatMessage({ id: "column.name.notes" })}
              value={notes}
              onChange={(event) => setNotes(event.target.value)}
              style={{ marginTop: "1rem" }}
            />
            <Select
              id="reception-reject-reason"
              labelText={intl.formatMessage({ id: "reception.reject.reason" })}
              value={rejectReasonId}
              onChange={(event) => setRejectReasonId(event.target.value)}
              style={{ marginTop: "1rem" }}
            >
              <SelectItem
                value=""
                text={intl.formatMessage({ id: "select.default.option.label" })}
              />
              {rejectReasons.map((reason) => (
                <SelectItem
                  key={reason.id}
                  value={reason.id}
                  text={reason.value}
                />
              ))}
            </Select>
          </>
        )}
      </ModalBody>
      <ModalFooter>
        <Button
          kind="secondary"
          onClick={handleClose}
          disabled={actionInProgress}
        >
          <FormattedMessage id="label.button.cancel" />
        </Button>
        <Button
          kind="danger"
          onClick={handleReject}
          disabled={actionInProgress || loading || !detail}
        >
          <FormattedMessage id="reception.button.reject" />
        </Button>
        <Button
          onClick={handleApprove}
          disabled={actionInProgress || loading || !detail}
        >
          <FormattedMessage id="reception.button.approve" />
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};

export default ReceptionActionModal;
