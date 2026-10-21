package org.openelisglobal.program.service.supply;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.dataexchange.openmrs.rest.OpenMrsRestClient;
import org.openelisglobal.dataexchange.openmrs.rest.OpenMrsRestException;
import org.openelisglobal.program.bean.SupplyCatalogOption;
import org.openelisglobal.program.controller.supply.ProgramSupplyRequestForm;
import org.openelisglobal.program.service.supply.ProgramSupplyOrderException.Code;
import org.openelisglobal.program.valueholder.supply.ProgramSupplyRequest;
import org.openelisglobal.program.valueholder.supply.ProgramSupplyRequest.DaguStatus;
import org.openelisglobal.program.valueholder.supply.ProgramSupplyRequest.Status;
import org.openelisglobal.program.valueholder.supply.ProgramSupplyRequestDisplayItem;
import org.openelisglobal.program.valueholder.supply.ProgramSupplyRequestItem;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Orders medical supplies for a pathology/cytology case by creating an OpenMRS
 * order encounter of {@code medicalsupplyorder} lines, the same shape the O3
 * order basket posts. OpenMRS then queues that encounter for Dagu (custom
 * module prescription outbox → OpenFn) and bills it, unchanged.
 *
 * Deliberately not {@code @Transactional}: each request row is committed before
 * the OpenMRS call, so a slow or failed call never rolls back the record of
 * what was asked for, and a retry reuses the same encounter uuid.
 */
@Service
public class ProgramSupplyOrderService {

    static final Set<String> URGENCIES = Set.of("ROUTINE", "STAT");
    static final int MAX_INSTRUCTIONS = 500;

    @Autowired
    private OpenMrsRestClient openMrsRestClient;
    @Autowired
    private ProgramSupplyRequestService supplyRequestService;
    @Autowired
    private SystemUserService systemUserService;
    @Autowired
    private ConfigurationProperties configurationProperties;

    @Value("${org.openelisglobal.openmrs.supply.ordererProviderUuid:}")
    private String ordererProviderUuid;

    @Value("${org.openelisglobal.openmrs.supply.encounterTypeUuid:39da3525-afe4-45ff-8977-c53b7b359158}")
    private String encounterTypeUuid;

    @Value("${org.openelisglobal.openmrs.supply.careSettingUuid:6f0c9a92-6f24-11e3-af88-005056821db0}")
    private String careSettingUuid;

    @Value("${org.openelisglobal.openmrs.supply.conceptClassUuid:0dcf23d4-3008-4d8e-b12c-4ec95d1cfd97}")
    private String conceptClassUuid;

    @Value("${org.openelisglobal.openmrs.supply.quantityUnitsConceptSetUuid:162402AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA}")
    private String quantityUnitsConceptSetUuid;

    /** Used only when the patient's active visit has no location. */
    @Value("${org.openelisglobal.openmrs.supply.locationUuid:}")
    private String fallbackLocationUuid;

    public boolean isEnabled() {
        return configurationProperties.isPropertyValueEqual(Property.PATHOLOGY_SUPPLY_ORDER_ENABLED, "true");
    }

    public List<SupplyCatalogOption> searchCatalog(String term) {
        requireReady();
        if (GenericValidator.isBlankOrNull(term) || term.trim().length() < 2) {
            return List.of();
        }
        JsonNode body = call(() -> openMrsRestClient.get("conceptsearch?conceptClasses=" + encode(conceptClassUuid)
                + "&q=" + encode(term.trim()) + "&limit=20"));
        List<SupplyCatalogOption> options = new ArrayList<>();
        Map<String, Boolean> seen = new LinkedHashMap<>();
        for (JsonNode result : results(body)) {
            String uuid = result.path("concept").path("uuid").asText(null);
            String display = result.path("concept").path("display").asText(result.path("display").asText(null));
            if (uuid != null && seen.putIfAbsent(uuid, true) == null) {
                options.add(new SupplyCatalogOption(uuid, display));
            }
        }
        return options;
    }

    public List<SupplyCatalogOption> getQuantityUnits() {
        requireReady();
        JsonNode body = call(() -> openMrsRestClient
                .get("concept/" + encode(quantityUnitsConceptSetUuid) + "?v=custom:(setMembers:(uuid,display))"));
        List<SupplyCatalogOption> units = new ArrayList<>();
        if (body != null) {
            for (JsonNode member : body.path("setMembers")) {
                units.add(new SupplyCatalogOption(member.path("uuid").asText(), member.path("display").asText()));
            }
        }
        return units;
    }

    /**
     * Requests for the case, newest first. Dagu outcomes not yet known are
     * refreshed from OpenMRS on a best-effort basis; OpenMRS being down never hides
     * the list.
     */
    public List<ProgramSupplyRequestDisplayItem> listForSample(Sample sample) {
        List<ProgramSupplyRequest> requests = supplyRequestService.getBySampleId(sample.getId());
        refreshDaguOutcomes(requests);
        return requests.stream().map(this::toDisplayItem).collect(Collectors.toList());
    }

    public ProgramSupplyRequestDisplayItem create(Sample sample, boolean caseClosed, ProgramSupplyRequestForm form,
            String sysUserId) {
        requireReady();
        if (caseClosed) {
            throw new ProgramSupplyOrderException(Code.CASE_CLOSED, "Case is completed");
        }
        List<ProgramSupplyRequestItem> items = validItems(form);
        OrderContext context = resolveContext(sample);

        ProgramSupplyRequest request = new ProgramSupplyRequest();
        request.setSample(sample);
        request.setEncounterUuid(UUID.randomUUID().toString());
        request.setStatus(Status.PENDING);
        request.setRequestedBy(systemUserService.get(sysUserId));
        request.setRequestedAt(new Timestamp(System.currentTimeMillis()));
        request.setSysUserId(sysUserId);
        items.forEach(request::addItem);
        request = supplyRequestService.save(request);

        return toDisplayItem(send(request, context, sysUserId));
    }

    /**
     * Re-sends a request that never reached OpenMRS. If OpenMRS already has the
     * encounter (the earlier response was lost) it is only marked sent.
     */
    public ProgramSupplyRequestDisplayItem retry(Integer requestId, String sampleId, boolean caseClosed,
            String sysUserId) {
        requireReady();
        ProgramSupplyRequest request = supplyRequestService.get(requestId);
        if (request == null || request.getSample() == null || !request.getSample().getId().equals(sampleId)) {
            throw new ProgramSupplyOrderException(Code.NOT_FOUND, "Supply request not found");
        }
        if (request.getStatus() == Status.SENT) {
            return toDisplayItem(request);
        }
        if (caseClosed) {
            throw new ProgramSupplyOrderException(Code.CASE_CLOSED, "Case is completed");
        }
        JsonNode existing = call(
                () -> openMrsRestClient.get("encounter/" + encode(request.getEncounterUuid()) + "?v=custom:(uuid)"));
        if (existing != null) {
            markSent(request, sysUserId);
            return toDisplayItem(supplyRequestService.update(request));
        }
        return toDisplayItem(send(request, resolveContext(request.getSample()), sysUserId));
    }

    private ProgramSupplyRequest send(ProgramSupplyRequest request, OrderContext context, String sysUserId) {
        try {
            openMrsRestClient.post("encounter", encounterPayload(request, context));
            markSent(request, sysUserId);
        } catch (OpenMrsRestException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "send",
                    "Supply order " + request.getEncounterUuid() + " not accepted by OpenMRS: " + e.getMessage());
            request.setStatus(Status.FAILED);
            request.setFailureReason(e.getMessage());
            request.setSysUserId(sysUserId);
        }
        return supplyRequestService.update(request);
    }

    private void markSent(ProgramSupplyRequest request, String sysUserId) {
        request.setStatus(Status.SENT);
        request.setFailureReason(null);
        request.setSysUserId(sysUserId);
    }

    Map<String, Object> encounterPayload(ProgramSupplyRequest request, OrderContext context) {
        String requester = request.getRequestedBy() == null ? "" : request.getRequestedBy().getNameForDisplay();
        String accession = request.getSample().getAccessionNumber();
        String origin = "Requested via OpenELIS by " + requester + ", lab no. " + accession;

        List<Map<String, Object>> orders = new ArrayList<>();
        for (ProgramSupplyRequestItem item : request.getItems()) {
            Map<String, Object> order = new LinkedHashMap<>();
            order.put("type", "medicalsupplyorder");
            order.put("action", "NEW");
            order.put("patient", context.patientUuid);
            order.put("careSetting", careSettingUuid);
            order.put("orderer", ordererProviderUuid);
            order.put("concept", item.getConceptUuid());
            order.put("urgency", item.getUrgency());
            order.put("quantity", item.getQuantity());
            if (!GenericValidator.isBlankOrNull(item.getQuantityUnitsUuid())) {
                order.put("quantityUnits", item.getQuantityUnitsUuid());
            }
            order.put("instructions", GenericValidator.isBlankOrNull(item.getInstructions()) ? origin
                    : item.getInstructions() + " (" + origin + ")");
            orders.add(order);
        }

        Map<String, Object> encounter = new LinkedHashMap<>();
        encounter.put("uuid", request.getEncounterUuid());
        encounter.put("patient", context.patientUuid);
        encounter.put("encounterType", encounterTypeUuid);
        encounter.put("location", context.locationUuid);
        encounter.put("visit", context.visitUuid);
        encounter.put("obs", List.of());
        encounter.put("orders", orders);
        return encounter;
    }

    /**
     * OpenMRS patient, active visit and location for the case's referring order.
     */
    OrderContext resolveContext(Sample sample) {
        String orderUuid = sample.getReferringId();
        if (GenericValidator.isBlankOrNull(orderUuid)) {
            throw new ProgramSupplyOrderException(Code.NO_OPENMRS_ORDER, "Case has no OpenMRS order");
        }
        JsonNode order = call(
                () -> openMrsRestClient.get("order/" + encode(orderUuid) + "?v=custom:(uuid,patient:(uuid))"));
        String patientUuid = order == null ? null : order.path("patient").path("uuid").asText(null);
        if (patientUuid == null) {
            throw new ProgramSupplyOrderException(Code.ORDER_NOT_FOUND, "OpenMRS order not found: " + orderUuid);
        }
        JsonNode visits = call(() -> openMrsRestClient.get(
                "visit?patient=" + encode(patientUuid) + "&includeInactive=false&v=custom:(uuid,location:(uuid))"));
        List<JsonNode> active = results(visits);
        if (active.isEmpty()) {
            throw new ProgramSupplyOrderException(Code.NO_ACTIVE_VISIT, "Patient has no active OpenMRS visit");
        }
        JsonNode visit = active.get(0);
        String locationUuid = visit.path("location").path("uuid").asText(null);
        if (GenericValidator.isBlankOrNull(locationUuid)) {
            locationUuid = fallbackLocationUuid;
        }
        if (GenericValidator.isBlankOrNull(locationUuid)) {
            throw new ProgramSupplyOrderException(Code.NO_LOCATION, "No location for the OpenMRS visit");
        }
        return new OrderContext(patientUuid, visit.path("uuid").asText(), locationUuid);
    }

    private List<ProgramSupplyRequestItem> validItems(ProgramSupplyRequestForm form) {
        if (form == null || form.getItems() == null || form.getItems().isEmpty()) {
            throw new ProgramSupplyOrderException(Code.INVALID_ITEMS, "At least one supply is required");
        }
        List<ProgramSupplyRequestItem> items = new ArrayList<>();
        for (ProgramSupplyRequestForm.Item line : form.getItems()) {
            if (GenericValidator.isBlankOrNull(line.getConceptUuid()) || line.getQuantity() == null
                    || line.getQuantity() < 1 || line.getUrgency() == null || !URGENCIES.contains(line.getUrgency())
                    || (line.getInstructions() != null && line.getInstructions().length() > MAX_INSTRUCTIONS)) {
                throw new ProgramSupplyOrderException(Code.INVALID_ITEMS, "Invalid supply line");
            }
            ProgramSupplyRequestItem item = new ProgramSupplyRequestItem();
            item.setConceptUuid(line.getConceptUuid());
            item.setConceptName(line.getConceptName());
            item.setQuantity(line.getQuantity());
            item.setQuantityUnitsUuid(line.getQuantityUnitsUuid());
            item.setQuantityUnitsName(line.getQuantityUnitsName());
            item.setUrgency(line.getUrgency());
            item.setInstructions(
                    GenericValidator.isBlankOrNull(line.getInstructions()) ? null : line.getInstructions().trim());
            items.add(item);
        }
        return items;
    }

    private void refreshDaguOutcomes(List<ProgramSupplyRequest> requests) {
        Map<String, ProgramSupplyRequest> awaiting = new LinkedHashMap<>();
        for (ProgramSupplyRequest request : requests) {
            if (request.getStatus() == Status.SENT && request.getDaguStatus() != DaguStatus.SUCCESS) {
                awaiting.put(request.getEncounterUuid(), request);
            }
        }
        if (awaiting.isEmpty() || !openMrsRestClient.isConfigured()) {
            return;
        }
        try {
            JsonNode body = openMrsRestClient.get("ethiopiaemrcustommodule/prescriptionOutbox/status?encounters="
                    + encode(String.join(",", awaiting.keySet())));
            for (JsonNode outcome : results(body)) {
                ProgramSupplyRequest request = awaiting.get(outcome.path("encounterUuid").asText());
                DaguStatus status = parseDaguStatus(outcome.path("outcomeStatus").asText());
                if (request == null || status == null || (status == request.getDaguStatus()
                        && sameText(outcome.path("reason").asText(null), request.getDaguReason()))) {
                    continue;
                }
                request.setDaguStatus(status);
                request.setDaguReason(status == DaguStatus.FAILED ? outcome.path("reason").asText(null) : null);
                request.setSysUserId(request.getRequestedBy() == null ? "1" : request.getRequestedBy().getId());
                supplyRequestService.update(request);
            }
        } catch (OpenMrsRestException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "refreshDaguOutcomes",
                    "Could not read Dagu outcomes from OpenMRS: " + e.getMessage());
        }
    }

    private static DaguStatus parseDaguStatus(String value) {
        if ("SUCCESS".equals(value)) {
            return DaguStatus.SUCCESS;
        }
        if ("FAILED".equals(value)) {
            return DaguStatus.FAILED;
        }
        return null;
    }

    private static boolean sameText(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    ProgramSupplyRequestDisplayItem toDisplayItem(ProgramSupplyRequest request) {
        ProgramSupplyRequestDisplayItem display = new ProgramSupplyRequestDisplayItem();
        display.setId(request.getId());
        display.setRequestedAt(request.getRequestedAt() == null ? null : request.getRequestedAt().getTime());
        SystemUser requestedBy = request.getRequestedBy();
        display.setRequestedBy(requestedBy == null ? null : requestedBy.getNameForDisplay());
        display.setStatus(request.getStatus() == null ? null : request.getStatus().name());
        display.setFailureReason(request.getFailureReason());
        display.setDaguStatus(request.getDaguStatus() == null ? null : request.getDaguStatus().name());
        display.setDaguReason(request.getDaguReason());
        for (ProgramSupplyRequestItem item : request.getItems()) {
            ProgramSupplyRequestDisplayItem.Item line = new ProgramSupplyRequestDisplayItem.Item();
            line.setConceptName(item.getConceptName());
            line.setQuantity(item.getQuantity());
            line.setQuantityUnitsName(item.getQuantityUnitsName());
            line.setUrgency(item.getUrgency());
            line.setInstructions(item.getInstructions());
            display.getItems().add(line);
        }
        return display;
    }

    private void requireReady() {
        if (!isEnabled()) {
            throw new ProgramSupplyOrderException(Code.NOT_ENABLED, "Supply ordering is not enabled");
        }
        if (!openMrsRestClient.isConfigured() || GenericValidator.isBlankOrNull(ordererProviderUuid)) {
            throw new ProgramSupplyOrderException(Code.NOT_CONFIGURED, "OpenMRS supply ordering is not configured");
        }
    }

    private static JsonNode call(java.util.function.Supplier<JsonNode> request) {
        try {
            return request.get();
        } catch (OpenMrsRestException e) {
            throw new ProgramSupplyOrderException(Code.OPENMRS_UNAVAILABLE, e.getMessage());
        }
    }

    private static List<JsonNode> results(JsonNode body) {
        List<JsonNode> list = new ArrayList<>();
        if (body != null) {
            body.path("results").forEach(list::add);
        }
        return list;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    static final class OrderContext {
        final String patientUuid;
        final String visitUuid;
        final String locationUuid;

        OrderContext(String patientUuid, String visitUuid, String locationUuid) {
            this.patientUuid = patientUuid;
            this.visitUuid = visitUuid;
            this.locationUuid = locationUuid;
        }
    }
}
