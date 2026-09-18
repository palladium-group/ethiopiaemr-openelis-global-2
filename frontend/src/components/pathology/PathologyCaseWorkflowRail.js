import React, { useEffect, useState } from "react";
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

/** Statuses at or past Grossing completion (GROSSING → PROCESSING jump). */
const PAST_GROSSING = new Set([
  "CUTTING",
  "PROCESSING",
  "EMBEDDING",
  "SLICING",
  "STAINING",
  "READY_PATHOLOGIST",
  "ADDITIONAL_REQUEST",
  "COMPLETED",
]);

/** Statuses at or past Processing completion (PROCESSING → EMBEDDING). */
const PAST_PROCESSING = new Set([
  "EMBEDDING",
  "SLICING",
  "STAINING",
  "READY_PATHOLOGIST",
  "ADDITIONAL_REQUEST",
  "COMPLETED",
]);

/** Statuses at or past Embedding completion (EMBEDDING → SLICING / Microtomy). */
const PAST_EMBEDDING = new Set([
  "SLICING",
  "STAINING",
  "READY_PATHOLOGIST",
  "ADDITIONAL_REQUEST",
  "COMPLETED",
]);

const cassetteSuffix = (index) => "A" + (index + 1);

const cassetteCode = (labNo, index) => {
  const suffix = cassetteSuffix(index);
  return labNo ? labNo + "." + suffix : suffix;
};

const blockDisplayCode = (block, labNo, index) => {
  if (block?.location && labNo) {
    return labNo + "." + block.location;
  }
  return cassetteCode(labNo, index);
};

/**
 * PDF-style vertical progress rail for Steps 3–10.
 * Completed steps are collapsed by default; user can expand to view details /
 * allowed actions (e.g. reprint). Collection through Embedding are interactive.
 */
function PathologyCaseWorkflowRail({
  pathologySampleId,
  pathologySampleInfo,
  onCaseUpdated,
}) {
  const intl = useIntl();
  const [confirming, setConfirming] = useState(false);
  const [sending, setSending] = useState(false);
  const [markingComplete, setMarkingComplete] = useState(false);
  const [markingBlockId, setMarkingBlockId] = useState(null);
  /** Completed step ids the user has manually expanded. */
  const [expandedCompleted, setExpandedCompleted] = useState({});
  const [grossExamDraft, setGrossExamDraft] = useState("");
  /** Local draft cassette count (suffixes A1..An). Persisted only on send. */
  const [cassetteCount, setCassetteCount] = useState(0);

  const status = pathologySampleInfo?.status;
  const isCollected = !!pathologySampleInfo?.collectionDate;
  const isGrossingDone = PAST_GROSSING.has(status);
  const isGrossingActive = isCollected && status === "GROSSING";
  const isProcessingDone = PAST_PROCESSING.has(status);
  const isProcessingActive = status === "PROCESSING";
  const isEmbeddingDone = PAST_EMBEDDING.has(status);
  const isEmbeddingActive = status === "EMBEDDING";
  const labNo = pathologySampleInfo?.labNumber;

  useEffect(() => {
    if (isGrossingActive) {
      setGrossExamDraft(pathologySampleInfo?.grossExam || "");
      const existing = pathologySampleInfo?.blocks?.length || 0;
      setCassetteCount(existing > 0 ? existing : 0);
    }
  }, [
    isGrossingActive,
    pathologySampleInfo?.grossExam,
    pathologySampleInfo?.blocks,
    pathologySampleId,
  ]);

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

  const printCassetteLabel = (code) => {
    if (!code) {
      return;
    }
    window.open(
      config.serverBaseUrl +
        "/LabelMakerServlet?labelType=block&code=" +
        encodeURIComponent(code),
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

  const addCassette = () => {
    setCassetteCount((n) => n + 1);
  };

  const removeCassette = (index) => {
    setCassetteCount((n) => Math.max(0, n - 1));
  };

  const sendToProcessing = () => {
    if (sending || !isGrossingActive || cassetteCount < 1) {
      return;
    }
    const blocks = Array.from({ length: cassetteCount }, (_, index) => ({
      blockNumber: index + 1,
      location: cassetteSuffix(index),
    }));
    setSending(true);
    postToOpenElisServerFullResponse(
      "/rest/pathology/caseView/" + pathologySampleId + "/sendToProcessing",
      JSON.stringify({
        grossExam: grossExamDraft,
        blocks,
      }),
      (response) => {
        if (response && response.ok) {
          response
            .json()
            .then((data) => {
              if (onCaseUpdated) {
                onCaseUpdated(data);
              }
            })
            .finally(() => setSending(false));
        } else {
          setSending(false);
        }
      },
    );
  };

  const markProcessingComplete = () => {
    if (markingComplete || !isProcessingActive) {
      return;
    }
    setMarkingComplete(true);
    postToOpenElisServerFullResponse(
      "/rest/pathology/caseView/" + pathologySampleId + "/markProcessingComplete",
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
            .finally(() => setMarkingComplete(false));
        } else {
          setMarkingComplete(false);
        }
      },
    );
  };

  const markBlockEmbedded = (blockId) => {
    if (!isEmbeddingActive || !blockId || markingBlockId) {
      return;
    }
    setMarkingBlockId(blockId);
    postToOpenElisServerFullResponse(
      "/rest/pathology/caseView/" +
        pathologySampleId +
        "/blocks/" +
        blockId +
        "/markEmbedded",
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
            .finally(() => setMarkingBlockId(null));
        } else {
          setMarkingBlockId(null);
        }
      },
    );
  };

  const stepState = (stepId) => {
    if (stepId === "collection") {
      return isCollected ? "completed" : "active";
    }
    if (stepId === "grossing") {
      if (!isCollected) {
        return "locked";
      }
      if (isGrossingDone) {
        return "completed";
      }
      if (isGrossingActive) {
        return "active";
      }
      return "locked";
    }
    if (stepId === "processing") {
      if (!isGrossingDone) {
        return "locked";
      }
      if (isProcessingDone) {
        return "completed";
      }
      if (isProcessingActive) {
        return "active";
      }
      return "locked";
    }
    if (stepId === "embedding") {
      if (!isProcessingDone) {
        return "locked";
      }
      if (isEmbeddingDone) {
        return "completed";
      }
      if (isEmbeddingActive) {
        return "active";
      }
      return "locked";
    }
    if (stepId === "microtomy" && isEmbeddingDone) {
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

  const savedBlocks = pathologySampleInfo?.blocks || [];
  const completedGrossingSummary = intl.formatMessage(
    { id: "pathology.workflow.grossingSummaryDone" },
    {
      count: savedBlocks.length,
    },
  );

  const processingStartedLabel = formatDateTime(
    pathologySampleInfo?.processingStartedAt,
  );
  const processingEstimateLabel = formatDateTime(
    pathologySampleInfo?.processingEstimatedComplete,
  );
  const completedProcessingSummary = intl.formatMessage(
    { id: "pathology.workflow.processingSummaryDone" },
    {
      when: processingStartedLabel || "—",
      count: savedBlocks.length,
    },
  );

  const embeddedCount = savedBlocks.filter((b) => !!b.embeddedAt).length;
  const completedEmbeddingSummary = intl.formatMessage(
    { id: "pathology.workflow.embeddingSummaryDone" },
    {
      embedded: embeddedCount,
      total: savedBlocks.length,
    },
  );

  const badgeLabelId = !isCollected
    ? "pathology.workflow.badgeCollection"
    : !isGrossingDone
      ? "pathology.workflow.badgeGrossing"
      : !isProcessingDone
        ? "pathology.workflow.badgeProcessing"
        : isEmbeddingDone
          ? "pathology.workflow.badgeMicrotomy"
          : "pathology.workflow.badgeEmbedding";

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
          disabled={!labNo}
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

  const renderCassetteRow = (code, index, { removable, onRemove }) => (
    <div key={code + "-" + index} className="pathology-cassette-row">
      <div className="pathology-cassette-code">{code}</div>
      <div className="pathology-cassette-row-actions">
        <button
          type="button"
          className="pathology-btn pathology-btn--ghost pathology-btn--sm"
          onClick={() => printCassetteLabel(code)}
        >
          <FormattedMessage id="pathology.workflow.printCassetteLabel" />
        </button>
        {removable && (
          <button
            type="button"
            className="pathology-btn pathology-btn--ghost pathology-btn--sm"
            onClick={onRemove}
          >
            <FormattedMessage id="pathology.workflow.removeCassette" />
          </button>
        )}
      </div>
    </div>
  );

  const renderGrossingCard = ({ editable }) => {
    const rows = editable
      ? Array.from({ length: cassetteCount }, (_, index) =>
          renderCassetteRow(cassetteCode(labNo, index), index, {
            removable: true,
            onRemove: () => removeCassette(index),
          }),
        )
      : savedBlocks.map((block, index) => {
          const code = blockDisplayCode(block, labNo, index);
          return renderCassetteRow(code, index, { removable: false });
        });

    return (
      <div
        className={
          "pathology-grossing-card" +
          (editable ? "" : " pathology-grossing-card--compact")
        }
      >
        <label className="pathology-grossing-label" htmlFor="pathology-gross-exam">
          <FormattedMessage id="pathology.workflow.macroDescription" />
        </label>
        {editable ? (
          <textarea
            id="pathology-gross-exam"
            className="pathology-grossing-textarea"
            rows={4}
            value={grossExamDraft}
            onChange={(e) => setGrossExamDraft(e.target.value)}
            placeholder={intl.formatMessage({
              id: "pathology.workflow.macroDescriptionHint",
            })}
          />
        ) : (
          <div className="pathology-grossing-readonly">
            {pathologySampleInfo?.grossExam?.trim()
              ? pathologySampleInfo.grossExam
              : "—"}
          </div>
        )}

        <div className="pathology-grossing-cassettes-header">
          <span className="pathology-grossing-label">
            <FormattedMessage id="pathology.workflow.cassetteList" />
          </span>
          {editable && (
            <button
              type="button"
              className="pathology-btn pathology-btn--ghost pathology-btn--sm"
              onClick={addCassette}
            >
              <FormattedMessage id="pathology.workflow.addCassette" />
            </button>
          )}
        </div>

        {rows.length === 0 ? (
          <div className="pathology-cassette-empty">
            <FormattedMessage id="pathology.workflow.noCassettesYet" />
          </div>
        ) : (
          <div className="pathology-cassette-list">{rows}</div>
        )}

        {editable && (
          <div className="pathology-grossing-footer">
            <button
              type="button"
              className="pathology-btn pathology-btn--primary"
              disabled={sending || cassetteCount < 1}
              onClick={sendToProcessing}
            >
              <FormattedMessage id="pathology.workflow.sendToProcessing" />
            </button>
          </div>
        )}
      </div>
    );
  };

  const renderProcessingCard = ({ showComplete }) => (
    <div
      className={
        "pathology-processing-card" +
        (showComplete ? "" : " pathology-processing-card--compact")
      }
    >
      <div className="pathology-processing-status">
        <div className="pathology-grossing-label">
          <FormattedMessage id="pathology.workflow.processingStatus" />
        </div>
        <div className="pathology-processing-status-line">
          <FormattedMessage
            id="pathology.workflow.processingStatusLine"
            values={{
              started: processingStartedLabel || "—",
              estimated: processingEstimateLabel || "—",
            }}
          />
        </div>
      </div>

      <div className="pathology-grossing-cassettes-header">
        <span className="pathology-grossing-label">
          <FormattedMessage id="pathology.workflow.cassetteList" />
        </span>
      </div>
      {savedBlocks.length === 0 ? (
        <div className="pathology-cassette-empty">
          <FormattedMessage id="pathology.workflow.noCassettesYet" />
        </div>
      ) : (
        <div className="pathology-cassette-list">
          {savedBlocks.map((block, index) =>
            renderCassetteRow(blockDisplayCode(block, labNo, index), index, {
              removable: false,
            }),
          )}
        </div>
      )}

      {showComplete && (
        <div className="pathology-grossing-footer">
          <button
            type="button"
            className="pathology-btn pathology-btn--primary"
            disabled={markingComplete}
            onClick={markProcessingComplete}
          >
            <FormattedMessage id="pathology.workflow.markProcessingComplete" />
          </button>
        </div>
      )}
    </div>
  );

  const renderEmbeddingCard = ({ allowMark }) => (
    <div
      className={
        "pathology-embedding-card" +
        (allowMark ? "" : " pathology-embedding-card--compact")
      }
    >
      <div className="pathology-grossing-cassettes-header">
        <span className="pathology-grossing-label">
          <FormattedMessage id="pathology.workflow.embeddingChecklist" />
        </span>
        <span className="pathology-embedding-progress">
          <FormattedMessage
            id="pathology.workflow.embeddingProgress"
            values={{ embedded: embeddedCount, total: savedBlocks.length }}
          />
        </span>
      </div>
      {savedBlocks.length === 0 ? (
        <div className="pathology-cassette-empty">
          <FormattedMessage id="pathology.workflow.noCassettesYet" />
        </div>
      ) : (
        <div className="pathology-cassette-list">
          {savedBlocks.map((block, index) => {
            const code = blockDisplayCode(block, labNo, index);
            const isEmbedded = !!block.embeddedAt;
            return (
              <div
                key={block.id || code + "-" + index}
                className={
                  "pathology-cassette-row" +
                  (isEmbedded ? " pathology-cassette-row--done" : "")
                }
              >
                <div className="pathology-cassette-row-main">
                  <div className="pathology-cassette-code">{code}</div>
                  {isEmbedded && (
                    <div className="pathology-cassette-embedded-meta">
                      <FormattedMessage
                        id="pathology.workflow.embeddedAt"
                        values={{
                          when: formatDateTime(block.embeddedAt) || "—",
                        }}
                      />
                    </div>
                  )}
                </div>
                <div className="pathology-cassette-row-actions">
                  <button
                    type="button"
                    className="pathology-btn pathology-btn--ghost pathology-btn--sm"
                    onClick={() => printCassetteLabel(code)}
                  >
                    <FormattedMessage id="pathology.workflow.printCassetteLabel" />
                  </button>
                  {allowMark && !isEmbedded && (
                    <button
                      type="button"
                      className="pathology-btn pathology-btn--primary pathology-btn--sm"
                      disabled={markingBlockId === block.id || !block.id}
                      onClick={() => markBlockEmbedded(block.id)}
                    >
                      <FormattedMessage id="pathology.workflow.markEmbedded" />
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
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
            {[labNo, requester !== "—" ? requester : null, pathologist]
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
          <FormattedMessage id={badgeLabelId} />
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

                {state === "completed" && step.id === "grossing" && (
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
                          {completedGrossingSummary}
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
                      renderGrossingCard({ editable: false })}
                  </div>
                )}

                {state === "completed" && step.id === "processing" && (
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
                          {completedProcessingSummary}
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
                      renderProcessingCard({ showComplete: false })}
                  </div>
                )}

                {state === "completed" && step.id === "embedding" && (
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
                          {completedEmbeddingSummary}
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
                      renderEmbeddingCard({ allowMark: false })}
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

                {state === "active" && step.id === "grossing" && (
                  <div className="pathology-rail-panel">
                    <h3 className="pathology-rail-step-title">{label}</h3>
                    <p className="pathology-rail-step-copy">
                      <FormattedMessage id="pathology.workflow.grossingIntro" />
                    </p>
                    {renderGrossingCard({ editable: true })}
                  </div>
                )}

                {state === "active" && step.id === "processing" && (
                  <div className="pathology-rail-panel">
                    <h3 className="pathology-rail-step-title">{label}</h3>
                    <p className="pathology-rail-step-copy">
                      <FormattedMessage id="pathology.workflow.processingIntro" />
                    </p>
                    {renderProcessingCard({ showComplete: true })}
                  </div>
                )}

                {state === "active" && step.id === "embedding" && (
                  <div className="pathology-rail-panel">
                    <h3 className="pathology-rail-step-title">{label}</h3>
                    <p className="pathology-rail-step-copy">
                      <FormattedMessage id="pathology.workflow.embeddingIntro" />
                    </p>
                    {renderEmbeddingCard({ allowMark: true })}
                  </div>
                )}

                {state === "active" &&
                  step.id !== "collection" &&
                  step.id !== "grossing" &&
                  step.id !== "processing" &&
                  step.id !== "embedding" && (
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
