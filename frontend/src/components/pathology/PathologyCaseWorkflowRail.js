import React, { useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import config from "../../config.json";
import { postToOpenElisServerFullResponse } from "../utils/Utils";

const WORKFLOW_STEPS = [
  { id: "collection", labelId: "pathology.workflow.collection" },
  { id: "grossing", labelId: "pathology.workflow.grossing" },
  { id: "processing", labelId: "pathology.workflow.processing" },
  { id: "embedding", labelId: "pathology.workflow.embedding" },
  { id: "microtomy", labelId: "pathology.workflow.microtomy" },
  { id: "staining", labelId: "pathology.workflow.staining" },
  { id: "review", labelId: "pathology.workflow.review" },
  { id: "release", labelId: "pathology.workflow.release" },
];

/**
 * PDF-style vertical progress rail for Steps 3–10.
 * Completed steps are collapsed by default; user can expand to view details /
 * allowed actions (e.g. reprint). Only Collection is fully interactive so far.
 */
function PathologyCaseWorkflowRail({
  pathologySampleId,
  pathologySampleInfo,
  onCaseUpdated,
}) {
  const intl = useIntl();
  const [confirming, setConfirming] = useState(false);
  /** Completed step ids the user has manually expanded. */
  const [expandedCompleted, setExpandedCompleted] = useState({});

  const isCollected = !!pathologySampleInfo?.collectionDate;

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
    const labNo = pathologySampleInfo?.labNumber;
    if (!labNo) {
      return;
    }
    window.open(
      config.serverBaseUrl +
        "/LabelMakerServlet?labNo=" +
        encodeURIComponent(labNo) +
        "&type=specimen",
      "_blank",
    );
  };

  const confirmReceived = () => {
    if (confirming || isCollected) {
      return;
    }
    setConfirming(true);
    postToOpenElisServerFullResponse(
      "/rest/pathology/caseView/" + pathologySampleId + "/confirmReceived",
      "{}",
      (response) => {
        if (response && response.ok) {
          response
            .json()
            .then((data) => {
              if (onCaseUpdated) {
                onCaseUpdated(data);
              }
            })
            .finally(() => setConfirming(false));
        } else {
          setConfirming(false);
        }
      },
    );
  };

  const stepState = (stepId) => {
    if (stepId === "collection") {
      return isCollected ? "completed" : "active";
    }
    if (stepId === "grossing" && isCollected) {
      return "active";
    }
    return "locked";
  };

  const toggleCompleted = (stepId) => {
    setExpandedCompleted((prev) => ({
      ...prev,
      [stepId]: !prev[stepId],
    }));
  };

  const patientName = [pathologySampleInfo?.firstName, pathologySampleInfo?.lastName]
    .filter(Boolean)
    .join(" ");
  const assignedAt = formatDateTime(pathologySampleInfo?.assignedAt);
  const collectedAt = formatDateTime(pathologySampleInfo?.collectionDate);
  const requester = pathologySampleInfo?.requester || "—";
  const pathologist = pathologySampleInfo?.assignedPathologist;

  const collectionSummaryLine = [
    requester !== "—"
      ? intl.formatMessage(
          { id: "pathology.workflow.requesterAssigned" },
          {
            requester,
            when: assignedAt || "—",
          },
        )
      : null,
    pathologist
      ? intl.formatMessage(
          { id: "pathology.workflow.assignedTo" },
          { name: pathologist },
        )
      : null,
  ]
    .filter(Boolean)
    .join(" · ");

  const completedCollectionSummary = intl.formatMessage(
    { id: "pathology.workflow.collectionSummaryDone" },
    {
      when: collectedAt || "—",
      requester,
    },
  );

  const renderCollectionCard = ({ showConfirm }) => (
    <div
      className={
        "pathology-container-card" +
        (showConfirm ? "" : " pathology-container-card--compact")
      }
    >
      <div className="pathology-container-card-copy">
        <div className="pathology-container-card-title">
          <FormattedMessage id="pathology.workflow.containerFromReception" />
        </div>
        <div className="pathology-container-card-meta">
          {collectionSummaryLine || "—"}
        </div>
      </div>
      <div className="pathology-container-card-actions">
        <button
          type="button"
          className="pathology-btn pathology-btn--ghost"
          onClick={printContainerLabel}
          disabled={!pathologySampleInfo?.labNumber}
        >
          <FormattedMessage id="pathology.workflow.printContainerLabel" />
        </button>
        {showConfirm && (
          <button
            type="button"
            className="pathology-btn pathology-btn--primary"
            disabled={confirming}
            onClick={confirmReceived}
          >
            <FormattedMessage id="pathology.workflow.confirmReceived" />
          </button>
        )}
      </div>
    </div>
  );

  return (
    <div className="pathology-case-rail">
      <div className="pathology-case-rail-header">
        <div className="pathology-case-rail-patient">
          <div className="pathology-case-rail-name">
            {patientName || <FormattedMessage id="pathology.workflow.case" />}
          </div>
          <div className="pathology-case-rail-meta">
            {[
              pathologySampleInfo?.labNumber,
              requester !== "—" ? requester : null,
              pathologist,
            ]
              .filter(Boolean)
              .join(" · ")}
          </div>
        </div>
        <span
          className={
            "pathology-case-rail-badge" +
            (isCollected ? " pathology-case-rail-badge--progress" : "")
          }
        >
          {isCollected ? (
            <FormattedMessage id="pathology.workflow.badgeGrossing" />
          ) : (
            <FormattedMessage id="pathology.workflow.badgeCollection" />
          )}
        </span>
      </div>

      <ol className="pathology-rail">
        {WORKFLOW_STEPS.map((step, index) => {
          const state = stepState(step.id);
          const stepNumber = index + 1;
          const label = intl.formatMessage({ id: step.labelId });
          const isLast = index === WORKFLOW_STEPS.length - 1;
          const isExpandedCompleted = !!expandedCompleted[step.id];

          return (
            <li
              key={step.id}
              className={`pathology-rail-step pathology-rail-step--${state}`}
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
                    stepNumber
                  )}
                </span>
                {!isLast && <span className="pathology-rail-line" />}
              </div>

              <div className="pathology-rail-body">
                {state === "completed" && step.id === "collection" && (
                  <div className="pathology-rail-panel pathology-rail-panel--completed">
                    <button
                      type="button"
                      className="pathology-rail-toggle"
                      aria-expanded={isExpandedCompleted}
                      onClick={() => toggleCompleted(step.id)}
                    >
                      <span className="pathology-rail-toggle-text">
                        <span className="pathology-rail-collapsed-title">
                          {label}
                        </span>
                        <span className="pathology-rail-collapsed-summary">
                          {" — "}
                          {completedCollectionSummary}
                        </span>
                      </span>
                      <span
                        className={
                          "pathology-rail-chevron" +
                          (isExpandedCompleted
                            ? " pathology-rail-chevron--open"
                            : "")
                        }
                        aria-hidden="true"
                      />
                    </button>
                    {isExpandedCompleted &&
                      renderCollectionCard({ showConfirm: false })}
                  </div>
                )}

                {state === "active" && step.id === "collection" && (
                  <div className="pathology-rail-panel">
                    <h3 className="pathology-rail-step-title">{label}</h3>
                    <p className="pathology-rail-step-copy">
                      <FormattedMessage id="pathology.workflow.collectionIntro" />
                    </p>
                    {renderCollectionCard({ showConfirm: true })}
                  </div>
                )}

                {state === "active" && step.id !== "collection" && (
                  <div className="pathology-rail-panel">
                    <h3 className="pathology-rail-step-title">{label}</h3>
                    <p className="pathology-rail-step-copy pathology-rail-step-copy--muted">
                      <FormattedMessage id="pathology.workflow.placeholder" />
                    </p>
                  </div>
                )}

                {state === "locked" && (
                  <div className="pathology-rail-collapsed pathology-rail-collapsed--locked">
                    <span className="pathology-rail-collapsed-title">
                      {label}
                    </span>
                    <span className="pathology-rail-collapsed-summary">
                      {" — "}
                      <FormattedMessage id="pathology.workflow.notYetStarted" />
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

export default PathologyCaseWorkflowRail;
