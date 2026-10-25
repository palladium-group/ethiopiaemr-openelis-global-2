package org.openelisglobal.program.valueholder.supply;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.systemuser.valueholder.SystemUser;

/**
 * One "Order supplies" submission from a pathology/cytology case. It becomes a
 * single OpenMRS order encounter whose uuid is generated up front, so a retry
 * never creates a second encounter. OpenMRS forwards that encounter to Dagu the
 * same way it does for an O3 order basket.
 */
@Entity
@Table(name = "program_supply_request")
public class ProgramSupplyRequest extends BaseObject<Integer> {

    /** Whether the order reached OpenMRS. */
    public enum Status {
        PENDING, SENT, FAILED
    }

    /**
     * Final outcome OpenFn reported to OpenMRS for the encounter; null until one is
     * reported.
     */
    public enum DaguStatus {
        SUCCESS, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "program_supply_request_generator")
    @SequenceGenerator(name = "program_supply_request_generator", sequenceName = "program_supply_request_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "sample_id", referencedColumnName = "id")
    private Sample sample;

    @Column(name = "encounter_uuid")
    private String encounterUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "dagu_status")
    private DaguStatus daguStatus;

    @Column(name = "dagu_reason")
    private String daguReason;

    @ManyToOne
    @JoinColumn(name = "requested_by", referencedColumnName = "id")
    private SystemUser requestedBy;

    @Column(name = "requested_at")
    private Timestamp requestedAt;

    @OneToMany(mappedBy = "supplyRequest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id")
    private List<ProgramSupplyRequestItem> items = new ArrayList<>();

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Sample getSample() {
        return sample;
    }

    public void setSample(Sample sample) {
        this.sample = sample;
    }

    public String getEncounterUuid() {
        return encounterUuid;
    }

    public void setEncounterUuid(String encounterUuid) {
        this.encounterUuid = encounterUuid;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public DaguStatus getDaguStatus() {
        return daguStatus;
    }

    public void setDaguStatus(DaguStatus daguStatus) {
        this.daguStatus = daguStatus;
    }

    public String getDaguReason() {
        return daguReason;
    }

    public void setDaguReason(String daguReason) {
        this.daguReason = daguReason;
    }

    public SystemUser getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(SystemUser requestedBy) {
        this.requestedBy = requestedBy;
    }

    public Timestamp getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Timestamp requestedAt) {
        this.requestedAt = requestedAt;
    }

    public List<ProgramSupplyRequestItem> getItems() {
        return items;
    }

    public void setItems(List<ProgramSupplyRequestItem> items) {
        this.items = items;
    }

    public void addItem(ProgramSupplyRequestItem item) {
        item.setSupplyRequest(this);
        items.add(item);
    }
}
