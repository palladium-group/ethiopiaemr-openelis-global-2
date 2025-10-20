package org.openelisglobal.notebook.valueholder;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.List;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

@Entity
@Table(name = "notebook")
public class NoteBook extends BaseObject<Integer> {

    private static final long serialVersionUID = -979624722823577192L;
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notebook_generator")
    @SequenceGenerator(name = "notebook_generator", sequenceName = "notebook_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "title")
    private String title;

    @Column(name = "type")
    private String type;

    @Column(name = "project")
    private String project;

    @Column(name = "objective")
    private String objective;

    @Column(name = "protocol")
    private String protocol;

    @Column(name = "content")
    private String content;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinTable(name = "notebook_samples", joinColumns = @JoinColumn(name = "notebook_id"), inverseJoinColumns = @JoinColumn(name = "sample_item_id"))
    private List<SampleItem> samples;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinTable(name = "notebook_analysers", joinColumns = @JoinColumn(name = "notebook_id"), inverseJoinColumns = @JoinColumn(name = "analyser_id"))
    private List<Analyzer> analysers;

    @ElementCollection
    @CollectionTable(name = "notebook_tags", joinColumns = @JoinColumn(name = "notebook_id"))
    @Column(name = "tag")
    private List<String> tags;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "notebook_id", referencedColumnName = "id")
    private List<NoteBookPage> pages;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "notebook_id", referencedColumnName = "id")
    private List<NoteBookFile> files;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

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

    public List<SampleItem> getSamples() {
        return samples;
    }

    public void setSamples(List<SampleItem> samples) {
        this.samples = samples;
    }

    public List<NoteBookPage> getPages() {
        return pages;
    }

    public void setPages(List<NoteBookPage> pages) {
        this.pages = pages;
    }

    public List<Analyzer> getAnalysers() {
        return analysers;
    }

    public void setAnalysers(List<Analyzer> analysers) {
        this.analysers = analysers;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

}
