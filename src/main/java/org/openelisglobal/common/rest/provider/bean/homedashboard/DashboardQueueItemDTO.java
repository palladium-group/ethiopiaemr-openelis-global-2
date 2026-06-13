package org.openelisglobal.common.rest.provider.bean.homedashboard;

import java.sql.Timestamp;

public class DashboardQueueItemDTO {

    private String id;

    private String accessionNumber;

    private String priority;

    private String orderDate;

    private Timestamp orderDateSort;

    private String patientName;

    private String subjectNumber;

    private String patientNationalId;

    private int testCount;

    private String testSectionId;

    private String testNames;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(String orderDate) {
        this.orderDate = orderDate;
    }

    public Timestamp getOrderDateSort() {
        return orderDateSort;
    }

    public void setOrderDateSort(Timestamp orderDateSort) {
        this.orderDateSort = orderDateSort;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getSubjectNumber() {
        return subjectNumber;
    }

    public void setSubjectNumber(String subjectNumber) {
        this.subjectNumber = subjectNumber;
    }

    public String getPatientNationalId() {
        return patientNationalId;
    }

    public void setPatientNationalId(String patientNationalId) {
        this.patientNationalId = patientNationalId;
    }

    public int getTestCount() {
        return testCount;
    }

    public void setTestCount(int testCount) {
        this.testCount = testCount;
    }

    public String getTestSectionId() {
        return testSectionId;
    }

    public void setTestSectionId(String testSectionId) {
        this.testSectionId = testSectionId;
    }

    public String getTestNames() {
        return testNames;
    }

    public void setTestNames(String testNames) {
        this.testNames = testNames;
    }
}
