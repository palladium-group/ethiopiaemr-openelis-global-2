package org.openelisglobal.dataexchange.order.controller;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.openmrs.OpenMrsOrderUuidResolver;
import org.openelisglobal.dataexchange.openmrs.service.OpenMrsPaymentVerificationService;
import org.openelisglobal.dataexchange.order.ElectronicOrderSortOrderCategoryConvertor;
import org.openelisglobal.dataexchange.order.form.ElectronicOrderPaging;
import org.openelisglobal.dataexchange.order.form.ElectronicOrderViewForm;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderDisplayItem;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.statusofsample.service.StatusOfSampleService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RestElectronicOrdersController extends BaseController {
    private static final String SAMPLE_TYPE_INPUT_CODE = "CI0050007AAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final String GROUP_KEY_SEPARATOR = "|";

    private static final String[] ALLOWED_FIELDS = new String[] { "searchType", "searchValue", "startDate", "endDate",
            "testIds", "statusId", "useAllInfo" };

    @Autowired
    private StatusOfSampleService statusOfSampleService;
    @Autowired
    private ElectronicOrderService electronicOrderService;
    @Autowired
    private PatientService patientService;
    @Autowired
    private TestService testService;
    @Autowired
    private PanelService panelService;
    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private FhirUtil fhirUtil;
    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private OpenMrsPaymentVerificationService openMrsPaymentVerificationService;
    @Autowired
    private OpenMrsOrderUuidResolver orderUuidResolver;

    @InitBinder
    public void initBinder(final WebDataBinder webdataBinder) {
        webdataBinder.registerCustomEditor(ElectronicOrder.SortOrder.class,
                new ElectronicOrderSortOrderCategoryConvertor());
        webdataBinder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/rest/ElectronicOrders", method = RequestMethod.GET)
    public ElectronicOrderViewForm showElectronicOrders(HttpServletRequest request,
            @ModelAttribute("form") @Valid ElectronicOrderViewForm form, BindingResult result)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        form.setReferralFacilitySelectionList(
                DisplayListService.getInstance().getList(ListType.REFERRAL_ORGANIZATIONS));
        form.setTestSelectionList(DisplayListService.getInstance().getList(ListType.ORDERABLE_TESTS));
        form.setStatusSelectionList(DisplayListService.getInstance().getList(ListType.ELECTRONIC_ORDER_STATUSES));

        List<ElectronicOrder> electronicOrders;
        List<ElectronicOrderDisplayItem> eOrderDisplayItems = new ArrayList<>();
        ElectronicOrderPaging paging = new ElectronicOrderPaging();
        String requestedPage = request.getParameter("page");
        if (GenericValidator.isBlankOrNull(requestedPage)) {
            if (form.getSearchType() != null) {
                electronicOrders = electronicOrderService.searchForElectronicOrders(form);
                eOrderDisplayItems = convertToDisplayItem(electronicOrders, form.getUseAllInfo());
                if (ConfigurationProperties.getInstance()
                        .isPropertyValueEqual(Property.PER_ANALYSIS_REFERRING_SERVICE_REQUEST_FHIR, "true")) {
                    eOrderDisplayItems = groupElectronicOrders(eOrderDisplayItems);
                } else {
                    for (ElectronicOrderDisplayItem item : eOrderDisplayItems) {
                        item.setGroupSize(1);
                        List<String> singleExternal = new ArrayList<>();
                        if (!GenericValidator.isBlankOrNull(item.getExternalOrderId())) {
                            singleExternal.add(item.getExternalOrderId());
                        }
                        item.setGroupExternalOrderIds(singleExternal);
                    }
                }
                applySampleTypeDisplayValues(eOrderDisplayItems);
                paging.setDatabaseResults(request, form, eOrderDisplayItems);
            }
        } else {
            int requestedPageNumber = Integer.parseInt(requestedPage);
            // Sets the requested page in the response.
            paging.page(request, form, requestedPageNumber);
        }
        form.setSearchFinished(true);
        return form;
    }

    private List<ElectronicOrderDisplayItem> convertToDisplayItem(List<ElectronicOrder> electronicOrders,
            boolean useAllInfo) {
        return electronicOrders.stream().map(e -> convertToDisplayItem(e, useAllInfo)).collect(Collectors.toList());
    }

    private ElectronicOrderDisplayItem convertToDisplayItem(ElectronicOrder electronicOrder, boolean useAllInfo) {
        ElectronicOrderDisplayItem displayItem = new ElectronicOrderDisplayItem();

        try {

            displayItem.setStatus(statusOfSampleService.get(electronicOrder.getStatusId()).getDefaultLocalizedName());
            displayItem.setStatusId(electronicOrder.getStatusId());
            displayItem.setElectronicOrderId(electronicOrder.getId());
            displayItem.setExternalOrderId(electronicOrder.getExternalId());
            displayItem.setPriority(electronicOrder.getPriority());

            Patient patient = electronicOrder.getPatient();
            if (patient != null) {
                displayItem.setPatientId(patient.getId());
                displayItem.setSubjectNumber(patientService.getSubjectNumber(patient));
                displayItem.setPatientLastName(patient.getPerson().getLastName());
                displayItem.setPatientFirstName(patient.getPerson().getFirstName());
                displayItem.setPatientNationalId(patient.getNationalId());
            } else {
                String errorMsg = "error in data collection - Patient was a null resource";
                displayItem.setWarnings(Arrays.asList(errorMsg));
            }
            Task task = fhirUtil.getFhirParser().parseResource(Task.class, electronicOrder.getData());
            displayItem.setRequestDateDisplay(DateUtil.formatDateAsText(task.getAuthoredOn()));
            displayItem.setSampleType(getSampleTypeFromTask(task));

            Organization organization = organizationService.getOrganizationByFhirId(
                    task.getRestriction().getRecipientFirstRep().getReferenceElement().getIdPart());
            if (organization != null) {
                displayItem.setRequestingFacility(organization.getOrganizationName());
            } else {
                if (!task.getLocation().isEmpty()) {
                    organization = organizationService
                            .getOrganizationByFhirId(task.getLocation().getReferenceElement().getIdPart());
                    if (organization != null) {
                        displayItem.setRequestingFacility(organization.getOrganizationName());
                    }
                }
            }

            Sample sample = sampleService.getSampleByReferringId(electronicOrder.getExternalId());
            if (sample != null) {
                displayItem.setLabNumber(sample.getAccessionNumber());
            }

            IGenericClient fhirClient = fhirUtil.getFhirClient(fhirConfig.getLocalFhirStorePath());
            String serviceRequestId = orderUuidResolver.resolve(electronicOrder);
            if (GenericValidator.isBlankOrNull(serviceRequestId)) {
                openMrsPaymentVerificationService.applyPaymentStatusToDisplayItem(displayItem, electronicOrder);
            } else {
                ServiceRequest serviceRequest = fhirClient.read().resource(ServiceRequest.class)
                        .withId(serviceRequestId).execute();
                if (serviceRequest.hasEncounter() && serviceRequest.getEncounter().hasReferenceElement()) {
                    displayItem.setEncounterId(serviceRequest.getEncounter().getReferenceElement().getIdPart());
                }
                // Appointment-based orders (OpenMRS priority "On scheduled date") carry the
                // scheduled/collection date in ServiceRequest.occurrencePeriod.start (=
                // TestOrder.effectiveStartDate). Flag it as upcoming when that date is a future
                // calendar day so collectors can distinguish it from collect-now orders.
                if (serviceRequest.hasOccurrencePeriod() && serviceRequest.getOccurrencePeriod().hasStart()) {
                    Date scheduledStart = serviceRequest.getOccurrencePeriod().getStart();
                    if (isUpcoming(scheduledStart)) {
                        displayItem.setUpcoming(true);
                        displayItem.setScheduledDate(DateUtil.formatDateAsText(scheduledStart));
                    }
                }
                if (GenericValidator.isBlankOrNull(displayItem.getSampleType())) {
                    String typeIdFromOrder = resolveTypeOfSampleIdFromServiceRequest(serviceRequest);
                    if (!GenericValidator.isBlankOrNull(typeIdFromOrder)) {
                        displayItem.setSampleType(typeIdFromOrder);
                    }
                }
                openMrsPaymentVerificationService.applyPaymentStatusToDisplayItem(displayItem, electronicOrder,
                        serviceRequest);
                if (useAllInfo) {

                    if (serviceRequest.getRequisition() != null) {
                        displayItem.setReferringLabNumber(serviceRequest.getRequisition().getValue());
                    }

                    Test test = null;
                    for (Coding coding : serviceRequest.getCode().getCoding()) {
                        if (coding.hasSystem()) {
                            if (coding.getSystem().equalsIgnoreCase("http://loinc.org")) {
                                List<Test> tests = testService.getActiveTestsByLoinc(coding.getCode());
                                if (tests.size() != 0) {
                                    test = tests.get(0);
                                    break;
                                }
                            }
                        }
                    }
                    if (test != null) {
                        displayItem.setTestName(test.getLocalizedTestName().getLocalizedValue());
                    }

                    String patientUuid = serviceRequest.getSubject().getReferenceElement().getIdPart();
                    org.hl7.fhir.r4.model.Patient fhirPatient = fhirClient.read()
                            .resource(org.hl7.fhir.r4.model.Patient.class).withId(patientUuid).execute();

                    for (Identifier identifier : fhirPatient.getIdentifier()) {
                        if ("passport".equals(identifier.getSystem())) {
                            displayItem.setPassportNumber(identifier.getId());
                        }
                        if ((fhirConfig.getOeFhirSystem() + "/pat_subjectNumber").equals(identifier.getSystem())) {
                            displayItem.setSubjectNumber(identifier.getId());
                        }
                    }
                }
            }
        } catch (ResourceNotFoundException e) {
            String errorMsg = "error in data collection - FHIR resource not found";
            displayItem.setWarnings(Arrays.asList(errorMsg));
            openMrsPaymentVerificationService.applyPaymentStatusToDisplayItem(displayItem, electronicOrder);
            LogEvent.logError(e);
        } catch (NullPointerException e) {
            String errorMsg = "error in data collection - null data";
            displayItem.setWarnings(Arrays.asList(errorMsg));
            openMrsPaymentVerificationService.applyPaymentStatusToDisplayItem(displayItem, electronicOrder);
            LogEvent.logError(e);
        } catch (RuntimeException e) {
            String errorMsg = "error in data collection - unknown exception";
            displayItem.setWarnings(Arrays.asList(errorMsg));
            openMrsPaymentVerificationService.applyPaymentStatusToDisplayItem(displayItem, electronicOrder);
            LogEvent.logError(e);
        }

        return displayItem;
    }

    /** True when the scheduled/appointment date falls on a future calendar day. */
    private boolean isUpcoming(Date scheduledStart) {
        if (scheduledStart == null) {
            return false;
        }
        LocalDate scheduledDay = scheduledStart.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return scheduledDay.isAfter(LocalDate.now());
    }

    private String getSampleTypeFromTask(Task task) {
        if (task == null || task.getInput() == null) {
            return "";
        }
        return task.getInput().stream()
                .filter(input -> input.getType() != null && input.getType().hasCoding()
                        && SAMPLE_TYPE_INPUT_CODE.equals(input.getType().getCodingFirstRep().getCode())
                        && input.getValue() != null)
                .map(input -> input.getValue().primitiveValue()).findFirst().orElse("");
    }

    private String resolveSampleTypeDisplayValue(String sampleTypeIdOrName) {
        if (GenericValidator.isBlankOrNull(sampleTypeIdOrName)) {
            return "";
        }
        String displayName = typeOfSampleService.getTypeOfSampleNameForId(sampleTypeIdOrName);
        if (!GenericValidator.isBlankOrNull(displayName)) {
            return displayName;
        }
        TypeOfSample sampleType = typeOfSampleService.getTypeOfSampleById(sampleTypeIdOrName);
        if (sampleType != null) {
            if (!GenericValidator.isBlankOrNull(sampleType.getLocalizedName())) {
                return sampleType.getLocalizedName();
            }
            if (!GenericValidator.isBlankOrNull(sampleType.getDescription())) {
                return sampleType.getDescription();
            }
        }
        return sampleTypeIdOrName;
    }

    private void applySampleTypeDisplayValues(List<ElectronicOrderDisplayItem> items) {
        for (ElectronicOrderDisplayItem item : items) {
            item.setSampleType(resolveSampleTypeDisplayValue(item.getSampleType()));
        }
    }

    /**
     * When Task.input does not carry the legacy sample-type concept, derive
     * OpenELIS type-of-sample id from the ordered panel or test (LOINC on
     * ServiceRequest), matching {@code TaskInterpreterImpl} resolution order.
     */
    private String resolveTypeOfSampleIdFromServiceRequest(ServiceRequest serviceRequest) {
        if (serviceRequest == null || !serviceRequest.hasCode() || !serviceRequest.getCode().hasCoding()) {
            return "";
        }
        for (Coding coding : serviceRequest.getCode().getCoding()) {
            if (!coding.hasSystem() || !"http://loinc.org".equalsIgnoreCase(coding.getSystem()) || !coding.hasCode()) {
                continue;
            }
            String loinc = coding.getCode();
            if (GenericValidator.isBlankOrNull(loinc)) {
                continue;
            }
            Panel panel = panelService.getPanelByLoincCode(loinc);
            if (panel != null) {
                List<TypeOfSample> types = typeOfSampleService.getTypeOfSampleForPanelId(panel.getId());
                if (types != null && !types.isEmpty()) {
                    return types.get(0).getId();
                }
            }
            List<Test> tests = testService.getActiveTestsByLoinc(loinc);
            if (tests == null || tests.isEmpty()) {
                tests = testService.getTestsByLoincCode(loinc);
            }
            if (tests != null && !tests.isEmpty()) {
                TypeOfSample typeOfSample = testService.getTypeOfSample(tests.get(0));
                if (typeOfSample != null) {
                    return typeOfSample.getId();
                }
            }
        }
        return "";
    }

    private List<ElectronicOrderDisplayItem> groupElectronicOrders(
            List<ElectronicOrderDisplayItem> eOrderDisplayItems) {
        Map<String, ElectronicOrderDisplayItem> groupedItems = new LinkedHashMap<>();

        for (ElectronicOrderDisplayItem item : eOrderDisplayItems) {
            if (!isGroupable(item)) {
                item.setGroupSize(1);
                item.setGroupExternalOrderIds(new ArrayList<>(List.of(item.getExternalOrderId())));
                groupedItems.put("single:" + normalize(item.getElectronicOrderId()), item);
                continue;
            }

            String groupKey = buildGroupKey(item);
            if (!groupedItems.containsKey(groupKey)) {
                item.setGroupSize(1);
                item.setGroupExternalOrderIds(new ArrayList<>(List.of(item.getExternalOrderId())));
                groupedItems.put(groupKey, item);
                continue;
            }

            ElectronicOrderDisplayItem groupedItem = groupedItems.get(groupKey);
            groupedItem.setGroupSize(groupedItem.getGroupSize() + 1);

            List<String> groupOrderIds = groupedItem.getGroupExternalOrderIds();
            if (!groupOrderIds.contains(item.getExternalOrderId())) {
                groupOrderIds.add(item.getExternalOrderId());
            }

            if (!GenericValidator.isBlankOrNull(item.getTestName())) {
                if (GenericValidator.isBlankOrNull(groupedItem.getTestName())) {
                    groupedItem.setTestName(item.getTestName());
                } else if (!Arrays.asList(groupedItem.getTestName().split(", ")).contains(item.getTestName())) {
                    groupedItem.setTestName(groupedItem.getTestName() + ", " + item.getTestName());
                }
            }
        }

        return new ArrayList<>(groupedItems.values());
    }

    private String buildGroupKey(ElectronicOrderDisplayItem item) {
        return normalize(item.getPatientId()) + GROUP_KEY_SEPARATOR + normalize(item.getEncounterId())
                + GROUP_KEY_SEPARATOR + normalize(item.getSampleType()) + GROUP_KEY_SEPARATOR
                + normalize(item.getStatusId());
    }

    private boolean isGroupable(ElectronicOrderDisplayItem item) {
        return item != null && !GenericValidator.isBlankOrNull(item.getPatientId())
                && !GenericValidator.isBlankOrNull(item.getEncounterId())
                && !GenericValidator.isBlankOrNull(item.getSampleType())
                && !GenericValidator.isBlankOrNull(item.getStatusId());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "electronicOrderViewDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return "eorder.browse.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "eorder.browse.title";
    }
}
