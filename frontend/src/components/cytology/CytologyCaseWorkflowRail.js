import React, { useEffect, useMemo, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import config from "../../config.json";
import { postToOpenElisServerFullResponse } from "../utils/Utils";

/** Statuses at or past Collection completion. */
const PAST_COLLECTION = new Set([
  "CELL_BLOCK",
  "STAINING",
  "SCREENING",
  "READY_FOR_CYTOPATHOLOGIST",
  "COMPLETED",
  "REJECTED",
]);

/** Statuses at or past Cell block completion (Fluid only). */
const PAST_CELL_BLOCK = new Set([
  "STAINING",
  "SCREENING",
  "READY_FOR_CYTOPATHOLOGIST",
  "COMPLETED",
]);

/** Statuses at or past Staining completion. */
const PAST_STAINING = new Set(["READY_FOR_CYTOPATHOLOGIST", "COMPLETED"]);

const PAST_READ = new Set(["COMPLETED"]);

const COLLECTION_ACTIVE = new Set(["RECEIVED", "PREPARING_SLIDES"]);
const STAINING_ACTIVE = new Set(["STAINING", "SCREENING"]);

const slideDisplayCode = (slide, labNo) => {
  if (slide?.location && labNo) {
    return labNo + "." + slide.location;
  }
  if (slide?.location) {
    return slide.location;
  }
  return slide?.slideNumber != null ? String(slide.slideNumber) : "—";
};

/**
 * Compact vertical progress rail for cytopathology Steps:
 * Collection → [Cell block if Fluid] → Staining → The read & report.
 * Reuses pathology rail CSS classes from PathologyDashboard.css.
 */
function CytologyCaseWorkflowRail({
  cytologySampleId,
  pathologySampleInfo,
  onCaseUpdated,
}) {
  const intl = useIntl();
  const [confirming, setConfirming] = useState(false);
  const [rejecting, setRejecting] = useState(false);
  const [cellBlockStepBusy, setCellBlockStepBusy] = useState(null);
  const [stainingSlideId, setStainingSlideId] = useState(null);
  const [addingSmear, setAddingSmear] = useState(false);
  const [savingDraft, setSavingDraft] = useState(false);
  const [signingOut, setSigningOut] = useState(false);
  const [actionBusy, setActionBusy] = useState(null);
  const [expandedCompleted, setExpandedCompleted] = useState({});

  const [collectionSite, setCollectionSite] = useState("");
  const [collectionNotes, setCollectionNotes] = useState("");
  const [radiologyReference, setRadiologyReference] = useState("");
  const [roseAdequate, setRoseAdequate] = useState(false);
  const [lastMenstrualPeriod, setLastMenstrualPeriod] = useState("");
  const [previousPapResult, setPreviousPapResult] = useState("");
  const [fixationMethod, setFixationMethod] = useState("");
  const [fluidVolume, setFluidVolume] = useState("");
  const [fluidClarity, setFluidClarity] = useState("");
  const [rejectionReason, setRejectionReason] = useState("");

  const [microscopyDraft, setMicroscopyDraft] = useState("");
  const [conclusionDraft, setConclusionDraft] = useState("");
  const [conclusionTextDraft, setConclusionTextDraft] = useState("");

  const status = pathologySampleInfo?.status;
  const subtype = pathologySampleInfo?.subtype || "FNAC";
  const isFluid = subtype === "FLUID";
  const isRejected = status === "REJECTED";

  const isCollectionDone = PAST_COLLECTION.has(status);
  const isCollectionActive = COLLECTION_ACTIVE.has(status) && !isRejected;
  const isCellBlockDone = PAST_CELL_BLOCK.has(status);
  const isCellBlockActive = isFluid && status === "CELL_BLOCK";
  const isStainingDone = PAST_STAINING.has(status);
  const isStainingActive = STAINING_ACTIVE.has(status);
  const isReadDone = PAST_READ.has(status);
  const isReadActive = status === "READY_FOR_CYTOPATHOLOGIST";
  const labNo = pathologySampleInfo?.labNumber;

  const workflowSteps = useMemo(() => {
    const steps = [
      { id: "collection", labelId: "cytology.workflow.collection" },
    ];
    if (isFluid) {
      steps.push({
        id: "cellBlock",
        labelId: "cytology.workflow.cellBlock",
      });
    }
    steps.push(
      { id: "staining", labelId: "cytology.workflow.staining" },
      { id: "review", labelId: "cytology.workflow.reviewAndReport" },
    );
    return steps;
  }, [isFluid]);

  useEffect(() => {
    if (isCollectionActive || isCollectionDone) {
      setCollectionSite(pathologySampleInfo?.collectionSite || "");
      setCollectionNotes(pathologySampleInfo?.collectionNotes || "");
      setRadiologyReference(pathologySampleInfo?.radiologyReference || "");
      setRoseAdequate(!!pathologySampleInfo?.roseAdequate);
      setLastMenstrualPeriod(pathologySampleInfo?.lastMenstrualPeriod || "");
      setPreviousPapResult(pathologySampleInfo?.previousPapResult || "");
      setFixationMethod(pathologySampleInfo?.fixationMethod || "");
      setFluidVolume(pathologySampleInfo?.fluidVolume || "");
      setFluidClarity(pathologySampleInfo?.fluidClarity || "");
      setRejectionReason(pathologySampleInfo?.rejectionReason || "");
    }
  }, [
    isCollectionActive,
    isCollectionDone,
    pathologySampleInfo?.collectionSite,
    pathologySampleInfo?.collectionNotes,
    pathologySampleInfo?.radiologyReference,
    pathologySampleInfo?.roseAdequate,
    pathologySampleInfo?.lastMenstrualPeriod,
    pathologySampleInfo?.previousPapResult,
    pathologySampleInfo?.fixationMethod,
    pathologySampleInfo?.fluidVolume,
    pathologySampleInfo?.fluidClarity,
    pathologySampleInfo?.rejectionReason,
    cytologySampleId,
  ]);

  useEffect(() => {
    if (isReadActive || isReadDone) {
      setMicroscopyDraft(pathologySampleInfo?.microscopyExam || "");
      setConclusionDraft(pathologySampleInfo?.conclusion || "");
      setConclusionTextDraft(pathologySampleInfo?.conclusionText || "");
    }
  }, [
    isReadActive,
    isReadDone,
    pathologySampleInfo?.microscopyExam,
    pathologySampleInfo?.conclusion,
    pathologySampleInfo?.conclusionText,
    cytologySampleId,
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

  const applyUpdatedCase = (response, clearBusy) => {
    if (response && response.ok) {
      response
        .json()
        .then((data) => {
          if (onCaseUpdated) {
            onCaseUpdated(data);
          }
        })
        .finally(clearBusy);
    } else {
      clearBusy();
    }
  };

  const printContainerLabel = () => {
    if (!labNo) {
      return;
    }
    // type=default prints order + specimen labels for the accession.
    // type=specimen requires labNo.itemNo (e.g. ACC.1) and crashes on bare accession.
    window.open(
      config.serverBaseUrl +
        "/LabelMakerServlet?labNo=" +
        encodeURIComponent(labNo) +
        "&type=default",
      "_blank",
    );
  };

  const buildCollectionPayload = () => {
    const body = {
      collectionSite,
      collectionNotes,
    };
    if (subtype === "IMAGE_GUIDED_FNAC") {
      body.radiologyReference = radiologyReference;
      body.roseAdequate = roseAdequate;
    }
    if (subtype === "PAP_SMEAR") {
      body.lastMenstrualPeriod = lastMenstrualPeriod;
      body.previousPapResult = previousPapResult;
      body.fixationMethod = fixationMethod;
    }
    if (isFluid) {
      body.fluidVolume = fluidVolume;
      body.fluidClarity = fluidClarity;
    }
    return JSON.stringify(body);
  };

  const confirmCollection = () => {
    if (confirming || !isCollectionActive) {
      return;
    }
    setConfirming(true);
    postToOpenElisServerFullResponse(
      "/rest/cytology/caseView/" + cytologySampleId + "/confirmCollection",
      buildCollectionPayload(),
      (response) => applyUpdatedCase(response, () => setConfirming(false)),
    );
  };

  const rejectCollection = () => {
    if (rejecting || !isCollectionActive || !isFluid) {
      return;
    }
    setRejecting(true);
    postToOpenElisServerFullResponse(
      "/rest/cytology/caseView/" + cytologySampleId + "/rejectCollection",
      JSON.stringify({ rejectionReason }),
      (response) => applyUpdatedCase(response, () => setRejecting(false)),
    );
  };

  const markCellBlockStep = (step) => {
    if (!isCellBlockActive || cellBlockStepBusy) {
      return;
    }
    setCellBlockStepBusy(step);
    postToOpenElisServerFullResponse(
      "/rest/cytology/caseView/" +
        cytologySampleId +
        "/cellBlock/" +
        step,
      "{}",
      (response) =>
        applyUpdatedCase(response, () => setCellBlockStepBusy(null)),
    );
  };

  const markSlideStained = (slideId) => {
    if (!isStainingActive || !slideId || stainingSlideId) {
      return;
    }
    setStainingSlideId(slideId);
    postToOpenElisServerFullResponse(
      "/rest/cytology/caseView/" +
        cytologySampleId +
        "/slides/" +
        slideId +
        "/markStained",
      "{}",
      (response) =>
        applyUpdatedCase(response, () => setStainingSlideId(null)),
    );
  };

  const addSmearSlide = () => {
    if (addingSmear || !(isStainingActive || isCellBlockActive)) {
      return;
    }
    setAddingSmear(true);
    postToOpenElisServerFullResponse(
      "/rest/cytology/caseView/" + cytologySampleId + "/addSmearSlide",
      "{}",
      (response) => applyUpdatedCase(response, () => setAddingSmear(false)),
    );
  };

  const buildReadPayload = () =>
    JSON.stringify({
      microscopyExam: microscopyDraft,
      conclusion: conclusionDraft,
      conclusionText: conclusionTextDraft,
    });

  const saveReadDraft = () => {
    if (savingDraft || signingOut || !isReadActive) {
      return;
    }
    setSavingDraft(true);
    postToOpenElisServerFullResponse(
      "/rest/cytology/caseView/" + cytologySampleId + "/saveReadDraft",
      buildReadPayload(),
      (response) => applyUpdatedCase(response, () => setSavingDraft(false)),
    );
  };

  const signOutCase = () => {
    if (signingOut || savingDraft || !isReadActive) {
      return;
    }
    setSigningOut(true);
    postToOpenElisServerFullResponse(
      "/rest/cytology/caseView/" + cytologySampleId + "/signOut",
      buildReadPayload(),
      (response) => applyUpdatedCase(response, () => setSigningOut(false)),
    );
  };

  const postReadAction = (path, busyKey) => {
    if (!isReadActive || actionBusy) {
      return;
    }
    setActionBusy(busyKey);
    postToOpenElisServerFullResponse(
      "/rest/cytology/caseView/" + cytologySampleId + "/" + path,
      "{}",
      (response) => applyUpdatedCase(response, () => setActionBusy(null)),
    );
  };

  const stepState = (stepId) => {
    if (stepId === "collection") {
      if (isCollectionDone) {
        return "completed";
      }
      if (isCollectionActive) {
        return "active";
      }
      return "locked";
    }
    if (stepId === "cellBlock") {
      if (!isCollectionDone || isRejected) {
        return "locked";
      }
      if (isCellBlockDone) {
        return "completed";
      }
      if (isCellBlockActive) {
        return "active";
      }
      return "locked";
    }
    if (stepId === "staining") {
      const stainingUnlocked = isFluid
        ? isCellBlockDone
        : isCollectionDone && !isRejected;
      if (!stainingUnlocked) {
        return "locked";
      }
      if (isStainingDone) {
        return "completed";
      }
      if (isStainingActive) {
        return "active";
      }
      return "locked";
    }
    if (stepId === "review") {
      if (!isStainingDone) {
        return "locked";
      }
      if (isReadDone) {
        return "completed";
      }
      if (isReadActive) {
        return "active";
      }
      return "locked";
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
  const requester = pathologySampleInfo?.requester || "—";
  const pathologist =
    pathologySampleInfo?.assignedCytoPathologist ||
    pathologySampleInfo?.assignedPathologist;
  const collectedAt = formatDateTime(
    pathologySampleInfo?.collectionConfirmedAt ||
      pathologySampleInfo?.collectionDate,
  );

  const savedSlides = pathologySampleInfo?.slides || [];
  const stainedCount = savedSlides.filter((s) => !!s.stainedAt).length;
  const allStained =
    savedSlides.length > 0 && stainedCount === savedSlides.length;

  const completedCollectionSummary = isRejected
    ? intl.formatMessage(
        { id: "cytology.workflow.collectionSummaryRejected" },
        {
          reason: pathologySampleInfo?.rejectionReason || "—",
        },
      )
    : intl.formatMessage(
        { id: "cytology.workflow.collectionSummaryDone" },
        {
          when: collectedAt || "—",
          site: pathologySampleInfo?.collectionSite || "—",
        },
      );

  const completedCellBlockSummary = intl.formatMessage({
    id: "cytology.workflow.cellBlockSummaryDone",
  });

  const completedStainingSummary = intl.formatMessage(
    { id: "cytology.workflow.stainingSummaryDone" },
    {
      stained: stainedCount,
      total: savedSlides.length,
    },
  );

  const completedReadSummary = intl.formatMessage({
    id: "cytology.workflow.readSummaryDone",
  });

  const badgeLabelId = isRejected
    ? "cytology.workflow.badgeRejected"
    : !isCollectionDone
      ? "cytology.workflow.badgeCollection"
      : isFluid && !isCellBlockDone
        ? "cytology.workflow.badgeCellBlock"
        : !isStainingDone
          ? "cytology.workflow.badgeStaining"
          : isReadDone
            ? "cytology.workflow.badgeCompleted"
            : "cytology.workflow.badgeReview";

  const renderField = (id, labelId, value, onChange, editable, multiline) => (
    <div key={id} className="pathology-grossing-field">
      <label className="pathology-grossing-label" htmlFor={id}>
        <FormattedMessage id={labelId} />
      </label>
      {editable ? (
        multiline ? (
          <textarea
            id={id}
            className="pathology-grossing-textarea"
            rows={2}
            value={value}
            onChange={(e) => onChange(e.target.value)}
          />
        ) : (
          <input
            id={id}
            className="pathology-grossing-textarea"
            style={{ minHeight: "2rem", resize: "none" }}
            value={value}
            onChange={(e) => onChange(e.target.value)}
          />
        )
      ) : (
        <div className="pathology-grossing-readonly">{value?.trim() ? value : "—"}</div>
      )}
    </div>
  );

  const renderCollectionCard = ({ editable }) => (
    <div
      className={
        "pathology-grossing-card" +
        (editable ? "" : " pathology-grossing-card--compact")
      }
    >
      <div className="pathology-container-card-actions" style={{ marginBottom: "0.75rem" }}>
        <button
          type="button"
          className="pathology-btn pathology-btn--ghost"
          onClick={printContainerLabel}
          disabled={!labNo}
        >
          <FormattedMessage id="cytology.workflow.printContainerLabel" />
        </button>
      </div>

      {renderField(
        "cyto-collection-site",
        "cytology.workflow.collectionSite",
        editable ? collectionSite : pathologySampleInfo?.collectionSite || "",
        setCollectionSite,
        editable,
        false,
      )}
      {renderField(
        "cyto-collection-notes",
        "cytology.workflow.collectionNotes",
        editable ? collectionNotes : pathologySampleInfo?.collectionNotes || "",
        setCollectionNotes,
        editable,
        true,
      )}

      {subtype === "IMAGE_GUIDED_FNAC" && (
        <>
          {renderField(
            "cyto-radiology-ref",
            "cytology.workflow.radiologyReference",
            editable
              ? radiologyReference
              : pathologySampleInfo?.radiologyReference || "",
            setRadiologyReference,
            editable,
            false,
          )}
          <label className="pathology-conclusion-option">
            <input
              type="checkbox"
              checked={
                editable ? roseAdequate : !!pathologySampleInfo?.roseAdequate
              }
              disabled={!editable}
              onChange={(e) => setRoseAdequate(e.target.checked)}
            />
            <span>
              <FormattedMessage id="cytology.workflow.roseAdequate" />
            </span>
          </label>
        </>
      )}

      {subtype === "PAP_SMEAR" && (
        <>
          {renderField(
            "cyto-lmp",
            "cytology.workflow.lastMenstrualPeriod",
            editable
              ? lastMenstrualPeriod
              : pathologySampleInfo?.lastMenstrualPeriod || "",
            setLastMenstrualPeriod,
            editable,
            false,
          )}
          {renderField(
            "cyto-prev-pap",
            "cytology.workflow.previousPapResult",
            editable
              ? previousPapResult
              : pathologySampleInfo?.previousPapResult || "",
            setPreviousPapResult,
            editable,
            false,
          )}
          {renderField(
            "cyto-fixation",
            "cytology.workflow.fixationMethod",
            editable
              ? fixationMethod
              : pathologySampleInfo?.fixationMethod || "",
            setFixationMethod,
            editable,
            false,
          )}
        </>
      )}

      {isFluid && (
        <>
          {renderField(
            "cyto-fluid-volume",
            "cytology.workflow.fluidVolume",
            editable ? fluidVolume : pathologySampleInfo?.fluidVolume || "",
            setFluidVolume,
            editable,
            false,
          )}
          {renderField(
            "cyto-fluid-clarity",
            "cytology.workflow.fluidClarity",
            editable ? fluidClarity : pathologySampleInfo?.fluidClarity || "",
            setFluidClarity,
            editable,
            false,
          )}
        </>
      )}

      {isRejected && (
        <div className="pathology-grossing-readonly" style={{ marginTop: "0.5rem" }}>
          <FormattedMessage
            id="cytology.workflow.rejectedReason"
            values={{
              reason: pathologySampleInfo?.rejectionReason || "—",
            }}
          />
        </div>
      )}

      {editable && (
        <>
          {isFluid && (
            <div className="pathology-collection-reject-block">
              {renderField(
                "cyto-rejection-reason",
                "cytology.workflow.rejectionReason",
                rejectionReason,
                setRejectionReason,
                true,
                false,
              )}
            </div>
          )}
          <div className="pathology-grossing-footer pathology-collection-actions">
            {isFluid && (
              <button
                type="button"
                className="pathology-btn pathology-btn--danger"
                disabled={rejecting || confirming}
                onClick={rejectCollection}
              >
                <FormattedMessage id="cytology.workflow.rejectCollection" />
              </button>
            )}
            <button
              type="button"
              className="pathology-btn pathology-btn--primary"
              disabled={confirming || rejecting}
              onClick={confirmCollection}
            >
              <FormattedMessage id="cytology.workflow.confirmCollection" />
            </button>
          </div>
        </>
      )}
    </div>
  );

  const cellBlockSteps = [
    {
      id: "centrifuge",
      labelId: "cytology.workflow.cellBlockCentrifuge",
      at: pathologySampleInfo?.cellBlockCentrifugedAt,
      enabled: true,
    },
    {
      id: "prepare",
      labelId: "cytology.workflow.cellBlockPrepare",
      at: pathologySampleInfo?.cellBlockPreparedAt,
      enabled: !!pathologySampleInfo?.cellBlockCentrifugedAt,
    },
    {
      id: "slide",
      labelId: "cytology.workflow.cellBlockSlide",
      at: pathologySampleInfo?.cellBlockSlideAt,
      enabled: !!pathologySampleInfo?.cellBlockPreparedAt,
    },
  ];

  const renderCellBlockCard = ({ allowMark }) => (
    <div
      className={
        "pathology-processing-card" +
        (allowMark ? "" : " pathology-processing-card--compact")
      }
    >
      <div className="pathology-cassette-list">
        {cellBlockSteps.map((step) => {
          const done = !!step.at;
          return (
            <div
              key={step.id}
              className={
                "pathology-cassette-row" +
                (done ? " pathology-cassette-row--done" : "")
              }
            >
              <div className="pathology-cassette-row-main">
                <div className="pathology-cassette-code">
                  <FormattedMessage id={step.labelId} />
                </div>
                {done && (
                  <div className="pathology-cassette-embedded-meta">
                    {formatDateTime(step.at) || "—"}
                  </div>
                )}
              </div>
              <div className="pathology-cassette-row-actions">
                {allowMark && !done && (
                  <button
                    type="button"
                    className="pathology-btn pathology-btn--primary pathology-btn--sm"
                    disabled={
                      !step.enabled || cellBlockStepBusy === step.id
                    }
                    onClick={() => markCellBlockStep(step.id)}
                  >
                    <FormattedMessage id="cytology.workflow.markStepDone" />
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );

  const renderStainingCard = ({ allowMark }) => (
    <div
      className={
        "pathology-staining-card" +
        (allowMark ? "" : " pathology-staining-card--compact")
      }
    >
      <div className="pathology-grossing-cassettes-header">
        <span className="pathology-grossing-label">
          <FormattedMessage id="cytology.workflow.stainingList" />
        </span>
        <span className="pathology-embedding-progress">
          <FormattedMessage
            id="cytology.workflow.stainingProgress"
            values={{ stained: stainedCount, total: savedSlides.length }}
          />
        </span>
      </div>
      {savedSlides.length === 0 ? (
        <div className="pathology-cassette-empty">
          <FormattedMessage id="cytology.workflow.noSlidesToStain" />
        </div>
      ) : (
        <div className="pathology-cassette-list">
          {savedSlides.map((slide) => {
            const code = slideDisplayCode(slide, labNo);
            const isStained = !!slide.stainedAt;
            return (
              <div
                key={slide.id || code}
                className={
                  "pathology-cassette-row" +
                  (isStained ? " pathology-cassette-row--done" : "")
                }
              >
                <div className="pathology-cassette-row-main">
                  <div className="pathology-cassette-code">{code}</div>
                  {slide.slideType && (
                    <div className="pathology-stain-type">{slide.slideType}</div>
                  )}
                  {isStained && (
                    <div className="pathology-cassette-embedded-meta">
                      <FormattedMessage
                        id="cytology.workflow.stainedAt"
                        values={{
                          when: formatDateTime(slide.stainedAt) || "—",
                        }}
                      />
                    </div>
                  )}
                </div>
                <div className="pathology-cassette-row-actions">
                  {allowMark && !isStained && (
                    <button
                      type="button"
                      className="pathology-btn pathology-btn--primary pathology-btn--sm"
                      disabled={!!stainingSlideId || !slide.id}
                      onClick={() => markSlideStained(slide.id)}
                    >
                      <FormattedMessage id="cytology.workflow.markStained" />
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
      {allowMark && (
        <div className="pathology-grossing-footer">
          <button
            type="button"
            className="pathology-btn pathology-btn--ghost"
            disabled={addingSmear}
            onClick={addSmearSlide}
          >
            <FormattedMessage id="cytology.workflow.addSmear" />
          </button>
          {allStained && (
            <span className="pathology-embedding-progress">
              <FormattedMessage id="cytology.workflow.stainingCompleteHint" />
            </span>
          )}
        </div>
      )}
    </div>
  );

  const renderReadCard = ({ editable }) => (
    <div
      className={
        "pathology-read-card" + (editable ? "" : " pathology-read-card--compact")
      }
    >
      <label className="pathology-grossing-label" htmlFor="cyto-microscopy">
        <FormattedMessage id="cytology.workflow.microscopicFindings" />
      </label>
      {editable ? (
        <textarea
          id="cyto-microscopy"
          className="pathology-grossing-textarea"
          rows={4}
          value={microscopyDraft}
          onChange={(e) => setMicroscopyDraft(e.target.value)}
          placeholder={intl.formatMessage({
            id: "cytology.workflow.microscopicFindingsHint",
          })}
        />
      ) : (
        <div className="pathology-grossing-readonly">
          {pathologySampleInfo?.microscopyExam?.trim()
            ? pathologySampleInfo.microscopyExam
            : "—"}
        </div>
      )}

      <label className="pathology-grossing-label" htmlFor="cyto-conclusion">
        <FormattedMessage id="cytology.workflow.conclusion" />
      </label>
      {editable ? (
        <input
          id="cyto-conclusion"
          className="pathology-grossing-textarea"
          style={{ minHeight: "2rem", resize: "none" }}
          value={conclusionDraft}
          onChange={(e) => setConclusionDraft(e.target.value)}
          placeholder={intl.formatMessage({
            id: "cytology.workflow.conclusionHint",
          })}
        />
      ) : (
        <div className="pathology-grossing-readonly">
          {pathologySampleInfo?.conclusion?.trim()
            ? pathologySampleInfo.conclusion
            : "—"}
        </div>
      )}

      <label
        className="pathology-grossing-label"
        htmlFor="cyto-conclusion-text"
      >
        <FormattedMessage id="cytology.workflow.conclusionText" />
      </label>
      {editable ? (
        <textarea
          id="cyto-conclusion-text"
          className="pathology-grossing-textarea"
          rows={3}
          value={conclusionTextDraft}
          onChange={(e) => setConclusionTextDraft(e.target.value)}
          placeholder={intl.formatMessage({
            id: "cytology.workflow.conclusionTextHint",
          })}
        />
      ) : (
        <div className="pathology-grossing-readonly">
          {pathologySampleInfo?.conclusionText?.trim()
            ? pathologySampleInfo.conclusionText
            : "—"}
        </div>
      )}

      {editable ? (
        <>
          <div className="pathology-read-footer">
            <button
              type="button"
              className="pathology-btn pathology-btn--ghost"
              disabled={savingDraft || signingOut}
              onClick={saveReadDraft}
            >
              <FormattedMessage id="cytology.workflow.saveDraft" />
            </button>
            <button
              type="button"
              className="pathology-btn pathology-btn--primary"
              disabled={signingOut || savingDraft}
              onClick={signOutCase}
            >
              <FormattedMessage id="cytology.workflow.signOutFinalize" />
            </button>
          </div>
          <div className="pathology-read-footer">
            <button
              type="button"
              className="pathology-btn pathology-btn--ghost pathology-btn--sm"
              disabled={!!actionBusy}
              onClick={() => postReadAction("requestRepeat", "repeat")}
            >
              <FormattedMessage id="cytology.workflow.requestRepeat" />
              {pathologySampleInfo?.repeatRequestedAt && " ✓"}
            </button>
            <button
              type="button"
              className="pathology-btn pathology-btn--ghost pathology-btn--sm"
              disabled={!!actionBusy}
              onClick={() =>
                postReadAction("requestSecondOpinion", "secondOpinion")
              }
            >
              <FormattedMessage id="cytology.workflow.requestSecondOpinion" />
              {pathologySampleInfo?.secondOpinionRequestedAt && " ✓"}
            </button>
            {isFluid && (
              <button
                type="button"
                className="pathology-btn pathology-btn--ghost pathology-btn--sm"
                disabled={!!actionBusy}
                onClick={() => postReadAction("orderIhc", "ihc")}
              >
                <FormattedMessage id="cytology.workflow.orderIhc" />
                {pathologySampleInfo?.ihcOrderedAt && " ✓"}
              </button>
            )}
          </div>
        </>
      ) : (
        <div className="pathology-read-signed-note">
          <FormattedMessage
            id="cytology.workflow.signedOutNote"
            values={{ pathologist: pathologist || "—" }}
          />
        </div>
      )}
    </div>
  );

  const renderCompletedToggle = (stepId, label, summary, body) => {
    const isExpandedCompleted = !!expandedCompleted[stepId];
    return (
      <div className="pathology-rail-panel pathology-rail-panel--completed">
        <button
          type="button"
          className="pathology-rail-toggle"
          aria-expanded={isExpandedCompleted}
          onClick={() => toggleCompleted(stepId)}
        >
          <span className="pathology-rail-toggle-text">
            <span className="pathology-rail-collapsed-title">{label}</span>
            <span className="pathology-rail-collapsed-summary">
              {" — "}
              {summary}
            </span>
          </span>
          <span
            className={
              "pathology-rail-chevron" +
              (isExpandedCompleted ? " pathology-rail-chevron--open" : "")
            }
            aria-hidden="true"
          />
        </button>
        {isExpandedCompleted && body}
      </div>
    );
  };

  const renderActivePanel = (stepId, introId, body) => {
    const label = intl.formatMessage({
      id:
        workflowSteps.find((s) => s.id === stepId)?.labelId ||
        "cytology.workflow.collection",
    });
    return (
      <div className="pathology-rail-panel">
        <h3 className="pathology-rail-step-title">{label}</h3>
        <p className="pathology-rail-step-copy">
          <FormattedMessage id={introId} />
        </p>
        {body}
      </div>
    );
  };

  const renderLockedPanel = (label) => (
    <div className="pathology-rail-collapsed pathology-rail-collapsed--locked">
      <span className="pathology-rail-collapsed-title">{label}</span>
      <span className="pathology-rail-collapsed-summary">
        {" — "}
        <FormattedMessage id="cytology.workflow.notYetStarted" />
      </span>
    </div>
  );

  return (
    <div className="pathology-case-rail">
      <div className="pathology-case-rail-header">
        <div className="pathology-case-rail-patient">
          <div className="pathology-case-rail-name">
            {patientName || (
              <FormattedMessage id="cytology.workflow.case" />
            )}
          </div>
          <div className="pathology-case-rail-meta">
            {[labNo, subtype, requester !== "—" ? requester : null, pathologist]
              .filter(Boolean)
              .join(" · ")}
          </div>
        </div>
        <span
          className={
            "pathology-case-rail-badge" +
            (isCollectionDone && !isRejected
              ? " pathology-case-rail-badge--progress"
              : "")
          }
        >
          <FormattedMessage id={badgeLabelId} />
        </span>
      </div>

      <ol className="pathology-rail">
        {workflowSteps.map((step, index) => {
          const state = stepState(step.id);
          const stepNumber = index + 1;
          const label = intl.formatMessage({ id: step.labelId });
          const isLast = index === workflowSteps.length - 1;

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
                {state === "completed" &&
                  step.id === "collection" &&
                  renderCompletedToggle(
                    step.id,
                    label,
                    completedCollectionSummary,
                    renderCollectionCard({ editable: false }),
                  )}
                {state === "completed" &&
                  step.id === "cellBlock" &&
                  renderCompletedToggle(
                    step.id,
                    label,
                    completedCellBlockSummary,
                    renderCellBlockCard({ allowMark: false }),
                  )}
                {state === "completed" &&
                  step.id === "staining" &&
                  renderCompletedToggle(
                    step.id,
                    label,
                    completedStainingSummary,
                    renderStainingCard({ allowMark: false }),
                  )}
                {state === "completed" &&
                  step.id === "review" &&
                  renderCompletedToggle(
                    step.id,
                    label,
                    completedReadSummary,
                    renderReadCard({ editable: false }),
                  )}

                {state === "active" &&
                  step.id === "collection" &&
                  renderActivePanel(
                    step.id,
                    "cytology.workflow.collectionIntro",
                    renderCollectionCard({ editable: true }),
                  )}
                {state === "active" &&
                  step.id === "cellBlock" &&
                  renderActivePanel(
                    step.id,
                    "cytology.workflow.cellBlockIntro",
                    renderCellBlockCard({ allowMark: true }),
                  )}
                {state === "active" &&
                  step.id === "staining" &&
                  renderActivePanel(
                    step.id,
                    "cytology.workflow.stainingIntro",
                    renderStainingCard({ allowMark: true }),
                  )}
                {state === "active" &&
                  step.id === "review" &&
                  renderActivePanel(
                    step.id,
                    "cytology.workflow.reviewIntro",
                    renderReadCard({ editable: true }),
                  )}

                {state === "locked" && renderLockedPanel(label)}
              </div>
            </li>
          );
        })}
      </ol>
    </div>
  );
}

export default CytologyCaseWorkflowRail;
