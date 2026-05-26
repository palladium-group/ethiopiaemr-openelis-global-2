package org.openelisglobal.reception.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.ExternalOrderStatus;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderType;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.reception.dto.RecollectionOrderResult;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ElectronicOrderRecollectionServiceImpl implements ElectronicOrderRecollectionService {

    static final Pattern RECOLLECTION_SUFFIX = Pattern.compile("-R\\d+$");

    @Autowired
    private ElectronicOrderService electronicOrderService;

    @Autowired
    private IStatusService statusService;

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
        newOrder.setData(sourceOrder.getData());
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
}
