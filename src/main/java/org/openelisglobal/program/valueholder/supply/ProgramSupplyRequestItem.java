package org.openelisglobal.program.valueholder.supply;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.openelisglobal.common.valueholder.BaseObject;

/** One supply line; becomes one OpenMRS medicalsupplyorder. */
@Entity
@Table(name = "program_supply_request_item")
public class ProgramSupplyRequestItem extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "program_supply_request_item_generator")
    @SequenceGenerator(name = "program_supply_request_item_generator", sequenceName = "program_supply_request_item_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "supply_request_id", referencedColumnName = "id")
    private ProgramSupplyRequest supplyRequest;

    @Column(name = "concept_uuid")
    private String conceptUuid;

    @Column(name = "concept_name")
    private String conceptName;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "quantity_units_uuid")
    private String quantityUnitsUuid;

    @Column(name = "quantity_units_name")
    private String quantityUnitsName;

    /** OpenMRS order urgency: ROUTINE or STAT. */
    @Column(name = "urgency")
    private String urgency;

    @Column(name = "instructions")
    private String instructions;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public ProgramSupplyRequest getSupplyRequest() {
        return supplyRequest;
    }

    public void setSupplyRequest(ProgramSupplyRequest supplyRequest) {
        this.supplyRequest = supplyRequest;
    }

    public String getConceptUuid() {
        return conceptUuid;
    }

    public void setConceptUuid(String conceptUuid) {
        this.conceptUuid = conceptUuid;
    }

    public String getConceptName() {
        return conceptName;
    }

    public void setConceptName(String conceptName) {
        this.conceptName = conceptName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getQuantityUnitsUuid() {
        return quantityUnitsUuid;
    }

    public void setQuantityUnitsUuid(String quantityUnitsUuid) {
        this.quantityUnitsUuid = quantityUnitsUuid;
    }

    public String getQuantityUnitsName() {
        return quantityUnitsName;
    }

    public void setQuantityUnitsName(String quantityUnitsName) {
        this.quantityUnitsName = quantityUnitsName;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
}
