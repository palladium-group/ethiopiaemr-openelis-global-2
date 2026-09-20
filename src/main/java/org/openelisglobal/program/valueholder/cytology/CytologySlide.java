package org.openelisglobal.program.valueholder.cytology;

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
@Table(name = "cytology_slide")
public class CytologySlide extends BaseObject<Integer> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cytology_slide_generator")
    @SequenceGenerator(name = "cytology_slide_generator", sequenceName = "cytology_slide_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "slide_number")
    private Integer slideNumber;

    @Column(name = "image")
    @Type(type = "org.hibernate.type.BinaryType")
    private byte[] image;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "location")
    private String location;

    /** SMEAR for the collected smears, CELL_BLOCK_HE for the Fluid cell block's own H&E slide. */
    @Enumerated(EnumType.STRING)
    @Column(name = "slide_type")
    private CytologySlideType slideType = CytologySlideType.SMEAR;

    @Column(name = "stained_at")
    private Timestamp stainedAt;

    public enum CytologySlideType {
        SMEAR, CELL_BLOCK_HE
    }

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

    public CytologySlideType getSlideType() {
        return slideType;
    }

    public void setSlideType(CytologySlideType slideType) {
        this.slideType = slideType;
    }

    public Timestamp getStainedAt() {
        return stainedAt;
    }

    public void setStainedAt(Timestamp stainedAt) {
        this.stainedAt = stainedAt;
    }
}
