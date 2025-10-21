package org.openelisglobal.notebook.bean;

import java.util.List;
import org.openelisglobal.notebook.valueholder.NoteBookFile;
import org.openelisglobal.notebook.valueholder.NoteBookPage;

public class NoteBookFullDisplayBean extends NoteBookDisplayBean {

    private String protocol;
    private String objective;
    private List<String> instruments;
    private String project;
    private String content;
    private List<NoteBookPage> pages;
    private List<NoteBookFile> files;
    private List<SampleDisplayBean> samples;
    private String assignedTechnician;

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

    public String getAssignedTechnician() {
        return assignedTechnician;
    }

    public void setAssignedTechnician(String assignedTechnician) {
        this.assignedTechnician = assignedTechnician;
    }

    public List<SampleDisplayBean> getSamples() {
        return samples;
    }

    public void setSamples(List<SampleDisplayBean> samples) {
        this.samples = samples;
    }

    public static class SampleDisplayBean {
        private String sampleType;
        private String collectionDate;
        private List<ResultDisplayBean> results;

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

        public List<ResultDisplayBean> getResults() {
            return results;
        }

        public void setResults(List<ResultDisplayBean> results) {
            this.results = results;
        }

    }

    public static class ResultDisplayBean {
        private String test;
        private String result;
        private String dateCreated;

        public String getTest() {
            return test;
        }

        public void setTest(String test) {
            this.test = test;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public String getDateCreated() {
            return dateCreated;
        }

        public void setDateCreated(String dateCreated) {
            this.dateCreated = dateCreated;
        }
    }
}
