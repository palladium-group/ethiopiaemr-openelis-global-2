package org.openelisglobal.program.valueholder.pathology;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.systemuser.valueholder.SystemUser;

/**
 * One pathologist read round on a case. Round 1 is the biopsy/H&E read; each
 * subsequent round is a read after a special-stain request. Rounds are
 * append-only history: nothing is overwritten, so the full story (each round's
 * microscopy + conclusion) is preserved on the case. The sign-out returns the
 * latest finalized round's conclusion.
 */
@Entity
@Table(name = "pathology_read")
public class PathologyRead extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pathology_read_generator")
    @SequenceGenerator(name = "pathology_read_generator", sequenceName = "pathology_read_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "round_number")
    private Integer roundNumber;

    @Column(name = "microscopy_exam")
    private String microscopyExam;

    @Column(name = "conclusion_text")
    private String conclusionText;

    /**
     * Comma-joined dictionary ids for the structured conclusions selected in this
     * round.
     */
    @Column(name = "conclusion_dictionary_ids")
    private String conclusionDictionaryIds;

    /**
     * True once the pathologist closes this round (by signing out or requesting a
     * special stain).
     */
    @Column(name = "finalized")
    private Boolean finalized = false;

    @OneToOne
    @JoinColumn(name = "reviewed_by", referencedColumnName = "id")
    private SystemUser reviewedBy;

    @Column(name = "reviewed_at")
    private Timestamp reviewedAt;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(Integer roundNumber) {
        this.roundNumber = roundNumber;
    }

    public String getMicroscopyExam() {
        return microscopyExam;
    }

    public void setMicroscopyExam(String microscopyExam) {
        this.microscopyExam = microscopyExam;
    }

    public String getConclusionText() {
        return conclusionText;
    }

    public void setConclusionText(String conclusionText) {
        this.conclusionText = conclusionText;
    }

    public String getConclusionDictionaryIds() {
        return conclusionDictionaryIds;
    }

    public void setConclusionDictionaryIds(String conclusionDictionaryIds) {
        this.conclusionDictionaryIds = conclusionDictionaryIds;
    }

    public Boolean getFinalized() {
        return finalized;
    }

    public void setFinalized(Boolean finalized) {
        this.finalized = finalized;
    }

    public SystemUser getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(SystemUser reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public Timestamp getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Timestamp reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
