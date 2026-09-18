package org.openelisglobal.program.valueholder.pathology;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "pathology_block")
public class PathologyBlock extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pathology_block_generator")
    @SequenceGenerator(name = "pathology_block_generator", sequenceName = "pathology_block_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "block_number")
    private Integer blockNumber;

    private String location;

    /** When this cassette was marked embedded (null until Embedding Step 6). */
    @Column(name = "embedded_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private Timestamp embeddedAt;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(Integer blockNumber) {
        this.blockNumber = blockNumber;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Timestamp getEmbeddedAt() {
        return embeddedAt;
    }

    public void setEmbeddedAt(Timestamp embeddedAt) {
        this.embeddedAt = embeddedAt;
    }
}
