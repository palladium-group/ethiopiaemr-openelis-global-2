import React, { useEffect, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import config from "../../config.json";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../utils/Utils";

const BIOPSY_WORKFLOW_STEPS = [
  { id: "collection", labelId: "pathology.workflow.collection" },
  { id: "grossing", labelId: "pathology.workflow.grossing" },
  { id: "processing", labelId: "pathology.workflow.processing" },
  { id: "embedding", labelId: "pathology.workflow.embedding" },
  { id: "microtomy", labelId: "pathology.workflow.microtomy" },
  { id: "staining", labelId: "pathology.workflow.staining" },
  /** Collapsed PDF Steps 9–10: The read + Sign-out & report. */
  { id: "review", labelId: "pathology.workflow.reviewAndReport" },
];

/** Frozen section skips paraffin processing/embedding — cryostat cut + rapid stain. */
const FROZEN_WORKFLOW_STEPS = [
  { id: "collection", labelId: "pathology.workflow.collection" },
  { id: "grossing", labelId: "pathology.workflow.grossing" },
  { id: "microtomy", labelId: "pathology.workflow.cryotomy" },
  { id: "staining", labelId: "pathology.workflow.rapidStaining" },
  { id: "review", labelId: "pathology.workflow.reviewAndReport" },
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

/** Statuses at or past Microtomy completion (SLICING → STAINING). */
const PAST_MICROTOMY = new Set([
  "STAINING",
  "READY_PATHOLOGIST",
  "ADDITIONAL_REQUEST",
  "COMPLETED",
]);

/** Statuses at or past Staining completion (STAINING → READY_PATHOLOGIST / The read). */
const PAST_STAINING = new Set([
  "READY_PATHOLOGIST",
  "ADDITIONAL_REQUEST",
  "COMPLETED",
]);

/** Case finalized (collapsed Steps 9–10 complete). */
const PAST_READ = new Set(["COMPLETED"]);

const PLANNED_SLIDES_PER_BLOCK = 1;

const DEFAULT_STAIN_TYPE = "H&E";

/**
 * Fallback special-stain catalog, used only when the seeded pathologist_requests displayList is
 * empty/unavailable. The live picker is dictionary-backed (see the PATHOLOGIST_REQUESTS fetch).
 */
const SPECIAL_STAIN_OPTIONS = [
  "PAS",
  "Ziehl-Neelsen",
  "Masson's Trichrome",
  "GMS",
  "Congo Red",
  "Perls Prussian Blue",
  "Reticulin",
  "Mucicarmine",
];

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
 * PDF-style vertical progress rail for Steps 3–10.
 * Completed steps are collapsed by default; user can expand to view details /
 * allowed actions (e.g. reprint). Collection through The read & report are interactive.
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
  const [cuttingBlockId, setCuttingBlockId] = useState(null);
  const [confirmingSlideId, setConfirmingSlideId] = useState(null);
  const [stainingSlideId, setStainingSlideId] = useState(null);
  const [savingDraft, setSavingDraft] = useState(false);
  const [signingOut, setSigningOut] = useState(false);
  /** Completed step ids the user has manually expanded. */
  const [expandedCompleted, setExpandedCompleted] = useState({});
  const [grossExamDraft, setGrossExamDraft] = useState("");
  /** Local draft cassette count (suffixes A1..An). Persisted only on send. */
  const [cassetteCount, setCassetteCount] = useState(0);
  const [microscopyDraft, setMicroscopyDraft] = useState("");
  const [conclusionTextDraft, setConclusionTextDraft] = useState("");
  const [selectedConclusionIds, setSelectedConclusionIds] = useState([]);
  const [conclusionOptions, setConclusionOptions] = useState([]);
  const [stainOptions, setStainOptions] = useState([]);
  const [selectedStains, setSelectedStains] = useState([]);
  const [requestingStain, setRequestingStain] = useState(false);
  const [startingStain, setStartingStain] = useState(false);

  const status = pathologySampleInfo?.status;
  const subtypeLabel = pathologySampleInfo?.subtype || "";
  const isFrozen =
    subtypeLabel === "Frozen section" ||
    subtypeLabel === "FROZEN" ||
    (typeof subtypeLabel === "string" &&
      subtypeLabel.toLowerCase().includes("frozen"));
  const workflowSteps = isFrozen ? FROZEN_WORKFLOW_STEPS : BIOPSY_WORKFLOW_STEPS;
  const isCollected = !!pathologySampleInfo?.collectionDate;
  const isGrossingDone = PAST_GROSSING.has(status);
  const isGrossingActive = isCollected && status === "GROSSING";
  const isProcessingDone = PAST_PROCESSING.has(status);
  const isProcessingActive = status === "PROCESSING";
  const isEmbeddingDone = PAST_EMBEDDING.has(status);
  const isEmbeddingActive = status === "EMBEDDING";
  const isMicrotomyDone = PAST_MICROTOMY.has(status);
  const isMicrotomyActive = status === "SLICING";
  const isStainingDone = PAST_STAINING.has(status);
  const isStainingActive = status === "STAINING";
  const isReadDone = PAST_READ.has(status);
  const isReadActive =
    status === "READY_PATHOLOGIST" || status === "ADDITIONAL_REQUEST";
  const isAdditionalRequest = status === "ADDITIONAL_REQUEST";
  const labNo = pathologySampleInfo?.labNumber;

  /** Read rounds (append-only history) and any outstanding special-stain request. */
  const reads = pathologySampleInfo?.reads || [];
  const openRequests = (pathologySampleInfo?.requests || []).filter(
    (r) => r.status === "OPENED",
  );
  /** Stain the tech is currently working (drives the label on newly cut slides). */
  const activeSpecialStain = openRequests.length
    ? openRequests
        .map((r) => r.value)
        .filter(Boolean)
        .join(", ")
    : null;

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

  useEffect(() => {
    if (isReadActive || isReadDone) {
      setMicroscopyDraft(pathologySampleInfo?.microscopyExam || "");
      setConclusionTextDraft(pathologySampleInfo?.conclusionText || "");
      setSelectedConclusionIds(
        (pathologySampleInfo?.conclusions || [])
          .map((c) => c.id)
          .filter(Boolean),
      );
    }
  }, [
    isReadActive,
    isReadDone,
    pathologySampleInfo?.microscopyExam,
    pathologySampleInfo?.conclusionText,
    pathologySampleInfo?.conclusions,
    pathologySampleId,
  ]);

  useEffect(() => {
    getFromOpenElisServer(
      "/rest/displayList/PATHOLOGIST_CONCLUSIONS",
      (list) => {
        setConclusionOptions(Array.isArray(list) ? list : []);
      },
    );
  }, []);

  // Special-stain catalog: the seeded pathologist_requests dictionary (Stain PAS, ZN, Trichrome …).
  useEffect(() => {
    getFromOpenElisServer("/rest/displayList/PATHOLOGIST_REQUESTS", (list) => {
      setStainOptions(Array.isArray(list) ? list : []);
    });
  }, []);

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

  const printSlideLabel = (code) => {
    if (!code) {
      return;
    }
    window.open(
      config.serverBaseUrl +
        "/LabelMakerServlet?labelType=slide&code=" +
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
    if (sending || !isGrossingActive) {
      return;
    }
    // Biopsy requires at least one cassette; Frozen creates a cryostat block server-side.
    if (!isFrozen && cassetteCount < 1) {
      return;
    }
    if (!grossExamDraft.trim()) {
      return;
    }
    const blocks =
      cassetteCount < 1
        ? []
        : Array.from({ length: cassetteCount }, (_, index) => ({
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
      "/rest/pathology/caseView/" +
        pathologySampleId +
        "/markProcessingComplete",
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

  const cutSlide = (blockId, onCreated, slideRole) => {
    if (!isMicrotomyActive || !blockId || cuttingBlockId) {
      return;
    }
    setCuttingBlockId(blockId);
    const params = [];
    if (activeSpecialStain) {
      params.push("stainType=" + encodeURIComponent(activeSpecialStain));
    }
    if (slideRole) {
      params.push("slideRole=" + encodeURIComponent(slideRole));
    }
    postToOpenElisServerFullResponse(
      "/rest/pathology/caseView/" +
        pathologySampleId +
        "/blocks/" +
        blockId +
        "/cutSlide" +
        (params.length ? "?" + params.join("&") : ""),
      "{}",
      (response) => {
        if (response && response.ok) {
          response
            .json()
            .then((data) => {
              if (onCaseUpdated) {
                onCaseUpdated(data);
              }
              if (onCreated) {
                onCreated(data);
              }
            })
            .finally(() => setCuttingBlockId(null));
        } else {
          setCuttingBlockId(null);
        }
      },
    );
  };

  const confirmSlide = (slideId) => {
    if (!isMicrotomyActive || !slideId || confirmingSlideId) {
      return;
    }
    setConfirmingSlideId(slideId);
    postToOpenElisServerFullResponse(
      "/rest/pathology/caseView/" +
        pathologySampleId +
        "/slides/" +
        slideId +
        "/confirm",
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
            .finally(() => setConfirmingSlideId(null));
        } else {
          setConfirmingSlideId(null);
        }
      },
    );
  };

  const markSlideStained = (slideId) => {
    if (!isStainingActive || !slideId || stainingSlideId) {
      return;
    }
    setStainingSlideId(slideId);
    postToOpenElisServerFullResponse(
      "/rest/pathology/caseView/" +
        pathologySampleId +
        "/slides/" +
        slideId +
        "/markStained",
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
            .finally(() => setStainingSlideId(null));
        } else {
          setStainingSlideId(null);
        }
      },
    );
  };

  const buildReadPayload = () =>
    JSON.stringify({
      microscopyExam: microscopyDraft,
      conclusionText: conclusionTextDraft,
      conclusions: selectedConclusionIds,
    });

  const saveReadDraft = () => {
    if (savingDraft || signingOut || !isReadActive) {
      return;
    }
    setSavingDraft(true);
    postToOpenElisServerFullResponse(
      "/rest/pathology/caseView/" + pathologySampleId + "/saveReadDraft",
      buildReadPayload(),
      (response) => {
        if (response && response.ok) {
          response
            .json()
            .then((data) => {
              if (onCaseUpdated) {
                onCaseUpdated(data);
              }
            })
            .finally(() => setSavingDraft(false));
        } else {
          setSavingDraft(false);
        }
      },
    );
  };

  const signOutCase = () => {
    if (signingOut || savingDraft || !isReadActive) {
      return;
    }
    setSigningOut(true);
    postToOpenElisServerFullResponse(
      "/rest/pathology/caseView/" + pathologySampleId + "/signOut",
      buildReadPayload(),
      (response) => {
        if (response && response.ok) {
          response
            .json()
            .then((data) => {
              if (onCaseUpdated) {
                onCaseUpdated(data);
              }
            })
            .finally(() => setSigningOut(false));
        } else {
          setSigningOut(false);
        }
      },
    );
  };

  const toggleConclusionId = (id) => {
    setSelectedConclusionIds((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id],
    );
  };

  const toggleStain = (name) => {
    setSelectedStains((prev) =>
      prev.includes(name) ? prev.filter((x) => x !== name) : [...prev, name],
    );
  };

  /** Pathologist asks for one or more special stains; keeps the case on the same accession. */
  const requestSpecialStain = () => {
    if (requestingStain || !isReadActive || selectedStains.length === 0) {
      return;
    }
    setRequestingStain(true);
    postToOpenElisServerFullResponse(
      "/rest/pathology/caseView/" + pathologySampleId + "/requestSpecialStain",
      JSON.stringify({
        microscopyExam: microscopyDraft,
        conclusionText: conclusionTextDraft,
        conclusions: selectedConclusionIds,
        specialStains: selectedStains,
      }),
      (response) => {
        if (response && response.ok) {
          response
            .json()
            .then((data) => {
              if (onCaseUpdated) {
                onCaseUpdated(data);
              }
              setSelectedStains([]);
            })
            .finally(() => setRequestingStain(false));
        } else {
          setRequestingStain(false);
        }
      },
    );
  };

  /** Tech picks up the request: ADDITIONAL_REQUEST → SLICING so a new section can be cut/stained. */
  const startSpecialStain = () => {
    if (startingStain || !isAdditionalRequest) {
      return;
    }
    setStartingStain(true);
    postToOpenElisServerFullResponse(
      "/rest/pathology/caseView/" + pathologySampleId + "/startSpecialStain",
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
            .finally(() => setStartingStain(false));
        } else {
          setStartingStain(false);
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
    if (stepId === "microtomy") {
      if (!isEmbeddingDone) {
        return "locked";
      }
      if (isMicrotomyDone) {
        return "completed";
      }
      if (isMicrotomyActive) {
        return "active";
      }
      return "locked";
    }
    if (stepId === "staining") {
      if (!isMicrotomyDone) {
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

  const patientName = [
    pathologySampleInfo?.firstName,
    pathologySampleInfo?.lastName,
  ]
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

  const savedSlides = pathologySampleInfo?.slides || [];
  const slidesForBlock = (blockId) =>
    savedSlides.filter((s) => s.pathologyBlockId === blockId);
  const confirmedSlideCount = savedSlides.filter(
    (s) => s.pathologyBlockId != null && !!s.confirmedAt,
  ).length;
  const linkedSlideCount = savedSlides.filter(
    (s) => s.pathologyBlockId != null,
  ).length;
  const completedMicrotomySummary = intl.formatMessage(
    { id: "pathology.workflow.microtomySummaryDone" },
    {
      confirmed: confirmedSlideCount,
      total: Math.max(
        linkedSlideCount,
        savedBlocks.length * PLANNED_SLIDES_PER_BLOCK,
      ),
    },
  );

  const slidesToStain = savedSlides.filter(
    (s) => s.pathologyBlockId != null && !!s.confirmedAt,
  );
  const stainedCount = slidesToStain.filter((s) => !!s.stainedAt).length;
  const completedStainingSummary = intl.formatMessage(
    { id: "pathology.workflow.stainingSummaryDone" },
    {
      stained: stainedCount,
      total: slidesToStain.length,
    },
  );

  const completedReadSummary = intl.formatMessage({
    id: "pathology.workflow.readSummaryDone",
  });

  const badgeLabelId = !isCollected
    ? "pathology.workflow.badgeCollection"
    : !isGrossingDone
      ? "pathology.workflow.badgeGrossing"
      : !isProcessingDone
        ? "pathology.workflow.badgeProcessing"
        : !isEmbeddingDone
          ? "pathology.workflow.badgeEmbedding"
          : !isMicrotomyDone
            ? "pathology.workflow.badgeMicrotomy"
            : !isStainingDone
              ? "pathology.workflow.badgeStaining"
              : isReadDone
                ? "pathology.workflow.badgeCompleted"
                : "pathology.workflow.badgeReview";

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
        <label
          className="pathology-grossing-label"
          htmlFor="pathology-gross-exam"
        >
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
            <FormattedMessage
              id={
                isFrozen
                  ? "pathology.workflow.frozenPortionOptional"
                  : "pathology.workflow.cassetteList"
              }
            />
          </span>
          {editable && !isFrozen && (
            <button
              type="button"
              className="pathology-btn pathology-btn--ghost pathology-btn--sm"
              onClick={addCassette}
            >
              <FormattedMessage id="pathology.workflow.addCassette" />
            </button>
          )}
        </div>

        {!isFrozen && (rows.length === 0 ? (
          <div className="pathology-cassette-empty">
            <FormattedMessage id="pathology.workflow.noCassettesYet" />
          </div>
        ) : (
          <div className="pathology-cassette-list">{rows}</div>
        ))}

        {editable && (
          <div className="pathology-grossing-footer">
            <button
              type="button"
              className="pathology-btn pathology-btn--primary"
              disabled={
                sending ||
                (!isFrozen && cassetteCount < 1) ||
                !grossExamDraft.trim()
              }
              onClick={sendToProcessing}
            >
              <FormattedMessage
                id={
                  isFrozen
                    ? "pathology.workflow.sendToCryotomy"
                    : "pathology.workflow.sendToProcessing"
                }
              />
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

  const handleCutSlide = (block, blockIndex) => {
    cutSlide(block.id, (updated) => {
      const blockSlides = (updated?.slides || []).filter(
        (s) => s.pathologyBlockId === block.id,
      );
      const newest = blockSlides.reduce((best, slide) => {
        if (!best) {
          return slide;
        }
        return (slide.slideNumber || 0) > (best.slideNumber || 0)
          ? slide
          : best;
      }, null);
      if (newest) {
        printSlideLabel(slideDisplayCode(newest, updated?.labNumber || labNo));
      }
    });
  };

  const renderMicrotomyCard = ({ allowActions }) => (
    <div
      className={
        "pathology-microtomy-card" +
        (allowActions ? "" : " pathology-microtomy-card--compact")
      }
    >
      {savedBlocks.length === 0 ? (
        <div className="pathology-cassette-empty">
          <FormattedMessage id="pathology.workflow.noCassettesYet" />
        </div>
      ) : (
        <div className="pathology-microtomy-blocks">
          {savedBlocks.map((block, blockIndex) => {
            const blockCode = blockDisplayCode(block, labNo, blockIndex);
            const blockSlides = slidesForBlock(block.id);
            const confirmed = blockSlides.filter((s) => !!s.confirmedAt).length;
            const planned = Math.max(
              PLANNED_SLIDES_PER_BLOCK,
              blockSlides.length,
            );
            return (
              <div
                key={block.id || blockCode}
                className="pathology-microtomy-block"
              >
                <div className="pathology-microtomy-block-header">
                  <div>
                    <div className="pathology-cassette-code">{blockCode}</div>
                    <div className="pathology-microtomy-block-progress">
                      <FormattedMessage
                        id="pathology.workflow.microtomyBlockProgress"
                        values={{ confirmed, planned }}
                      />
                    </div>
                  </div>
                  {allowActions && (
                    <div className="pathology-microtomy-block-actions">
                      <button
                        type="button"
                        className="pathology-btn pathology-btn--ghost pathology-btn--sm"
                        disabled={cuttingBlockId === block.id || !block.id}
                        onClick={() => handleCutSlide(block, blockIndex)}
                      >
                        <FormattedMessage id="pathology.workflow.cutSlide" />
                      </button>
                      {activeSpecialStain && (
                        <button
                          type="button"
                          className="pathology-btn pathology-btn--ghost pathology-btn--sm"
                          disabled={cuttingBlockId === block.id || !block.id}
                          onClick={() =>
                            cutSlide(block.id, null, "CONTROL_POS")
                          }
                        >
                          <FormattedMessage id="pathology.workflow.addControlSlide" />
                        </button>
                      )}
                    </div>
                  )}
                </div>
                {blockSlides.length === 0 ? (
                  <div className="pathology-cassette-empty">
                    <FormattedMessage id="pathology.workflow.noSlidesYet" />
                  </div>
                ) : (
                  <div className="pathology-cassette-list">
                    {blockSlides.map((slide) => {
                      const code = slideDisplayCode(slide, labNo);
                      const isConfirmed = !!slide.confirmedAt;
                      return (
                        <div
                          key={slide.id || code}
                          className={
                            "pathology-cassette-row" +
                            (isConfirmed ? " pathology-cassette-row--done" : "")
                          }
                        >
                          <div className="pathology-cassette-row-main">
                            <div className="pathology-cassette-code">
                              {code}
                            </div>
                            {isConfirmed && (
                              <div className="pathology-cassette-embedded-meta">
                                <FormattedMessage
                                  id="pathology.workflow.slideConfirmedAt"
                                  values={{
                                    when:
                                      formatDateTime(slide.confirmedAt) || "—",
                                  }}
                                />
                              </div>
                            )}
                          </div>
                          <div className="pathology-cassette-row-actions">
                            <button
                              type="button"
                              className="pathology-btn pathology-btn--ghost pathology-btn--sm"
                              onClick={() => printSlideLabel(code)}
                            >
                              <FormattedMessage id="pathology.workflow.printSlideLabel" />
                            </button>
                            {allowActions && !isConfirmed && (
                              <button
                                type="button"
                                className="pathology-btn pathology-btn--primary pathology-btn--sm"
                                disabled={
                                  confirmingSlideId === slide.id || !slide.id
                                }
                                onClick={() => confirmSlide(slide.id)}
                              >
                                <FormattedMessage id="pathology.workflow.confirmSlide" />
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
          })}
        </div>
      )}
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
          <FormattedMessage id="pathology.workflow.stainingList" />
        </span>
        <span className="pathology-embedding-progress">
          <FormattedMessage
            id="pathology.workflow.stainingProgress"
            values={{ stained: stainedCount, total: slidesToStain.length }}
          />
        </span>
      </div>
      {slidesToStain.length === 0 ? (
        <div className="pathology-cassette-empty">
          <FormattedMessage id="pathology.workflow.noSlidesToStain" />
        </div>
      ) : (
        <div className="pathology-cassette-list">
          {slidesToStain.map((slide) => {
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
                  <div className="pathology-stain-type">
                    <FormattedMessage
                      id="pathology.workflow.stainType"
                      values={{ type: slide.stainType || DEFAULT_STAIN_TYPE }}
                    />
                    {slide.slideRole && slide.slideRole !== "PATIENT" && (
                      <span className="pathology-stain-control">
                        {" · "}
                        <FormattedMessage id="pathology.workflow.controlSlide" />
                      </span>
                    )}
                  </div>
                  {isStained && (
                    <div className="pathology-cassette-embedded-meta">
                      <FormattedMessage
                        id="pathology.workflow.stainedAt"
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
                      disabled={stainingSlideId === slide.id || !slide.id}
                      onClick={() => markSlideStained(slide.id)}
                    >
                      <FormattedMessage id="pathology.workflow.markStained" />
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

  const renderReadHistory = () => {
    const finalizedRounds = reads.filter((r) => r.finalized);
    if (finalizedRounds.length === 0) {
      return null;
    }
    return (
      <div className="pathology-read-history">
        <div className="pathology-grossing-label">
          <FormattedMessage id="pathology.workflow.readHistory" />
        </div>
        {finalizedRounds.map((round) => {
          const conclusionLine = [
            round.conclusionText,
            (round.conclusions || []).join(", "),
          ]
            .filter(Boolean)
            .join(" — ");
          return (
            <div key={round.roundNumber} className="pathology-read-round">
              <div className="pathology-read-round-header">
                <FormattedMessage
                  id="pathology.workflow.readRound"
                  values={{ round: round.roundNumber }}
                />
                {round.reviewedBy && (
                  <span className="pathology-read-round-meta">
                    {" — "}
                    <FormattedMessage
                      id="pathology.workflow.readRoundMeta"
                      values={{
                        by: round.reviewedBy,
                        when: formatDateTime(round.reviewedAt) || "—",
                      }}
                    />
                  </span>
                )}
              </div>
              {round.microscopyExam?.trim() && (
                <div className="pathology-read-round-body">
                  {round.microscopyExam}
                </div>
              )}
              {conclusionLine && (
                <div className="pathology-read-round-conclusion">
                  {conclusionLine}
                </div>
              )}
            </div>
          );
        })}
      </div>
    );
  };

  const renderSpecialStainSection = ({ editable }) => {
    if (!editable && openRequests.length === 0) {
      return null;
    }
    // Prefer the seeded catalog; fall back to the built-in list if it is not configured.
    const stainChoices = stainOptions.length
      ? stainOptions.map((o) => o.value).filter(Boolean)
      : SPECIAL_STAIN_OPTIONS;
    return (
      <div className="pathology-special-stain">
        {openRequests.length > 0 && (
          <div className="pathology-special-stain-pending">
            <span>
              <FormattedMessage
                id="pathology.workflow.specialStainRequested"
                values={{
                  stain: openRequests
                    .map((r) => r.value)
                    .filter(Boolean)
                    .join(", "),
                }}
              />
            </span>
            {isAdditionalRequest && (
              <button
                type="button"
                className="pathology-btn pathology-btn--primary pathology-btn--sm"
                disabled={startingStain}
                onClick={startSpecialStain}
              >
                <FormattedMessage id="pathology.workflow.startSpecialStain" />
              </button>
            )}
          </div>
        )}
        {editable && !isFrozen && (
          <>
            <div className="pathology-grossing-label">
              <FormattedMessage id="pathology.workflow.requestSpecialStainLabel" />
            </div>
            <div className="pathology-conclusion-options">
              {stainChoices.map((name) => (
                <label key={name} className="pathology-conclusion-option">
                  <input
                    type="checkbox"
                    checked={selectedStains.includes(name)}
                    onChange={() => toggleStain(name)}
                  />
                  <span>{name}</span>
                </label>
              ))}
            </div>
            <div className="pathology-read-footer">
              <button
                type="button"
                className="pathology-btn pathology-btn--ghost"
                disabled={requestingStain || selectedStains.length === 0}
                onClick={requestSpecialStain}
              >
                <FormattedMessage id="pathology.workflow.requestSpecialStain" />
              </button>
            </div>
          </>
        )}
      </div>
    );
  };

  const renderReadCard = ({ editable }) => (
    <div
      className={
        "pathology-read-card" +
        (editable ? "" : " pathology-read-card--compact")
      }
    >
      {renderReadHistory()}
      <label
        className="pathology-grossing-label"
        htmlFor="pathology-microscopy"
      >
        <FormattedMessage id="pathology.workflow.microscopicFindings" />
      </label>
      {editable ? (
        <textarea
          id="pathology-microscopy"
          className="pathology-grossing-textarea"
          rows={4}
          value={microscopyDraft}
          onChange={(e) => setMicroscopyDraft(e.target.value)}
          placeholder={intl.formatMessage({
            id: "pathology.workflow.microscopicFindingsHint",
          })}
        />
      ) : (
        <div className="pathology-grossing-readonly">
          {pathologySampleInfo?.microscopyExam?.trim()
            ? pathologySampleInfo.microscopyExam
            : "—"}
        </div>
      )}

      <div className="pathology-grossing-label">
        <FormattedMessage id="pathology.workflow.conclusionStructured" />
      </div>
      {editable ? (
        <div className="pathology-conclusion-options">
          {conclusionOptions.length === 0 ? (
            <div className="pathology-cassette-empty">
              <FormattedMessage id="pathology.workflow.noConclusionOptions" />
            </div>
          ) : (
            conclusionOptions.map((option) => (
              <label key={option.id} className="pathology-conclusion-option">
                <input
                  type="checkbox"
                  checked={selectedConclusionIds.includes(option.id)}
                  onChange={() => toggleConclusionId(option.id)}
                />
                <span>{option.value}</span>
              </label>
            ))
          )}
        </div>
      ) : (
        <div className="pathology-grossing-readonly">
          {(pathologySampleInfo?.conclusions || []).length > 0
            ? (pathologySampleInfo.conclusions || [])
                .map((c) => c.value)
                .filter(Boolean)
                .join(", ")
            : "—"}
        </div>
      )}

      <label
        className="pathology-grossing-label"
        htmlFor="pathology-conclusion-text"
      >
        <FormattedMessage id="pathology.workflow.conclusionText" />
      </label>
      {editable ? (
        <textarea
          id="pathology-conclusion-text"
          className="pathology-grossing-textarea"
          rows={3}
          value={conclusionTextDraft}
          onChange={(e) => setConclusionTextDraft(e.target.value)}
          placeholder={intl.formatMessage({
            id: "pathology.workflow.conclusionTextHint",
          })}
        />
      ) : (
        <div className="pathology-grossing-readonly">
          {pathologySampleInfo?.conclusionText?.trim()
            ? pathologySampleInfo.conclusionText
            : "—"}
        </div>
      )}

      {renderSpecialStainSection({ editable })}

      {editable ? (
        <div className="pathology-read-footer">
          <button
            type="button"
            className="pathology-btn pathology-btn--ghost"
            disabled={savingDraft || signingOut}
            onClick={saveReadDraft}
          >
            <FormattedMessage id="pathology.workflow.saveDraft" />
          </button>
          <button
            type="button"
            className="pathology-btn pathology-btn--primary"
            disabled={signingOut || savingDraft}
            onClick={signOutCase}
          >
            <FormattedMessage id="pathology.workflow.signOutFinalize" />
          </button>
        </div>
      ) : (
        <div className="pathology-read-signed-note">
          <FormattedMessage
            id="pathology.workflow.signedOutNote"
            values={{
              pathologist: pathologySampleInfo?.assignedPathologist || "—",
            }}
          />
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
            {[
              labNo,
              subtypeLabel || null,
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
          <FormattedMessage id={badgeLabelId} />
        </span>
      </div>

      <ol className="pathology-rail">
        {workflowSteps.map((step, index) => {
          const state = stepState(step.id);
          const stepNumber = index + 1;
          const label = intl.formatMessage({ id: step.labelId });
          const isLast = index === workflowSteps.length - 1;
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

                {state === "completed" && step.id === "microtomy" && (
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
                          {completedMicrotomySummary}
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
                      renderMicrotomyCard({ allowActions: false })}
                  </div>
                )}

                {state === "completed" && step.id === "staining" && (
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
                          {completedStainingSummary}
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
                      renderStainingCard({ allowMark: false })}
                  </div>
                )}

                {state === "completed" && step.id === "review" && (
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
                          {completedReadSummary}
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
                    {isExpandedCompleted && renderReadCard({ editable: false })}
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

                {state === "active" && step.id === "microtomy" && (
                  <div className="pathology-rail-panel">
                    <h3 className="pathology-rail-step-title">{label}</h3>
                    <p className="pathology-rail-step-copy">
                      <FormattedMessage id="pathology.workflow.microtomyIntro" />
                    </p>
                    {renderMicrotomyCard({ allowActions: true })}
                  </div>
                )}

                {state === "active" && step.id === "staining" && (
                  <div className="pathology-rail-panel">
                    <h3 className="pathology-rail-step-title">{label}</h3>
                    <p className="pathology-rail-step-copy">
                      <FormattedMessage id="pathology.workflow.stainingIntro" />
                    </p>
                    {renderStainingCard({ allowMark: true })}
                  </div>
                )}

                {state === "active" && step.id === "review" && (
                  <div className="pathology-rail-panel">
                    <h3 className="pathology-rail-step-title">{label}</h3>
                    <p className="pathology-rail-step-copy">
                      <FormattedMessage id="pathology.workflow.reviewIntro" />
                    </p>
                    {renderReadCard({ editable: true })}
                  </div>
                )}

                {state === "active" &&
                  step.id !== "collection" &&
                  step.id !== "grossing" &&
                  step.id !== "processing" &&
                  step.id !== "embedding" &&
                  step.id !== "microtomy" &&
                  step.id !== "staining" &&
                  step.id !== "review" && (
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
