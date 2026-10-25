package org.openelisglobal.program.controller.supply;

import java.util.ArrayList;
import java.util.List;

public class ProgramSupplyRequestForm {

    private List<Item> items = new ArrayList<>();

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public static class Item {
        private String conceptUuid;
        private String conceptName;
        private Integer quantity;
        private String quantityUnitsUuid;
        private String quantityUnitsName;
        private String urgency;
        private String instructions;

        public String getConceptUuid() {
            return conceptUuid;
        }

        public void setConceptUuid(String conceptUuid) {
            this.conceptUuid = conceptUuid;
        }

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

        public String getQuantityUnitsUuid() {
            return quantityUnitsUuid;
        }

        public void setQuantityUnitsUuid(String quantityUnitsUuid) {
            this.quantityUnitsUuid = quantityUnitsUuid;
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
