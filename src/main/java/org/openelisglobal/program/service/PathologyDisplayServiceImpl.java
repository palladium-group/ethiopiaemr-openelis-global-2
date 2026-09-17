package org.openelisglobal.program.service;

import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.SampleOrderService;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.program.valueholder.pathology.PathologyCaseViewDisplayItem;
import org.openelisglobal.program.valueholder.pathology.PathologyCaseViewDisplayItem.RequestDisplayBean;
import org.openelisglobal.program.valueholder.pathology.PathologyConclusion;
import org.openelisglobal.program.valueholder.pathology.PathologyConclusion.ConclusionType;
import org.openelisglobal.program.valueholder.pathology.PathologyDisplayItem;
import org.openelisglobal.program.valueholder.pathology.PathologyRequest.RequestType;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.program.valueholder.pathology.PathologyTechnique.TechniqueType;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PathologyDisplayServiceImpl implements PathologyDisplayService {

    @Autowired
    private SampleService sampleService;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private PathologySampleService pathologySampleService;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private FhirUtil fhirUtil;
    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private ProviderService providerService;

    @Override
    @Transactional
    public PathologyDisplayItem convertToDisplayItem(Integer pathologySampleId) {
        PathologySample pathologySample = pathologySampleService.get(pathologySampleId);
        PathologyDisplayItem displayItem = new PathologyDisplayItem();
        displayItem.setStatus(pathologySample.getStatus());
        displayItem.setRequestDate(pathologySample.getSample().getEnteredDate());
        if (pathologySample.getPathologist() != null) {
            displayItem.setAssignedPathologist(pathologySample.getPathologist().getDisplayName());
        }
        if (pathologySample.getTechnician() != null) {
            displayItem.setAssignedTechnician(pathologySample.getTechnician().getDisplayName());
        }
        Patient patient = sampleService.getPatient(pathologySample.getSample());
        displayItem.setFirstName(patient.getPerson().getFirstName());
        displayItem.setLastName(patient.getPerson().getLastName());
        displayItem.setLabNumber(pathologySample.getSample().getAccessionNumber());
        displayItem.setPathologySampleId(pathologySample.getId());
        displayItem.setPatientPK(patient.getId());
        displayItem.setRequester(resolveRequesterName(pathologySample.getSample()));

        return displayItem;
    }

    /**
     * Requesting physician for Reception: sample_requester first, then SampleHuman.provider,
     * then ServiceRequest.requester via FHIR (covers program imports that predate requester
     * persistence).
     */
    private String resolveRequesterName(Sample sample) {
        try {
            SampleOrderService sampleOrderService = new SampleOrderService(sample);
            SampleOrderItem sampleItem = sampleOrderService.getSampleOrderItem();
            String requester = ((sampleItem.getProviderLastName() == null ? "" : sampleItem.getProviderLastName())
                    + " " + (sampleItem.getProviderFirstName() == null ? "" : sampleItem.getProviderFirstName()))
                    .trim();
            if (StringUtils.isNotBlank(requester)) {
                return requester;
            }
        } catch (RuntimeException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "resolveRequesterName",
                    "SampleOrderService could not resolve requester for sample " + sample.getId());
        }

        try {
            Provider provider = sampleHumanService.getProviderForSample(sample);
            String fromProvider = formatProviderName(provider);
            if (StringUtils.isNotBlank(fromProvider)) {
                return fromProvider;
            }
        } catch (RuntimeException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "resolveRequesterName",
                    "SampleHuman provider lookup failed for sample " + sample.getId());
        }

        return resolveRequesterNameFromFhir(sample);
    }

    private String formatProviderName(Provider provider) {
        if (provider == null || provider.getPerson() == null) {
            return "";
        }
        Person person = provider.getPerson();
        return ((person.getLastName() == null ? "" : person.getLastName()) + " "
                + (person.getFirstName() == null ? "" : person.getFirstName())).trim();
    }

    private String resolveRequesterNameFromFhir(Sample sample) {
        if (sample == null || GenericValidator.isBlankOrNull(sample.getReferringId())) {
            return "";
        }
        try {
            ServiceRequest serviceRequest = readServiceRequest(sample.getReferringId());
            if (serviceRequest == null || !serviceRequest.hasRequester() || GenericValidator
                    .isBlankOrNull(serviceRequest.getRequester().getReferenceElement().getIdPart())) {
                return "";
            }
            String practitionerId = serviceRequest.getRequester().getReferenceElement().getIdPart();
            Provider provider = providerService.getProviderByFhirId(UUID.fromString(practitionerId));
            String fromProvider = formatProviderName(provider);
            if (StringUtils.isNotBlank(fromProvider)) {
                return fromProvider;
            }
            Practitioner practitioner = readPractitioner(practitionerId);
            if (practitioner != null && practitioner.hasName()) {
                String family = practitioner.getNameFirstRep().getFamily();
                String given = practitioner.getNameFirstRep().hasGiven()
                        ? practitioner.getNameFirstRep().getGivenAsSingleString()
                        : "";
                return ((family == null ? "" : family) + " " + given).trim();
            }
        } catch (RuntimeException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "resolveRequesterNameFromFhir",
                    "could not resolve FHIR requester for sample " + sample.getId() + ": " + e.getMessage());
        }
        return "";
    }

    private ServiceRequest readServiceRequest(String id) {
        try {
            return fhirUtil.getLocalFhirClient().read().resource(ServiceRequest.class).withId(id).execute();
        } catch (RuntimeException localEx) {
            if (fhirConfig.getRemoteStorePaths() != null) {
                for (String remotePath : fhirConfig.getRemoteStorePaths()) {
                    try {
                        return fhirUtil.getFhirClient(remotePath).read().resource(ServiceRequest.class).withId(id)
                                .execute();
                    } catch (RuntimeException ignore) {
                        // try next remote store
                    }
                }
            }
            throw localEx;
        }
    }

    private Practitioner readPractitioner(String id) {
        try {
            return fhirUtil.getLocalFhirClient().read().resource(Practitioner.class).withId(id).execute();
        } catch (RuntimeException localEx) {
            if (fhirConfig.getRemoteStorePaths() != null) {
                for (String remotePath : fhirConfig.getRemoteStorePaths()) {
                    try {
                        return fhirUtil.getFhirClient(remotePath).read().resource(Practitioner.class).withId(id)
                                .execute();
                    } catch (RuntimeException ignore) {
                        // try next remote store
                    }
                }
            }
            return null;
        }
    }

    @Override
    @Transactional
    public PathologyCaseViewDisplayItem convertToCaseDisplayItem(Integer pathologySampleId) {
        PathologySample pathologySample = pathologySampleService.get(pathologySampleId);
        PathologyCaseViewDisplayItem displayItem = new PathologyCaseViewDisplayItem();
        displayItem.setStatus(pathologySample.getStatus());
        displayItem.setRequestDate(pathologySample.getSample().getEnteredDate());
        if (pathologySample.getPathologist() != null) {
            displayItem.setAssignedPathologist(pathologySample.getPathologist().getDisplayName());
            displayItem.setAssignedPathologistId(pathologySample.getPathologist().getId());
        }
        if (pathologySample.getTechnician() != null) {
            displayItem.setAssignedTechnician(pathologySample.getTechnician().getDisplayName());
            displayItem.setAssignedTechnicianId(pathologySample.getTechnician().getId());
        }
        pathologySample.getBlocks().size();
        pathologySample.getSlides().size();
        displayItem.setBlocks(pathologySample.getBlocks());
        displayItem.setSlides(pathologySample.getSlides());
        pathologySample.getReports().size();
        if (pathologySample.getReports() != null) {
            displayItem.setReports(pathologySample.getReports());
        }
        Patient patient = sampleService.getPatient(pathologySample.getSample());
        displayItem.setFirstName(patient.getPerson().getFirstName());
        displayItem.setLastName(patient.getPerson().getLastName());
        displayItem.setLabNumber(pathologySample.getSample().getAccessionNumber());
        displayItem.setPathologySampleId(pathologySample.getId());
        displayItem.setPatientPK(patient.getId());
        // Guard both FHIR reads: a program without a questionnaire, or a sample whose
        // questionnaire response is not (yet) in the local store, must not crash the
        // case view.
        if (pathologySample.getProgram() != null && pathologySample.getProgram().getQuestionnaireUUID() != null) {
            try {
                displayItem.setProgramQuestionnaire(fhirUtil.getLocalFhirClient().read().resource(Questionnaire.class)
                        .withId(pathologySample.getProgram().getQuestionnaireUUID().toString()).execute());
            } catch (RuntimeException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "convertToCaseDisplayItem",
                        "could not load program Questionnaire for pathology sample " + pathologySampleId);
            }
        }
        if (pathologySample.getQuestionnaireResponseUuid() != null) {
            try {
                displayItem.setProgramQuestionnaireResponse(
                        fhirUtil.getLocalFhirClient().read().resource(QuestionnaireResponse.class)
                                .withId(pathologySample.getQuestionnaireResponseUuid().toString()).execute());
            } catch (RuntimeException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "convertToCaseDisplayItem",
                        "could not load QuestionnaireResponse for pathology sample " + pathologySampleId);
            }
        }

        displayItem.setGrossExam(pathologySample.getGrossExam());
        displayItem.setMicroscopyExam(pathologySample.getMicroscopyExam());

        displayItem.setConclusions(
                pathologySample.getConclusions().stream().filter(e -> e.getType() == ConclusionType.DICTIONARY)
                        .map(e -> new IdValuePair(e.getValue(), dictionaryService.get(e.getValue()).getLocalizedName()))
                        .collect(Collectors.toList()));
        Optional<PathologyConclusion> conclusion = pathologySample.getConclusions().stream()
                .filter(e -> e.getType() == ConclusionType.TEXT).findFirst();
        if (conclusion.isPresent())
            displayItem.setConclusionText(conclusion.get().getValue());

        displayItem.setConclusions(
                pathologySample.getConclusions().stream().filter(e -> e.getType() == ConclusionType.DICTIONARY)
                        .map(e -> new IdValuePair(e.getValue(), dictionaryService.get(e.getValue()).getLocalizedName()))
                        .collect(Collectors.toList()));
        displayItem.setTechniques(
                pathologySample.getTechniques().stream().filter(e -> e.getType() == TechniqueType.DICTIONARY)
                        .map(e -> new IdValuePair(e.getValue(), dictionaryService.get(e.getValue()).getLocalizedName()))
                        .collect(Collectors.toList()));
        displayItem.setRequests(pathologySample.getRequests().stream()
                .filter(e -> e.getType() == RequestType.DICTIONARY).map(e -> new RequestDisplayBean(e.getValue(),
                        dictionaryService.get(e.getValue()).getLocalizedName(), e.getStatus()))
                .collect(Collectors.toList()));

        SampleOrderService sampleOrderService = new SampleOrderService(pathologySample.getSample());
        SampleOrderItem sampleItem = sampleOrderService.getSampleOrderItem();
        displayItem.setReferringFacility(sampleItem.getReferringSiteName());
        if (StringUtils.isNotBlank(sampleItem.getReferringSiteDepartmentId())) {
            Organization org = organizationService.get(sampleItem.getReferringSiteDepartmentId());
            if (org != null) {
                displayItem.setDepartment(org.getOrganizationName());
            }
        }
        displayItem.setRequester(resolveRequesterName(pathologySample.getSample()));
        displayItem.setAge(DateUtil.getCurrentAgeForDate(patient.getBirthDate(), DateUtil.getNowAsTimestamp()));
        displayItem.setSex(patient.getGender());
        return displayItem;
    }

    @Override
    @Transactional
    public PathologySample getPathologySampleWithLoadedAtttributes(Integer pathologySampleId) {
        PathologySample pathologySample = pathologySampleService.get(pathologySampleId);
        pathologySample.getBlocks().size();
        pathologySample.getSlides().size();
        pathologySample.getConclusions().size();
        return pathologySample;
    }
}
