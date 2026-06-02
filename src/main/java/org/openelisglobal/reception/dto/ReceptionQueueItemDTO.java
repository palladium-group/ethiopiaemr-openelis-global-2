package org.openelisglobal.reception.dto;

import java.util.ArrayList;
import java.util.List;

public class ReceptionQueueItemDTO {

    private String sampleId;
    private String accessionNumber;
    private String patientId;
    private String patientNationalId;
    private String patientName;
    private String referringId;
    private String priority;
    private String collectionDate;
    private String receivedDate;
    private String receivedTime;
    private int pendingTestCount;
    private List<ReceptionTestDTO> tests = new ArrayList<>();

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getPatientNationalId() {
        return patientNationalId;
    }

    public void setPatientNationalId(String patientNationalId) {
        this.patientNationalId = patientNationalId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getReferringId() {
        return referringId;
    }

    public void setReferringId(String referringId) {
        this.referringId = referringId;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(String collectionDate) {
        this.collectionDate = collectionDate;
    }

    public String getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(String receivedDate) {
        this.receivedDate = receivedDate;
    }

    public String getReceivedTime() {
        return receivedTime;
    }

    public void setReceivedTime(String receivedTime) {
        this.receivedTime = receivedTime;
    }

    public int getPendingTestCount() {
        return pendingTestCount;
    }

    public void setPendingTestCount(int pendingTestCount) {
        this.pendingTestCount = pendingTestCount;
    }

    public List<ReceptionTestDTO> getTests() {
        return tests;
    }

    public void setTests(List<ReceptionTestDTO> tests) {
        this.tests = tests;
    }
}
