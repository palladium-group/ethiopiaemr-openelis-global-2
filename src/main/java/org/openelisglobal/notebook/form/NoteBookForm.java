package org.openelisglobal.notebook.form;

import java.util.Base64;
import java.util.List;
import org.openelisglobal.notebook.valueholder.NoteBookFile;
import org.openelisglobal.notebook.valueholder.NoteBookPage;

public class NoteBookForm {
    private String title;
    private String type;
    private String project;
    private String objective;
    private String protocol;
    private String content;
    private String technicianId;
    private String patientId;
    private List<String> sampleIds;
    private List<String> tags;
    private List<NoteBookPage> pages;
    private List<NoteBookFileForm> files;
    private List<String> analyserIds;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(String technicianId) {
        this.technicianId = technicianId;
    }

    public List<String> getSampleIds() {
        return sampleIds;
    }

    public void setSampleIds(List<String> sampleIds) {
        this.sampleIds = sampleIds;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<NoteBookFileForm> getFiles() {
        return files;
    }

    public void setFiles(List<NoteBookFileForm> files) {
        this.files = files;
    }

    public List<String> getAnalyserIds() {
        return analyserIds;
    }

    public void setAnalyserIds(List<String> analyserIds) {
        this.analyserIds = analyserIds;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public List<NoteBookPage> getPages() {
        return pages;
    }

    public void setPages(List<NoteBookPage> pages) {
        this.pages = pages;
    }

    public static class NoteBookFileForm extends NoteBookFile {

        private static final long serialVersionUID = 3142138533368581327L;

        private String base64File;

        public String getBase64File() {
            return base64File;
        }

        public void setBase64File(String base64File) {
            this.base64File = base64File;
            String[] imageInfo = base64File.split(";base64,", 2);

            setFileType(imageInfo[0]);
            setFileData(Base64.getDecoder().decode(imageInfo[1]));
        }
    }
}
