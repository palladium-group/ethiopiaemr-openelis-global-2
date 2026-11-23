import React, { useEffect, useMemo, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import config from "../../config.json";
import { postToOpenElisServerFullResponse } from "../utils/Utils";

const FLOW = "FLOW_CYTOMETRY";
const BONE_MARROW = "BONE_MARROW_ASPIRATE";

/** Rank of each status along the rail; REJECTED stops after Collection. */
const STATUS_RANK = {
  RECEIVED: 1,
  AWAITING_PAYMENT: 2,
  SMEAR_PREP: 3,
  PROCESSING: 3,
  ACQUISITION: 4,
  READY_FOR_HEMATOPATHOLOGIST: 5,
  COMPLETED: 6,
};

/**
 * Steps per test. {@code doneAt} is the first status rank past the step;
 * {@code activeStatus} is the status in which the step is worked.
 */
export const hematopathologySteps = (subtype) =>
  subtype === FLOW
    ? [
        {
          id: "collection",
          labelId: "hematopathology.workflow.collection",
          activeStatus: "RECEIVED",
          doneAt: 2,
        },
        {
          id: "processing",
          labelId: "hematopathology.workflow.processing",
          activeStatus: "PROCESSING",
          doneAt: 4,
        },
        {
          id: "acquisition",
          labelId: "hematopathology.workflow.acquisition",
          activeStatus: "ACQUISITION",
          doneAt: 5,
        },
        {
          id: "review",
          labelId: "hematopathology.workflow.reviewAndReport",
          activeStatus: "READY_FOR_HEMATOPATHOLOGIST",
          doneAt: 6,
        },
      ]
    : [
        {
          id: "collection",
          labelId: "hematopathology.workflow.collection",
          activeStatus: "RECEIVED",
          doneAt: 2,
        },
        {
          id: "smearPrep",
          labelId: "hematopathology.workflow.smearPrep",
          activeStatus: "SMEAR_PREP",
          doneAt: 5,
        },
        {
          id: "review",
          labelId: "hematopathology.workflow.reviewAndReport",
          activeStatus: "READY_FOR_HEMATOPATHOLOGIST",
          doneAt: 6,
        },
      ];

/**
 * completed | active | locked for one step. A case held for payment shows the
 * hold on the first lab step (index 1).
 */
export const stepState = (step, index, status) => {
  if (status === "REJECTED") {
    return step.id === "collection" ? "completed" : "locked";
  }
  const rank = STATUS_RANK[status] || 0;
  if (rank >= step.doneAt) {
    return "completed";
  }
  if (status === step.activeStatus) {
    return "active";
  }
  if (status === "AWAITING_PAYMENT" && index === 1) {
    return "active";
  }
  return "locked";
};

const READ_FIELDS = {
  morphology: [
    "smearQuality",
    "peripheralBloodMorphology",
    "boneMarrowMorphology",
    "conclusion",
    "comment",
  ],
  flow: [
    "specimenViability",
    "antibodyPanel",
    "gatingFindings",
    "conclusion",
    "comment",
  ],
};

/** Result fields shown for the test: Peripheral morphology has no bone-marrow box. */
export const readFieldsFor = (subtype) =>
  subtype === FLOW
    ? READ_FIELDS.flow
    : READ_FIELDS.morphology.filter(
        (field) => field !== "boneMarrowMorphology" || subtype === BONE_MARROW,
      );

/**
 * Hematopathology case rail.
 * Morphology: Collection → Smear prep & staining → Review & report.
 * Flow cytometry: Collection → Processing → Acquisition & analysis → Review & report.
 */
function HematopathologyCaseWorkflowRail({ caseId, caseInfo, onCaseUpdated }) {
  const intl = useIntl();
  const [busy, setBusy] = useState(null);
  const [error, setError] = useState(null);
  const [expandedCompleted, setExpandedCompleted] = useState({});

  const [collectionNotes, setCollectionNotes] = useState("");
  const [spiculesPresent, setSpiculesPresent] = useState(false);
  const [sampleAgeHours, setSampleAgeHours] = useState("");
  const [rejectionReason, setRejectionReason] = useState("");
  const [readDraft, setReadDraft] = useState({});

  const status = caseInfo?.status;
  const subtype = caseInfo?.subtype || "PERIPHERAL_MORPHOLOGY";
  const isFlow = subtype === FLOW;
  const isBoneMarrow = subtype === BONE_MARROW;
  const isAwaitingPayment = status === "AWAITING_PAYMENT";
  const isRejected = status === "REJECTED";
  const paymentStatus = caseInfo?.paymentStatus;
  const labNo = caseInfo?.labNumber;
  const steps = useMemo(() => hematopathologySteps(subtype), [subtype]);
  const readFields = useMemo(() => readFieldsFor(subtype), [subtype]);

  useEffect(() => {
    setCollectionNotes(caseInfo?.collectionNotes || "");
    setSpiculesPresent(!!caseInfo?.spiculesPresent);
    setSampleAgeHours(caseInfo?.sampleAgeHours || "");
    setRejectionReason(caseInfo?.rejectionReason || "");
    const draft = {};
    READ_FIELDS.morphology
      .concat(READ_FIELDS.flow)
      .forEach((field) => (draft[field] = caseInfo?.[field] || ""));
    setReadDraft(draft);
  }, [caseId, caseInfo]);

  const post = (path, body, busyKey) => {
    if (busy) {
      return;
    }
    setBusy(busyKey);
    setError(null);
    postToOpenElisServerFullResponse(
      "/rest/hematopathology/caseView/" + caseId + "/" + path,
      JSON.stringify(body || {}),
      (response) => {
        if (response && response.ok) {
          response
            .json()
            .then((data) => onCaseUpdated && onCaseUpdated(data))
            .finally(() => setBusy(null));
        } else if (response) {
          response
            .text()
            .then((text) => setError(text || String(response.status)))
            .finally(() => setBusy(null));
        } else {
          setBusy(null);
        }
      },
    );
  };

  // Re-check payment when a held case is opened.
  useEffect(() => {
    if (isAwaitingPayment && !paymentStatus) {
      post("checkPayment", {}, "payment");
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isAwaitingPayment, caseId]);

  const formatDateTime = (value) => {
    if (!value) {
      return null;
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return String(value);
    }
    return date.toLocaleString(undefined, {
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const printContainerLabel = () => {
    if (labNo) {
      window.open(
        config.serverBaseUrl +
          "/LabelMakerServlet?labNo=" +
          encodeURIComponent(labNo) +
          "&type=default",
        "_blank",
      );
    }
  };

  const renderText = (id, labelId, value, onChange, editable, rows) => (
    <div key={id} className="pathology-grossing-field">
      <label className="pathology-grossing-label" htmlFor={id}>
        <FormattedMessage id={labelId} />
      </label>
      {editable ? (
        <textarea
          id={id}
          className="pathology-grossing-textarea"
          rows={rows || 2}
          value={value}
          onChange={(e) => onChange(e.target.value)}
        />
      ) : (
        <div className="pathology-grossing-readonly">
          {value && String(value).trim() ? value : "—"}
        </div>
      )}
    </div>
  );

  const renderCollectionCard = (editable) => (
    <div
      className={
        "pathology-grossing-card" +
        (editable ? "" : " pathology-grossing-card--compact")
      }
    >
      <div
        className="pathology-container-card-actions"
        style={{ marginBottom: "0.75rem" }}
      >
        <button
          type="button"
          className="pathology-btn pathology-btn--ghost"
          onClick={printContainerLabel}
          disabled={!labNo}
        >
          <FormattedMessage id="hematopathology.workflow.printContainerLabel" />
        </button>
      </div>
      {renderText(
        "heme-collection-notes",
        "hematopathology.workflow.collectionNotes",
        editable ? collectionNotes : caseInfo?.collectionNotes,
        setCollectionNotes,
        editable,
      )}
      {isBoneMarrow && (
        <label className="pathology-conclusion-option">
          <input
            id="heme-spicules"
            type="checkbox"
            checked={editable ? spiculesPresent : !!caseInfo?.spiculesPresent}
            disabled={!editable}
            onChange={(e) => setSpiculesPresent(e.target.checked)}
          />
          <span>
            <FormattedMessage id="hematopathology.workflow.spiculesPresent" />
          </span>
        </label>
      )}
      {isFlow &&
        renderText(
          "heme-sample-age",
          "hematopathology.workflow.sampleAgeHours",
          editable ? sampleAgeHours : caseInfo?.sampleAgeHours,
          setSampleAgeHours,
          editable,
          1,
        )}
      {isRejected && (
        <div
          className="pathology-grossing-readonly"
          style={{ marginTop: "0.5rem" }}
        >
          <FormattedMessage
            id="hematopathology.workflow.rejectedReason"
            values={{ reason: caseInfo?.rejectionReason || "—" }}
          />
        </div>
      )}
      {editable && (
        <>
          <div className="pathology-collection-reject-block">
            {renderText(
              "heme-rejection-reason",
              "hematopathology.workflow.rejectionReason",
              rejectionReason,
              setRejectionReason,
              true,
              1,
            )}
          </div>
          <div className="pathology-grossing-footer pathology-collection-actions">
            <button
              type="button"
              className="pathology-btn pathology-btn--danger"
              disabled={!!busy || !rejectionReason.trim()}
              onClick={() =>
                post("rejectCollection", { rejectionReason }, "reject")
              }
            >
              <FormattedMessage id="hematopathology.workflow.rejectCollection" />
            </button>
            <button
              type="button"
              className="pathology-btn pathology-btn--primary"
              disabled={!!busy}
              onClick={() =>
                post(
                  "confirmCollection",
                  {
                    collectionNotes,
                    spiculesPresent: isBoneMarrow ? spiculesPresent : null,
                    sampleAgeHours: isFlow ? sampleAgeHours : null,
                  },
                  "collect",
                )
              }
            >
              <FormattedMessage id="hematopathology.workflow.confirmCollection" />
            </button>
          </div>
        </>
      )}
    </div>
  );

  const renderPaymentHoldCard = () => (
    <div className="pathology-container-card">
      <div className="pathology-container-card-copy">
        <div className="pathology-container-card-title">
          <FormattedMessage id="hematopathology.workflow.awaitingPaymentTitle" />
        </div>
        <div className="pathology-container-card-meta">
          {paymentStatus ? (
            <FormattedMessage
              id="hematopathology.workflow.awaitingPaymentStatus"
              values={{
                status: intl.formatMessage({
                  id: "eorder.payment.status." + paymentStatus,
                  defaultMessage: paymentStatus,
                }),
              }}
            />
          ) : (
            <FormattedMessage id="hematopathology.workflow.awaitingPaymentChecking" />
          )}
        </div>
      </div>
      <div className="pathology-container-card-actions">
        <button
          type="button"
          className="pathology-btn pathology-btn--primary"
          disabled={!!busy}
          onClick={() => post("checkPayment", {}, "payment")}
        >
          <FormattedMessage id="hematopathology.workflow.checkPayment" />
        </button>
      </div>
    </div>
  );

  /** Checklist rows: [{ step, labelId, at, enabled }] marked via /step/{step}. */
  const renderChecklist = (rows, allowMark) => (
    <div
      className={
        "pathology-processing-card" +
        (allowMark ? "" : " pathology-processing-card--compact")
      }
    >
      <div className="pathology-cassette-list">
        {rows.map((row) => {
          const done = !!row.at;
          return (
            <div
              key={row.step}
              className={
                "pathology-cassette-row" +
                (done ? " pathology-cassette-row--done" : "")
              }
            >
              <div className="pathology-cassette-row-main">
                <div className="pathology-cassette-code">
                  <FormattedMessage id={row.labelId} />
                </div>
                {done && (
                  <div className="pathology-cassette-embedded-meta">
                    {formatDateTime(row.at) || "—"}
                  </div>
                )}
              </div>
              <div className="pathology-cassette-row-actions">
                {allowMark && !done && (
                  <button
                    type="button"
                    id={"heme-step-" + row.step}
                    className="pathology-btn pathology-btn--primary pathology-btn--sm"
                    disabled={!row.enabled || !!busy}
                    onClick={() => post("step/" + row.step, {}, row.step)}
                  >
                    <FormattedMessage id="hematopathology.workflow.markStepDone" />
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );

  const smearRows = [
    {
      step: "smearPrepared",
      labelId: "hematopathology.workflow.smearPrepared",
      at: caseInfo?.smearPreparedAt,
      enabled: true,
    },
    {
      step: "smearStained",
      labelId: "hematopathology.workflow.smearStained",
      at: caseInfo?.smearStainedAt,
      enabled: !!caseInfo?.smearPreparedAt,
    },
  ];
  const processingRows = [
    {
      step: "processing",
      labelId: "hematopathology.workflow.processingDone",
      at: caseInfo?.processingCompletedAt,
      enabled: true,
    },
  ];
  const acquisitionRows = [
    {
      step: "acquisition",
      labelId: "hematopathology.workflow.acquisitionDone",
      at: caseInfo?.acquisitionCompletedAt,
      enabled: true,
    },
  ];

  const renderReadCard = (editable) => (
    <div
      className={
        "pathology-read-card" +
        (editable ? "" : " pathology-read-card--compact")
      }
    >
      {readFields.map((field) =>
        renderText(
          "heme-" + field,
          "hematopathology.result." + field,
          editable ? readDraft[field] : caseInfo?.[field],
          (value) => setReadDraft((prev) => ({ ...prev, [field]: value })),
          editable,
          field === "conclusion" || field === "comment" ? 2 : 4,
        ),
      )}
      {editable ? (
        <div className="pathology-read-footer">
          <button
            type="button"
            className="pathology-btn pathology-btn--ghost"
            disabled={!!busy}
            onClick={() => post("saveReadDraft", readDraft, "draft")}
          >
            <FormattedMessage id="hematopathology.workflow.saveDraft" />
          </button>
          <button
            type="button"
            className="pathology-btn pathology-btn--primary"
            disabled={!!busy || !readDraft.conclusion?.trim()}
            onClick={() => post("signOut", readDraft, "signOut")}
          >
            <FormattedMessage id="hematopathology.workflow.signOutFinalize" />
          </button>
        </div>
      ) : (
        <div className="pathology-read-signed-note">
          <FormattedMessage
            id="hematopathology.workflow.signedOutNote"
            values={{ pathologist: caseInfo?.assignedHematopathologist || "—" }}
          />
        </div>
      )}
    </div>
  );

  const stepBody = (stepId, editable) => {
    switch (stepId) {
      case "collection":
        return renderCollectionCard(editable);
      case "smearPrep":
        return renderChecklist(smearRows, editable);
      case "processing":
        return renderChecklist(processingRows, editable);
      case "acquisition":
        return renderChecklist(acquisitionRows, editable);
      case "review":
        return renderReadCard(editable);
      default:
        return null;
    }
  };

  const completedSummary = (stepId) => {
    if (stepId === "collection") {
      return isRejected
        ? intl.formatMessage(
            { id: "hematopathology.workflow.collectionSummaryRejected" },
            { reason: caseInfo?.rejectionReason || "—" },
          )
        : intl.formatMessage(
            { id: "hematopathology.workflow.collectionSummaryDone" },
            {
              when:
                formatDateTime(
                  caseInfo?.collectionConfirmedAt || caseInfo?.collectionDate,
                ) || "—",
            },
          );
    }
    return intl.formatMessage({ id: "hematopathology.workflow.stepDone" });
  };

  const badgeLabelId = isRejected
    ? "hematopathology.workflow.badgeRejected"
    : "hematopathology.status." + (status || "RECEIVED");

  const patientName = [caseInfo?.firstName, caseInfo?.lastName]
    .filter(Boolean)
    .join(" ");
  const subtypeLabel = intl.formatMessage({
    id: "hematopathology.subtype." + subtype,
  });

  return (
    <div className="pathology-case-rail">
      <div className="pathology-case-rail-header">
        <div className="pathology-case-rail-patient">
          <div className="pathology-case-rail-name">
            {patientName || (
              <FormattedMessage id="hematopathology.workflow.case" />
            )}
          </div>
          <div className="pathology-case-rail-meta">
            {[
              labNo,
              subtypeLabel,
              caseInfo?.requester,
              caseInfo?.assignedHematopathologist,
            ]
              .filter(Boolean)
              .join(" · ")}
          </div>
        </div>
        <span
          className={
            "pathology-case-rail-badge" +
            (status && status !== "RECEIVED" && !isRejected
              ? " pathology-case-rail-badge--progress"
              : "")
          }
        >
          <FormattedMessage id={badgeLabelId} />
        </span>
      </div>

      {error && (
        <div className="pathology-grossing-readonly" role="alert">
          {error}
        </div>
      )}

      <ol className="pathology-rail">
        {steps.map((step, index) => {
          const state = stepState(step, index, status);
          const label = intl.formatMessage({ id: step.labelId });
          const isLast = index === steps.length - 1;
          return (
            <li
              key={step.id}
              className={`pathology-rail-step pathology-rail-step--${state}`}
              data-step={step.id}
              data-state={state}
            >
              <div className="pathology-rail-track" aria-hidden="true">
                <span className="pathology-rail-dot">
                  {state === "completed" ? (
                    <svg viewBox="0 0 20 20" width="14" height="14">
                      <path
                        fill="currentColor"
                        d="M7.7 13.3 4.5 10.1l1.1-1.1 2.1 2.1 5.2-5.2 1.1 1.1z"
                      />
                    </svg>
                  ) : (
                    index + 1
                  )}
                </span>
                {!isLast && <span className="pathology-rail-line" />}
              </div>
              <div className="pathology-rail-body">
                {state === "completed" && (
                  <div className="pathology-rail-panel pathology-rail-panel--completed">
                    <button
                      type="button"
                      className="pathology-rail-toggle"
                      aria-expanded={!!expandedCompleted[step.id]}
                      onClick={() =>
                        setExpandedCompleted((prev) => ({
                          ...prev,
                          [step.id]: !prev[step.id],
                        }))
                      }
                    >
                      <span className="pathology-rail-toggle-text">
                        <span className="pathology-rail-collapsed-title">
                          {label}
                        </span>
                        <span className="pathology-rail-collapsed-summary">
                          {" — "}
                          {completedSummary(step.id)}
                        </span>
                      </span>
                      <span
                        className={
                          "pathology-rail-chevron" +
                          (expandedCompleted[step.id]
                            ? " pathology-rail-chevron--open"
                            : "")
                        }
                        aria-hidden="true"
                      />
                    </button>
                    {expandedCompleted[step.id] && stepBody(step.id, false)}
                  </div>
                )}
                {state === "active" && (
                  <div className="pathology-rail-panel">
                    <h3 className="pathology-rail-step-title">{label}</h3>
                    <p className="pathology-rail-step-copy">
                      <FormattedMessage
                        id={
                          isAwaitingPayment
                            ? "hematopathology.workflow.awaitingPaymentIntro"
                            : "hematopathology.workflow." + step.id + "Intro"
                        }
                      />
                    </p>
                    {isAwaitingPayment
                      ? renderPaymentHoldCard()
                      : stepBody(step.id, true)}
                  </div>
                )}
                {state === "locked" && (
                  <div className="pathology-rail-collapsed pathology-rail-collapsed--locked">
                    <span className="pathology-rail-collapsed-title">
                      {label}
                    </span>
                    <span className="pathology-rail-collapsed-summary">
                      {" — "}
                      <FormattedMessage id="hematopathology.workflow.notYetStarted" />
                    </span>
                  </div>
                )}
              </div>
            </li>
          );
        })}
      </ol>
    </div>
  );
}

export default HematopathologyCaseWorkflowRail;
