package org.openelisglobal.reception.dto;

import java.util.ArrayList;
import java.util.List;

public class ReceptionDetailResponse extends ReceptionQueueItemDTO {

    private String notes;
    private List<ReceptionSampleItemDTO> sampleItems = new ArrayList<>();

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<ReceptionSampleItemDTO> getSampleItems() {
        return sampleItems;
    }

    public void setSampleItems(List<ReceptionSampleItemDTO> sampleItems) {
        this.sampleItems = sampleItems;
    }
}
