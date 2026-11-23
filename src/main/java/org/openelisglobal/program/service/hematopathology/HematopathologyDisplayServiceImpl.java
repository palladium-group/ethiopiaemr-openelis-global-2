package org.openelisglobal.program.service.hematopathology;

import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.SampleOrderService;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.program.valueholder.hematopathology.HematopathologyCaseViewDisplayItem;
import org.openelisglobal.program.valueholder.hematopathology.HematopathologyDisplayItem;
import org.openelisglobal.program.valueholder.hematopathology.HematopathologySample;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HematopathologyDisplayServiceImpl implements HematopathologyDisplayService {

    @Autowired
    private HematopathologySampleService hematopathologySampleService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private FhirUtil fhirUtil;
    @Autowired
    private OrganizationService organizationService;

    @Override
    @Transactional
    public HematopathologyDisplayItem convertToDisplayItem(Integer hematopathologySampleId) {
        HematopathologySample sample = hematopathologySampleService.get(hematopathologySampleId);
        HematopathologyDisplayItem item = new HematopathologyDisplayItem();
        fillListFields(item, sample, sampleService.getPatient(sample.getSample()));
        return item;
    }

    @Override
    @Transactional
    public HematopathologyCaseViewDisplayItem convertToCaseDisplayItem(Integer hematopathologySampleId) {
        HematopathologySample sample = hematopathologySampleService.get(hematopathologySampleId);
        Patient patient = sampleService.getPatient(sample.getSample());
        HematopathologyCaseViewDisplayItem item = new HematopathologyCaseViewDisplayItem();
        fillListFields(item, sample, patient);

        if (sample.getHematopathologist() != null) {
            item.setAssignedHematopathologistId(sample.getHematopathologist().getId());
        }
        if (sample.getTechnician() != null) {
            item.setAssignedTechnicianId(sample.getTechnician().getId());
        }
        loadQuestionnaire(item, sample);

        SampleOrderItem orderItem = new SampleOrderService(sample.getSample()).getSampleOrderItem();
        item.setReferringFacility(orderItem.getReferringSiteName());
        if (StringUtils.isNotBlank(orderItem.getReferringSiteDepartmentId())) {
            Organization department = organizationService.get(orderItem.getReferringSiteDepartmentId());
            if (department != null) {
                item.setDepartment(department.getOrganizationName());
            }
        }
        if (patient != null) {
            item.setAge(DateUtil.getCurrentAgeForDate(patient.getBirthDate(), DateUtil.getNowAsTimestamp()));
            item.setSex(patient.getGender());
        }

        item.setCollectionDate(sample.getSample().getCollectionDate());
        item.setCollectionNotes(sample.getCollectionNotes());
        item.setSpiculesPresent(sample.getSpiculesPresent());
        item.setSampleAgeHours(sample.getSampleAgeHours());
        item.setRejectionReason(sample.getRejectionReason());
        item.setCollectionConfirmedAt(sample.getCollectionConfirmedAt());
        item.setSmearPreparedAt(sample.getSmearPreparedAt());
        item.setSmearStainedAt(sample.getSmearStainedAt());
        item.setProcessingCompletedAt(sample.getProcessingCompletedAt());
        item.setAcquisitionCompletedAt(sample.getAcquisitionCompletedAt());
        item.setSmearQuality(sample.getSmearQuality());
        item.setPeripheralBloodMorphology(sample.getPeripheralBloodMorphology());
        item.setBoneMarrowMorphology(sample.getBoneMarrowMorphology());
        item.setSpecimenViability(sample.getSpecimenViability());
        item.setAntibodyPanel(sample.getAntibodyPanel());
        item.setGatingFindings(sample.getGatingFindings());
        item.setConclusion(sample.getConclusion());
        item.setComment(sample.getComment());
        return item;
    }

    private void fillListFields(HematopathologyDisplayItem item, HematopathologySample sample, Patient patient) {
        item.setCaseId(sample.getId());
        item.setStatus(sample.getStatus());
        item.setSubtype(sample.getSubtype());
        item.setRequestDate(formatRequestDate(sample.getSample()));
        item.setLabNumber(sample.getSample().getAccessionNumber());
        if (patient != null) {
            item.setPatientPK(patient.getId());
            if (patient.getPerson() != null) {
                item.setFirstName(patient.getPerson().getFirstName());
                item.setLastName(patient.getPerson().getLastName());
            }
        }
        if (sample.getHematopathologist() != null) {
            item.setAssignedHematopathologist(sample.getHematopathologist().getDisplayName());
        }
        if (sample.getTechnician() != null) {
            item.setAssignedTechnician(sample.getTechnician().getDisplayName());
        }
        item.setRequester(resolveRequester(sample));
    }

    /**
     * Both FHIR reads are guarded: a case with no form context (no supportingInfo
     * Observations) has no QuestionnaireResponse and must still open.
     */
    private void loadQuestionnaire(HematopathologyCaseViewDisplayItem item, HematopathologySample sample) {
        if (sample.getProgram() != null && sample.getProgram().getQuestionnaireUUID() != null) {
            try {
                item.setProgramQuestionnaire(fhirUtil.getLocalFhirClient().read().resource(Questionnaire.class)
                        .withId(sample.getProgram().getQuestionnaireUUID().toString()).execute());
            } catch (RuntimeException e) {
                LogEvent.logWarn(getClass().getSimpleName(), "loadQuestionnaire",
                        "could not load program Questionnaire for hematopathology case " + sample.getId());
            }
        }
        if (sample.getQuestionnaireResponseUuid() != null) {
            try {
                item.setProgramQuestionnaireResponse(
                        fhirUtil.getLocalFhirClient().read().resource(QuestionnaireResponse.class)
                                .withId(sample.getQuestionnaireResponseUuid().toString()).execute());
            } catch (RuntimeException e) {
                LogEvent.logWarn(getClass().getSimpleName(), "loadQuestionnaire",
                        "could not load QuestionnaireResponse for hematopathology case " + sample.getId());
            }
        }
    }

    private String resolveRequester(HematopathologySample sample) {
        try {
            SampleOrderItem orderItem = new SampleOrderService(sample.getSample()).getSampleOrderItem();
            String requester = ((orderItem.getProviderLastName() == null ? "" : orderItem.getProviderLastName()) + " "
                    + (orderItem.getProviderFirstName() == null ? "" : orderItem.getProviderFirstName())).trim();
            return StringUtils.isBlank(requester) ? null : requester;
        } catch (RuntimeException e) {
            LogEvent.logWarn(getClass().getSimpleName(), "resolveRequester",
                    "could not resolve requesting physician for hematopathology case " + sample.getId() + ": "
                            + e.getMessage());
            return null;
        }
    }

    private String formatRequestDate(Sample sample) {
        if (sample == null) {
            return null;
        }
        String display = sample.getEnteredDateForDisplay();
        if (StringUtils.isNotBlank(display)) {
            return display;
        }
        return sample.getEnteredDate() != null ? DateUtil.formatDateAsText(sample.getEnteredDate()) : null;
    }
}
