package org.openelisglobal.program.valueholder.supply;

import java.util.ArrayList;
import java.util.List;

public class ProgramSupplyRequestDisplayItem {

    private Integer id;
    /** Epoch milliseconds. */
    private Long requestedAt;
    private String requestedBy;
    private String status;
    private String failureReason;
    private String daguStatus;
    private String daguReason;
    private List<Item> items = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Long requestedAt) {
        this.requestedAt = requestedAt;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getDaguStatus() {
        return daguStatus;
    }

    public void setDaguStatus(String daguStatus) {
        this.daguStatus = daguStatus;
    }

    public String getDaguReason() {
        return daguReason;
    }

    public void setDaguReason(String daguReason) {
        this.daguReason = daguReason;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public static class Item {
        private String conceptName;
        private Integer quantity;
        private String quantityUnitsName;
        private String urgency;
        private String instructions;

        public String getConceptName() {
            return conceptName;
        }

        public void setConceptName(String conceptName) {
            this.conceptName = conceptName;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public String getQuantityUnitsName() {
            return quantityUnitsName;
        }

        public void setQuantityUnitsName(String quantityUnitsName) {
            this.quantityUnitsName = quantityUnitsName;
        }

        public String getUrgency() {
            return urgency;
        }

        public void setUrgency(String urgency) {
            this.urgency = urgency;
        }

        public String getInstructions() {
            return instructions;
        }

        public void setInstructions(String instructions) {
            this.instructions = instructions;
        }
    }
}
