package org.openelisglobal.program.bean;

public class PathologyDashBoardCount {

    /** Cases with no pathologist assigned (typically RECEIVED from EMR). */
    Long unassigned;

    Long inProgress;

    Long awaitingReview;

    Long additionalRequests;

    Long complete;

    public Long getUnassigned() {
        return unassigned;
    }

    public void setUnassigned(Long unassigned) {
        this.unassigned = unassigned;
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
