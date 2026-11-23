package org.openelisglobal.program.service.hematopathology;

import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.program.valueholder.hematopathology.HematopathologySample.HematopathologyStatus;
import org.openelisglobal.program.valueholder.hematopathology.HematopathologySample.HematopathologySubtype;

/**
 * Pure step rules for the hematopathology case rail.
 *
 * <pre>
 * Morphology (Peripheral / Bone marrow): Collection → Smear prep &amp; staining → Review &amp; report
 * Flow cytometry:                        Collection → Processing → Acquisition &amp; analysis → Review &amp; report
 * </pre>
 */
public final class HematopathologyWorkflow {

    /** Morphology: smear prepared (first half of Smear prep &amp; staining). */
    public static final String STEP_SMEAR_PREPARED = "smearPrepared";
    /** Morphology: smear stained — completes Smear prep &amp; staining. */
    public static final String STEP_SMEAR_STAINED = "smearStained";
    /** Flow: antibody staining / lysis done — completes Processing. */
    public static final String STEP_PROCESSING = "processing";
    /** Flow: run on the cytometer and gated — completes Acquisition. */
    public static final String STEP_ACQUISITION = "acquisition";

    private HematopathologyWorkflow() {
    }

    /** The first lab step after Collection (and after the payment hold). */
    public static HematopathologyStatus statusAfterCollection(HematopathologySubtype subtype) {
        return subtype == HematopathologySubtype.FLOW_CYTOMETRY ? HematopathologyStatus.PROCESSING
                : HematopathologyStatus.SMEAR_PREP;
    }

    public static boolean isAtCollection(HematopathologyStatus status) {
        return status == HematopathologyStatus.RECEIVED;
    }

    public static boolean isReadyForRead(HematopathologyStatus status) {
        return status == HematopathologyStatus.READY_FOR_HEMATOPATHOLOGIST;
    }

    public static boolean isClosed(HematopathologyStatus status) {
        return status == HematopathologyStatus.COMPLETED || status == HematopathologyStatus.REJECTED;
    }

    /**
     * Validates a lab step for the case's subtype and current status.
     *
     * @return the status the case moves to after the step, or {@code null} when the
     *         step was already done (idempotent re-click)
     * @throws IllegalArgumentException when the step does not apply now
     */
    public static HematopathologyStatus statusAfterStep(HematopathologySubtype subtype, HematopathologyStatus status,
            String step, boolean smearPrepared) {
        if (GenericValidator.isBlankOrNull(step)) {
            throw new IllegalArgumentException("step is required");
        }
        HematopathologySubtype type = subtype == null ? HematopathologySubtype.PERIPHERAL_MORPHOLOGY : subtype;
        switch (step.trim()) {
        case STEP_SMEAR_PREPARED:
            requireMorphology(type, step);
            if (status != HematopathologyStatus.SMEAR_PREP) {
                return alreadyPast(status, HematopathologyStatus.SMEAR_PREP, step);
            }
            return HematopathologyStatus.SMEAR_PREP;
        case STEP_SMEAR_STAINED:
            requireMorphology(type, step);
            if (status != HematopathologyStatus.SMEAR_PREP) {
                return alreadyPast(status, HematopathologyStatus.SMEAR_PREP, step);
            }
            if (!smearPrepared) {
                throw new IllegalArgumentException("prepare the smear before staining it");
            }
            return HematopathologyStatus.READY_FOR_HEMATOPATHOLOGIST;
        case STEP_PROCESSING:
            requireFlow(type, step);
            if (status != HematopathologyStatus.PROCESSING) {
                return alreadyPast(status, HematopathologyStatus.PROCESSING, step);
            }
            return HematopathologyStatus.ACQUISITION;
        case STEP_ACQUISITION:
            requireFlow(type, step);
            if (status != HematopathologyStatus.ACQUISITION) {
                return alreadyPast(status, HematopathologyStatus.ACQUISITION, step);
            }
            return HematopathologyStatus.READY_FOR_HEMATOPATHOLOGIST;
        default:
            throw new IllegalArgumentException("unknown step: " + step);
        }
    }

    private static void requireMorphology(HematopathologySubtype type, String step) {
        if (!type.isMorphology()) {
            throw new IllegalArgumentException(step + " applies to morphology tests only");
        }
    }

    private static void requireFlow(HematopathologySubtype type, String step) {
        if (type != HematopathologySubtype.FLOW_CYTOMETRY) {
            throw new IllegalArgumentException(step + " applies to flow cytometry only");
        }
    }

    private static HematopathologyStatus alreadyPast(HematopathologyStatus status, HematopathologyStatus stepStatus,
            String step) {
        if (rank(status) > rank(stepStatus)) {
            return null;
        }
        throw new IllegalArgumentException("case must be in " + stepStatus + " for " + step);
    }

    private static int rank(HematopathologyStatus status) {
        if (status == null) {
            return 0;
        }
        switch (status) {
        case RECEIVED:
            return 1;
        case AWAITING_PAYMENT:
            return 2;
        case SMEAR_PREP:
        case PROCESSING:
            return 3;
        case ACQUISITION:
            return 4;
        case READY_FOR_HEMATOPATHOLOGIST:
            return 5;
        case COMPLETED:
            return 6;
        default:
            // REJECTED never moves on
            return 0;
        }
    }
}
