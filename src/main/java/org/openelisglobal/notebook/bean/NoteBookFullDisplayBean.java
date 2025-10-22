package org.openelisglobal.notebook.bean;

import java.util.List;
import org.openelisglobal.notebook.valueholder.NoteBookFile;
import org.openelisglobal.notebook.valueholder.NoteBookPage;

public class NoteBookFullDisplayBean extends NoteBookDisplayBean {

    private String protocol;
    private String objective;
    private List<String> instruments;
    private List<Integer> instrumentIds;
    private String project;
    private String content;
    private List<NoteBookPage> pages;
    private List<NoteBookFile> files;
    private List<SampleDisplayBean> samples;
    private Integer technicianId;
    private String technicianName;
    private Integer patientId;

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public List<String> getInstruments() {
        return instruments;
    }

    public void setInstruments(List<String> instruments) {
        this.instruments = instruments;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<NoteBookPage> getPages() {
        return pages;
    }

    public void setPages(List<NoteBookPage> pages) {
        this.pages = pages;
    }

    public List<NoteBookFile> getFiles() {
        return files;
    }

    public void setFiles(List<NoteBookFile> files) {
        this.files = files;
    }

    public Integer getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(Integer technicianId) {
        this.technicianId = technicianId;
    }

    public List<SampleDisplayBean> getSamples() {
        return samples;
    }

    public void setSamples(List<SampleDisplayBean> samples) {
        this.samples = samples;
    }

    public String getTechnicianName() {
        return technicianName;
    }

    public void setTechnicianName(String technicianName) {
        this.technicianName = technicianName;
    }

    public Integer getPatientId() {
        return patientId;
    }

    public void setPatientId(Integer patientId) {
        this.patientId = patientId;
    }

    public List<Integer> getInstrumentIds() {
        return instrumentIds;
    }

    public void setInstrumentIds(List<Integer> instrumentIds) {
        this.instrumentIds = instrumentIds;
    }

}
