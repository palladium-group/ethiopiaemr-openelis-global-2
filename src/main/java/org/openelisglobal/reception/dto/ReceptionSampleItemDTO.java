package org.openelisglobal.reception.dto;

import java.util.ArrayList;
import java.util.List;

public class ReceptionSampleItemDTO {

    private String sampleItemId;
    private String externalId;
    private String sampleType;
    private String collectionDate;
    private List<ReceptionTestDTO> tests = new ArrayList<>();

    public String getSampleItemId() {
        return sampleItemId;
    }

    public void setSampleItemId(String sampleItemId) {
        this.sampleItemId = sampleItemId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    public String getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(String collectionDate) {
        this.collectionDate = collectionDate;
    }

    public List<ReceptionTestDTO> getTests() {
        return tests;
    }

    public void setTests(List<ReceptionTestDTO> tests) {
        this.tests = tests;
    }
}
