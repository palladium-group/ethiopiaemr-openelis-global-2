package org.openelisglobal.program.bean;

/**
 * Tile counts for the merged pathology reception dashboard, summed across the
 * selected service categories. {@code additionalRequests} only ever counts
 * histopathology cases — cytopathology has no equivalent stage.
 */
public class ProgramReceptionDashBoardCount {

    private Long unassigned = 0L;

    private Long received = 0L;

    private Long inProgress = 0L;

    private Long awaitingReview = 0L;

    private Long additionalRequests = 0L;

    private Long complete = 0L;

    public Long getUnassigned() {
        return unassigned;
    }

    public void setUnassigned(Long unassigned) {
        this.unassigned = unassigned;
    }

    public Long getReceived() {
        return received;
    }

    public void setReceived(Long received) {
        this.received = received;
    }

    public Long getInProgress() {
        return inProgress;
    }

    public void setInProgress(Long inProgress) {
        this.inProgress = inProgress;
    }

    public Long getAwaitingReview() {
        return awaitingReview;
    }

    public void setAwaitingReview(Long awaitingReview) {
        this.awaitingReview = awaitingReview;
    }

    public Long getAdditionalRequests() {
        return additionalRequests;
    }

    public void setAdditionalRequests(Long additionalRequests) {
        this.additionalRequests = additionalRequests;
    }

    public Long getComplete() {
        return complete;
    }

    public void setComplete(Long complete) {
        this.complete = complete;
    }
}
