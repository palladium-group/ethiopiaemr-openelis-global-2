package org.openelisglobal.reception.service;

import ca.uhn.fhir.context.FhirContext;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.ExternalOrderStatus;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderType;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.reception.dto.RecollectionOrderResult;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ElectronicOrderRecollectionServiceImpl implements ElectronicOrderRecollectionService {

    static final Pattern RECOLLECTION_SUFFIX = Pattern.compile("-R\\d+$");
    private static final String SAMPLE_TYPE_INPUT_CODE = "CI0050007AAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4Cached();

    @Autowired
    private ElectronicOrderService electronicOrderService;

    @Autowired
    private IStatusService statusService;

    @Autowired
    private SampleItemService sampleItemService;

    @Override
    @Transactional
    public Optional<RecollectionOrderResult> createRecollectionOrder(Sample sample, String sysUserId) {
        ElectronicOrder sourceOrder = resolveSourceOrder(sample);
        if (sourceOrder == null || GenericValidator.isBlankOrNull(sourceOrder.getExternalId())) {
            return Optional.empty();
        }

        String baseExternalId = extractBaseExternalId(sourceOrder.getExternalId());
        String newExternalId = nextRecollectionExternalId(baseExternalId);

        ElectronicOrder newOrder = new ElectronicOrder();
        newOrder.setExternalId(newExternalId);
        newOrder.setData(enrichOrderDataWithSampleType(sourceOrder.getData(), sample));
        newOrder.setPatient(sourceOrder.getPatient());
        newOrder.setStatusId(statusService.getStatusID(ExternalOrderStatus.Entered));
        newOrder.setOrderTimestamp(new Timestamp(System.currentTimeMillis()));
        newOrder.setSysUserId(sysUserId);
        newOrder.setType(sourceOrder.getType() != null ? sourceOrder.getType() : ElectronicOrderType.FHIR);
        newOrder.setPriority(sourceOrder.getPriority() != null ? sourceOrder.getPriority() : OrderPriority.ROUTINE);

        String newOrderId = electronicOrderService.insert(newOrder);
        return Optional.of(new RecollectionOrderResult(newOrderId, newExternalId));
    }

    ElectronicOrder resolveSourceOrder(Sample sample) {
        if (sample == null) {
            return null;
        }
        if (!GenericValidator.isBlankOrNull(sample.getClinicalOrderId())) {
            ElectronicOrder order = electronicOrderService.get(sample.getClinicalOrderId());
            if (order != null && !GenericValidator.isBlankOrNull(order.getId())) {
                return order;
            }
        }
        if (!GenericValidator.isBlankOrNull(sample.getReferringId())) {
            List<ElectronicOrder> orders = electronicOrderService
                    .getElectronicOrdersByExternalId(sample.getReferringId().trim());
            if (!orders.isEmpty()) {
                return orders.get(orders.size() - 1);
            }
        }
        return null;
    }

    static String extractBaseExternalId(String externalId) {
        if (GenericValidator.isBlankOrNull(externalId)) {
            return externalId;
        }
        return RECOLLECTION_SUFFIX.matcher(externalId.trim()).replaceFirst("");
    }

    String nextRecollectionExternalId(String baseExternalId) {
        int suffix = 1;
        while (true) {
            String candidate = baseExternalId + "-R" + suffix;
            if (electronicOrderService.getElectronicOrdersByExternalId(candidate).isEmpty()) {
                return candidate;
            }
            suffix++;
        }
    }

    String enrichOrderDataWithSampleType(String sourceData, Sample sample) {
        if (GenericValidator.isBlankOrNull(sourceData) || sample == null
                || GenericValidator.isBlankOrNull(sample.getId())) {
            return sourceData;
        }
        String sampleTypeId = resolveSampleTypeId(sample);
        if (GenericValidator.isBlankOrNull(sampleTypeId)) {
            return sourceData;
        }
        try {
            Task task = FHIR_CONTEXT.newJsonParser().parseResource(Task.class, sourceData);
            boolean updated = false;
            for (Task.ParameterComponent input : task.getInput()) {
                if (input.getType() != null && input.getType().hasCoding()
                        && SAMPLE_TYPE_INPUT_CODE.equals(input.getType().getCodingFirstRep().getCode())) {
                    input.setValue(new StringType(sampleTypeId));
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                Task.ParameterComponent input = task.addInput();
                input.setType(new CodeableConcept(new Coding().setCode(SAMPLE_TYPE_INPUT_CODE)));
                input.setValue(new StringType(sampleTypeId));
            }
            return FHIR_CONTEXT.newJsonParser().encodeResourceToString(task);
        } catch (RuntimeException e) {
            return sourceData;
        }
    }

    String resolveSampleTypeId(Sample sample) {
        List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
        for (SampleItem sampleItem : sampleItems) {
            if (sampleItem != null && !GenericValidator.isBlankOrNull(sampleItem.getTypeOfSampleId())) {
                return sampleItem.getTypeOfSampleId();
            }
        }
        return null;
    }
}
