package org.openelisglobal.program.bean;

/**
 * Pathologist choice for Reception assignment, including open caseload so Reception can
 * balance work across pathologists (Biopsy Workflow UI/UX Step 1).
 */
public class PathologyPathologistOption {

    private String id;
    private String value;
    private Long openCaseload;

    public PathologyPathologistOption() {
    }

    public PathologyPathologistOption(String id, String value, Long openCaseload) {
        this.id = id;
        this.value = value;
        this.openCaseload = openCaseload;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Long getOpenCaseload() {
        return openCaseload;
    }

    public void setOpenCaseload(Long openCaseload) {
        this.openCaseload = openCaseload;
    }
}
