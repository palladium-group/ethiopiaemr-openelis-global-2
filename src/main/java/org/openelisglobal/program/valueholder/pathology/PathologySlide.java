package org.openelisglobal.program.valueholder.pathology;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "pathology_slide")
public class PathologySlide extends BaseObject<Integer> {

    /** Whether this slide carries patient tissue or is a stain control. */
    public enum SlideRole {
        PATIENT, CONTROL_POS, CONTROL_NEG
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pathology_slide_generator")
    @SequenceGenerator(name = "pathology_slide_generator", sequenceName = "pathology_slide_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "slide_number")
    private Integer slideNumber;

    @Type(type = "org.hibernate.type.BinaryType")
    private byte[] image;

    @Column(name = "file_type")
    private String fileType;

    private String location;

    /**
     * Parent cassette/block for Microtomy cuts; null for legacy free-floating
     * slides.
     */
    @Column(name = "pathology_block_id")
    private Integer pathologyBlockId;

    /** When this slide passed Microtomy confirm (null until confirmed). */
    @Column(name = "confirmed_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private Timestamp confirmedAt;

    /** When this slide was marked stained (null until Staining Step 8). */
    @Column(name = "stained_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private Timestamp stainedAt;

    /**
     * Stain applied to this slide (e.g. H&E, PAS, Ziehl-Neelsen). Null on legacy
     * slides.
     */
    @Column(name = "stain_type")
    private String stainType;

    /**
     * PATIENT tissue, or a positive/negative control slide accompanying a special
     * stain.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "slide_role")
    private SlideRole slideRole = SlideRole.PATIENT;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSlideNumber() {
        return slideNumber;
    }

    public void setSlideNumber(Integer slideNumber) {
        this.slideNumber = slideNumber;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getPathologyBlockId() {
        return pathologyBlockId;
    }

    public void setPathologyBlockId(Integer pathologyBlockId) {
        this.pathologyBlockId = pathologyBlockId;
    }

    public Timestamp getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Timestamp confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Timestamp getStainedAt() {
        return stainedAt;
    }

    public void setStainedAt(Timestamp stainedAt) {
        this.stainedAt = stainedAt;
    }

    public String getStainType() {
        return stainType;
    }

    public void setStainType(String stainType) {
        this.stainType = stainType;
    }

    public SlideRole getSlideRole() {
        return slideRole;
    }

    public void setSlideRole(SlideRole slideRole) {
        this.slideRole = slideRole;
    }
}
