import React, {
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
} from "react";
import {
  Button,
  ComboBox,
  InlineNotification,
  Modal,
  NumberInput,
  Select,
  SelectItem,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
  TextInput,
} from "@carbon/react";
import { Renew, TrashCan } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";
import { ConfigurationContext } from "../layout/Layout";

const URGENCIES = ["ROUTINE", "STAT"];
const MAX_INSTRUCTIONS = 500;
const MAX_QUANTITY = 100000;
const SEARCH_DELAY_MS = 300;

const ERROR_MESSAGES = {
  NOT_ENABLED: "supplyOrder.error.notEnabled",
  NOT_CONFIGURED: "supplyOrder.error.notConfigured",
  NO_OPENMRS_ORDER: "supplyOrder.error.noOpenMrsOrder",
  ORDER_NOT_FOUND: "supplyOrder.error.orderNotFound",
  NO_ACTIVE_VISIT: "supplyOrder.error.noActiveVisit",
  NO_LOCATION: "supplyOrder.error.noLocation",
  INVALID_ITEMS: "supplyOrder.error.invalidItems",
  CASE_CLOSED: "supplyOrder.error.caseClosed",
  NOT_FOUND: "supplyOrder.error.notFound",
  OPENMRS_UNAVAILABLE: "supplyOrder.error.openMrsUnavailable",
};

const isErrorResponse = (json) => !json || json.statusCode || json.error;

export const lineIsValid = (line) =>
  Number.isInteger(line.quantity) &&
  line.quantity >= 1 &&
  URGENCIES.includes(line.urgency) &&
  (line.instructions || "").length <= MAX_INSTRUCTIONS;

/**
 * Medical supplies ordered for a pathology/cytology case. Each submission is
 * created in OpenMRS as an order encounter, which OpenMRS sends to Dagu just
 * like an order placed in the EMR.
 */
function CaseSupplyOrders({ caseType, caseId, caseClosed }) {
  const intl = useIntl();
  const { configurationProperties } = useContext(ConfigurationContext);
  const enabled =
    configurationProperties?.PATHOLOGY_SUPPLY_ORDER_ENABLED === "true";
  const basePath = "/rest/" + caseType + "/caseView/" + caseId;

  const [requests, setRequests] = useState([]);
  const [fromOpenMrs, setFromOpenMrs] = useState(false);
  const [loading, setLoading] = useState(false);
  const [notice, setNotice] = useState(null);
  const [retryingId, setRetryingId] = useState(null);
  const [modalOpen, setModalOpen] = useState(false);

  const errorText = useCallback(
    (json) =>
      intl.formatMessage({
        id: ERROR_MESSAGES[json?.code] || "supplyOrder.error.generic",
      }),
    [intl],
  );

  const load = useCallback(() => {
    if (!enabled || !caseId) {
      return;
    }
    setLoading(true);
    getFromOpenElisServer(basePath + "/supplyRequests", (json) => {
      setLoading(false);
      if (json && Array.isArray(json.requests)) {
        setRequests(json.requests);
        setFromOpenMrs(!!json.fromOpenMrs);
      }
    });
  }, [enabled, caseId, basePath]);

  useEffect(() => {
    load();
  }, [load]);

  const replaceRequest = (updated) =>
    setRequests((prev) => prev.map((r) => (r.id === updated.id ? updated : r)));

  const retry = (request) => {
    setRetryingId(request.id);
    setNotice(null);
    postToOpenElisServerJsonResponse(
      basePath + "/supplyRequests/" + request.id + "/retry",
      "{}",
      (json) => {
        setRetryingId(null);
        if (isErrorResponse(json)) {
          setNotice({ kind: "error", text: errorText(json) });
        } else {
          replaceRequest(json);
        }
      },
    );
  };

  const onSubmitted = (created) => {
    setModalOpen(false);
    setRequests((prev) => [created, ...prev]);
    setNotice(
      created.status === "SENT"
        ? {
            kind: "success",
            text: intl.formatMessage({ id: "supplyOrder.notice.sent" }),
          }
        : {
            kind: "error",
            text: intl.formatMessage({ id: "supplyOrder.notice.notSent" }),
          },
    );
  };

  if (!enabled) {
    return null;
  }

  return (
    <section
      className="case-supply-orders"
      aria-labelledby="supply-orders-title"
    >
      <div className="case-supply-orders-header">
        <h3 id="supply-orders-title" className="case-supply-orders-title">
          <FormattedMessage
            id="supplyOrder.title"
            values={{ count: requests.length }}
          />
        </h3>
        <div className="case-supply-orders-actions">
          <Button
            kind="ghost"
            size="sm"
            hasIconOnly
            renderIcon={Renew}
            iconDescription={intl.formatMessage({
              id: "supplyOrder.button.refresh",
            })}
            onClick={load}
            disabled={loading}
          />
          {fromOpenMrs && (
            <Button
              size="sm"
              onClick={() => {
                setNotice(null);
                setModalOpen(true);
              }}
              disabled={caseClosed}
            >
              <FormattedMessage id="supplyOrder.button.order" />
            </Button>
          )}
        </div>
      </div>

      {!fromOpenMrs && !loading && (
        <p className="case-supply-orders-hint">
          <FormattedMessage id="supplyOrder.hint.notFromOpenMrs" />
        </p>
      )}
      {caseClosed && fromOpenMrs && (
        <p className="case-supply-orders-hint">
          <FormattedMessage id="supplyOrder.hint.caseClosed" />
        </p>
      )}

      {notice && (
        <InlineNotification
          kind={notice.kind}
          lowContrast
          hideCloseButton={false}
          title=""
          subtitle={notice.text}
          onClose={() => setNotice(null)}
        />
      )}

      {requests.length > 0 && (
        <Table size="sm" useZebraStyles={false}>
          <TableHead>
            <TableRow>
              <TableHeader>
                <FormattedMessage id="supplyOrder.column.requested" />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="supplyOrder.column.items" />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="supplyOrder.column.requestedBy" />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="supplyOrder.column.status" />
              </TableHeader>
            </TableRow>
          </TableHead>
          <TableBody>
            {requests.map((request) => (
              <TableRow key={request.id}>
                <TableCell>
                  {request.requestedAt
                    ? intl.formatDate(request.requestedAt, {
                        dateStyle: "short",
                        timeStyle: "short",
                      })
                    : ""}
                </TableCell>
                <TableCell>
                  <ul className="case-supply-orders-items">
                    {request.items.map((item, index) => (
                      <li key={index}>
                        {item.conceptName} × {item.quantity}
                        {item.quantityUnitsName
                          ? " " + item.quantityUnitsName
                          : ""}
                        {item.urgency === "STAT" && (
                          <Tag type="red" size="sm">
                            <FormattedMessage id="supplyOrder.urgency.STAT" />
                          </Tag>
                        )}
                      </li>
                    ))}
                  </ul>
                </TableCell>
                <TableCell>{request.requestedBy}</TableCell>
                <TableCell>
                  <SupplyRequestStatus
                    request={request}
                    retrying={retryingId === request.id}
                    canRetry={!caseClosed}
                    onRetry={() => retry(request)}
                  />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

      {modalOpen && (
        <SupplyOrderModal
          basePath={basePath}
          errorText={errorText}
          onClose={() => setModalOpen(false)}
          onSubmitted={onSubmitted}
        />
      )}
    </section>
  );
}

export function SupplyRequestStatus({ request, retrying, canRetry, onRetry }) {
  if (request.status === "FAILED") {
    return (
      <div className="case-supply-orders-status">
        <Tag type="red" size="sm">
          <FormattedMessage id="supplyOrder.status.notSent" />
        </Tag>
        {request.failureReason && (
          <span className="case-supply-orders-reason">
            {request.failureReason}
          </span>
        )}
        {canRetry && (
          <Button kind="ghost" size="sm" onClick={onRetry} disabled={retrying}>
            <FormattedMessage id="supplyOrder.button.retry" />
          </Button>
        )}
      </div>
    );
  }
  if (request.status !== "SENT") {
    return (
      <Tag type="gray" size="sm">
        <FormattedMessage id="supplyOrder.status.sending" />
      </Tag>
    );
  }
  if (request.daguStatus === "SUCCESS") {
    return (
      <Tag type="green" size="sm">
        <FormattedMessage id="supplyOrder.status.receivedByDagu" />
      </Tag>
    );
  }
  if (request.daguStatus === "FAILED") {
    return (
      <div className="case-supply-orders-status">
        <Tag type="red" size="sm">
          <FormattedMessage id="supplyOrder.status.rejectedByDagu" />
        </Tag>
        {request.daguReason && (
          <span className="case-supply-orders-reason">
            {request.daguReason}
          </span>
        )}
      </div>
    );
  }
  return (
    <Tag type="blue" size="sm">
      <FormattedMessage id="supplyOrder.status.awaitingDagu" />
    </Tag>
  );
}

function SupplyOrderModal({ basePath, errorText, onClose, onSubmitted }) {
  const intl = useIntl();
  const [lines, setLines] = useState([]);
  const [units, setUnits] = useState([]);
  const [options, setOptions] = useState([]);
  const [searching, setSearching] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [comboKey, setComboKey] = useState(0);
  const searchTimer = useRef(null);

  useEffect(() => {
    getFromOpenElisServer("/rest/pathology/supplyOrder/units", (json) => {
      if (Array.isArray(json)) {
        setUnits(json);
      } else if (json && json.code) {
        setError(errorText(json));
      }
    });
    return () => clearTimeout(searchTimer.current);
  }, [errorText]);

  const search = (term) => {
    clearTimeout(searchTimer.current);
    const query = (term || "").trim();
    if (query.length < 2) {
      setOptions([]);
      return;
    }
    searchTimer.current = setTimeout(() => {
      setSearching(true);
      getFromOpenElisServer(
        "/rest/pathology/supplyOrder/catalog?q=" + encodeURIComponent(query),
        (json) => {
          setSearching(false);
          if (Array.isArray(json)) {
            setOptions(json);
          } else if (json && json.code) {
            setError(errorText(json));
          }
        },
      );
    }, SEARCH_DELAY_MS);
  };

  const addLine = (option) => {
    if (!option) {
      return;
    }
    setLines((prev) =>
      prev.some((l) => l.conceptUuid === option.uuid)
        ? prev
        : [
            ...prev,
            {
              conceptUuid: option.uuid,
              conceptName: option.display,
              quantity: 1,
              quantityUnitsUuid: "",
              quantityUnitsName: "",
              urgency: "ROUTINE",
              instructions: "",
            },
          ],
    );
    setOptions([]);
    setComboKey((k) => k + 1);
  };

  const updateLine = (index, changes) =>
    setLines((prev) =>
      prev.map((line, i) => (i === index ? { ...line, ...changes } : line)),
    );

  const removeLine = (index) =>
    setLines((prev) => prev.filter((_, i) => i !== index));

  const canSubmit = lines.length > 0 && lines.every(lineIsValid) && !submitting;

  const submit = () => {
    if (!canSubmit) {
      return;
    }
    setSubmitting(true);
    setError(null);
    postToOpenElisServerJsonResponse(
      basePath + "/supplyRequests",
      JSON.stringify({
        items: lines.map((line) => ({
          ...line,
          quantityUnitsUuid: line.quantityUnitsUuid || null,
          quantityUnitsName: line.quantityUnitsName || null,
          instructions: line.instructions.trim() || null,
        })),
      }),
      (json) => {
        setSubmitting(false);
        if (isErrorResponse(json)) {
          setError(errorText(json));
        } else {
          onSubmitted(json);
        }
      },
    );
  };

  return (
    <Modal
      open
      size="lg"
      modalHeading={intl.formatMessage({ id: "supplyOrder.modal.title" })}
      aria-label={intl.formatMessage({ id: "supplyOrder.modal.title" })}
      primaryButtonText={intl.formatMessage(
        { id: "supplyOrder.button.submit" },
        { count: lines.length },
      )}
      secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
      primaryButtonDisabled={!canSubmit}
      onRequestSubmit={submit}
      onRequestClose={onClose}
      onSecondarySubmit={onClose}
      hasScrollingContent
      className="case-supply-orders-modal"
    >
      <p className="case-supply-orders-hint">
        <FormattedMessage id="supplyOrder.modal.intro" />
      </p>
      {error && (
        <InlineNotification
          kind="error"
          lowContrast
          title=""
          subtitle={error}
          onClose={() => setError(null)}
        />
      )}
      <ComboBox
        key={comboKey}
        id="supply-order-search"
        titleText={intl.formatMessage({ id: "supplyOrder.field.search" })}
        placeholder={intl.formatMessage({
          id: "supplyOrder.field.searchPlaceholder",
        })}
        items={options}
        itemToString={(item) => (item ? item.display : "")}
        shouldFilterItem={() => true}
        onInputChange={(value) =>
          search(typeof value === "string" ? value : value?.inputValue)
        }
        onChange={({ selectedItem }) => addLine(selectedItem)}
        helperText={
          searching
            ? intl.formatMessage({ id: "supplyOrder.field.searching" })
            : ""
        }
      />

      {lines.length === 0 ? (
        <p className="case-supply-orders-empty">
          <FormattedMessage id="supplyOrder.modal.empty" />
        </p>
      ) : (
        <div className="case-supply-orders-lines">
          {lines.map((line, index) => (
            <div className="case-supply-orders-line" key={line.conceptUuid}>
              <div className="case-supply-orders-line-name">
                {line.conceptName}
              </div>
              <NumberInput
                id={"supply-qty-" + index}
                label={intl.formatMessage({ id: "supplyOrder.field.quantity" })}
                min={1}
                max={MAX_QUANTITY}
                step={1}
                value={line.quantity ?? ""}
                allowEmpty
                invalid={!lineIsValid({ ...line, instructions: "" })}
                invalidText={intl.formatMessage({
                  id: "supplyOrder.field.quantityInvalid",
                })}
                onChange={(event, { value } = {}) => {
                  // Carbon's stepper yields NaN when the field is empty; never
                  // store NaN, its controlled-value check would loop forever.
                  const raw = value !== undefined ? value : event.target.value;
                  const quantity =
                    raw === "" || raw === null ? NaN : Number(raw);
                  updateLine(index, {
                    quantity: Number.isFinite(quantity) ? quantity : null,
                  });
                }}
              />
              <Select
                id={"supply-unit-" + index}
                labelText={intl.formatMessage({ id: "supplyOrder.field.unit" })}
                value={line.quantityUnitsUuid}
                onChange={(event) => {
                  const unit = units.find((u) => u.uuid === event.target.value);
                  updateLine(index, {
                    quantityUnitsUuid: unit ? unit.uuid : "",
                    quantityUnitsName: unit ? unit.display : "",
                  });
                }}
              >
                <SelectItem
                  value=""
                  text={intl.formatMessage({ id: "supplyOrder.field.noUnit" })}
                />
                {units.map((unit) => (
                  <SelectItem
                    key={unit.uuid}
                    value={unit.uuid}
                    text={unit.display}
                  />
                ))}
              </Select>
              <Select
                id={"supply-urgency-" + index}
                labelText={intl.formatMessage({
                  id: "supplyOrder.field.priority",
                })}
                value={line.urgency}
                onChange={(event) =>
                  updateLine(index, { urgency: event.target.value })
                }
              >
                {URGENCIES.map((urgency) => (
                  <SelectItem
                    key={urgency}
                    value={urgency}
                    text={intl.formatMessage({
                      id: "supplyOrder.urgency." + urgency,
                    })}
                  />
                ))}
              </Select>
              <TextInput
                id={"supply-note-" + index}
                labelText={intl.formatMessage({ id: "supplyOrder.field.note" })}
                value={line.instructions}
                maxCount={MAX_INSTRUCTIONS}
                enableCounter
                onChange={(event) =>
                  updateLine(index, { instructions: event.target.value })
                }
              />
              <Button
                kind="ghost"
                size="md"
                hasIconOnly
                renderIcon={TrashCan}
                iconDescription={intl.formatMessage({
                  id: "supplyOrder.button.remove",
                })}
                onClick={() => removeLine(index)}
              />
            </div>
          ))}
        </div>
      )}
    </Modal>
  );
}

export default CaseSupplyOrders;
