package org.openelisglobal.notebook.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.notebook.bean.NoteBookDisplayBean;
import org.openelisglobal.notebook.bean.NoteBookFullDisplayBean;
import org.openelisglobal.notebook.bean.NoteBookFullDisplayBean.ResultDisplayBean;
import org.openelisglobal.notebook.bean.NoteBookFullDisplayBean.SampleDisplayBean;
import org.openelisglobal.notebook.dao.NoteBookDAO;
import org.openelisglobal.notebook.form.NoteBookForm;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBook.NoteBookStatus;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NoteBookServiceImpl extends AuditableBaseObjectServiceImpl<NoteBook, Integer> implements NoteBookService {

    @Autowired
    private NoteBookDAO baseObjectDAO;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    ResultService resultService;

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private SystemUserService systemUserService;

    public NoteBookServiceImpl() {
        super(NoteBook.class);
        this.auditTrailLog = true;
    }

    @Override
    protected BaseDAO<NoteBook, Integer> getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    public List<NoteBook> filterNoteBooks(List<NoteBookStatus> statuses, List<String> types, List<String> tags,
            Date fromDate, Date toDate) {
        return baseObjectDAO.filterNoteBooks(statuses, types, tags, fromDate, toDate);
    }

    @Override
    public void updateWithStatus(Integer notebookId, NoteBookStatus status) {
        Optional<NoteBook> optionalNoteBook = baseObjectDAO.get(notebookId);
        if (optionalNoteBook.isPresent()) {
            NoteBook noteBook = optionalNoteBook.get();
            noteBook.setStatus(status);
            baseObjectDAO.update(noteBook);
        }
    }

    @Override
    public void createWithFormValues(NoteBookForm form, String currentUserId) {
        NoteBook noteBook = new NoteBook();
        noteBook = createNoteBookFromForm(noteBook, form, currentUserId);
        baseObjectDAO.insert(noteBook);
    }

    @Override
    public void updateWithFormValues(Integer noteBookId, NoteBookForm form, String currentUserId) {
        Optional<NoteBook> optionalNoteBook = baseObjectDAO.get(noteBookId);
        if (optionalNoteBook.isPresent()) {
            NoteBook noteBook = optionalNoteBook.get();
            noteBook = createNoteBookFromForm(noteBook, form, currentUserId);
            baseObjectDAO.update(noteBook);
        }
    }

    @Override
    public NoteBookDisplayBean convertToDisplayBean(Integer noteBookId) {
        NoteBookDisplayBean displayBean = new NoteBookDisplayBean();
        Optional<NoteBook> optionalNoteBook = baseObjectDAO.get(noteBookId);
        if (optionalNoteBook.isPresent()) {
            NoteBook noteBook = optionalNoteBook.get();
            Patient patient = patientService.getData(noteBook.getPatient().getId());
            displayBean.setId(noteBook.getId());
            displayBean.setTitle(noteBook.getTitle());
            displayBean.setType(noteBook.getType());
            displayBean.setLastName(patientService.getLastName(patient));
            displayBean.setFirstName(patientService.getFirstName(patient));
            displayBean.setGender(patientService.getGender(patient));
            displayBean.setTags(noteBook.getTags());
            displayBean.setDateCreated(DateUtil.formatDateAsText(noteBook.getDateCreated()));
        }
        return displayBean;
    }

    @Override
    public NoteBookFullDisplayBean convertToFullDisplayBean(Integer noteBookId) {
        NoteBookFullDisplayBean fullDisplayBean = new NoteBookFullDisplayBean();
        Optional<NoteBook> optionalNoteBook = baseObjectDAO.get(noteBookId);
        if (optionalNoteBook.isPresent()) {
            NoteBook noteBook = optionalNoteBook.get();
            Patient patient = patientService.getData(noteBook.getPatient().getId());
            fullDisplayBean.setId(noteBook.getId());
            fullDisplayBean.setTitle(noteBook.getTitle());
            fullDisplayBean.setType(noteBook.getType());
            fullDisplayBean.setLastName(patientService.getLastName(patient));
            fullDisplayBean.setFirstName(patientService.getFirstName(patient));
            fullDisplayBean.setGender(patientService.getGender(patient));
            fullDisplayBean.setTags(noteBook.getTags());
            fullDisplayBean.setDateCreated(DateUtil.formatDateAsText(noteBook.getDateCreated()));
            fullDisplayBean.setContent(noteBook.getContent());
            fullDisplayBean.setObjective(noteBook.getObjective());
            fullDisplayBean.setProtocol(noteBook.getProtocol());
            fullDisplayBean.setProject(noteBook.getProject());
            List<String> instruments = noteBook.getAnalysers().stream().map(analyzer -> analyzer.getName()).toList();
            fullDisplayBean.setInstruments(instruments);
            fullDisplayBean.setPages(noteBook.getPages());
            fullDisplayBean.setFiles(noteBook.getFiles());
            fullDisplayBean.setAssignedTechnician(noteBook.getTechnician().getDisplayName());

            List<Sample> samples = sampleService.getSamplesForPatient(patient.getId());

            List<SampleItem> sampleItems = new ArrayList<>();
            for (Sample sample : samples) {
                List<SampleItem> items = sampleItemService.getSampleItemsBySampleId(sample.getId());
                sampleItems.addAll(items);
            }

            List<SampleDisplayBean> sampleDisplayBeans = new ArrayList<>();

            for (SampleItem sampleItem : sampleItems) {
                SampleDisplayBean displayBean = convertSampleToDisplayBean(sampleItem);
                sampleDisplayBeans.add(displayBean);
            }
            fullDisplayBean.setSamples(sampleDisplayBeans);

        }
        return fullDisplayBean;
    }

    private SampleDisplayBean convertSampleToDisplayBean(SampleItem sampleItem) {
        SampleDisplayBean sampleDisplayBean = new SampleDisplayBean();
        sampleDisplayBean
                .setSampleType(typeOfSampleService.getNameForTypeOfSampleId(sampleItem.getTypeOfSample().getId()));
        sampleDisplayBean.setCollectionDate(DateUtil.convertTimestampToStringDate(sampleItem.getCollectionDate()));

        List<Analysis> analyses = analysisService.getAnalysesBySampleItem(sampleItem);
        List<ResultDisplayBean> resultsDisplayBeans = new ArrayList<>();
        for (Analysis analysis : analyses) {
            List<Result> results = resultService.getResultsByAnalysis(analysis);
            for (Result result : results) {
                ResultDisplayBean resultDisplayBean = new ResultDisplayBean();
                resultDisplayBean.setResult(resultService.getResultValue(result, true));
                resultDisplayBean.setTest(result.getTestResult().getTest().getLocalizedName());
                resultDisplayBean.setDateCreated(DateUtil.convertTimestampToStringDate(result.getLastupdated()));
                resultsDisplayBeans.add(resultDisplayBean);
            }
        }
        sampleDisplayBean.setResults(resultsDisplayBeans);
        return sampleDisplayBean;
    }

    private NoteBook createNoteBookFromForm(NoteBook noteBook, NoteBookForm form, String currentUserId) {

        if (!GenericValidator.isBlankOrNull(form.getTitle())) {
            noteBook.setTitle(form.getTitle());
        }
        if (!GenericValidator.isBlankOrNull(form.getType())) {
            noteBook.setType(form.getType());
        }
        if (form.getTags() != null && !form.getTags().isEmpty()) {
            noteBook.setTags(form.getTags());
        }

        if (form.getPages() != null && !form.getPages().isEmpty()) {
            noteBook.setPages(form.getPages());
        }
        if (!GenericValidator.isBlankOrNull(form.getContent())) {
            noteBook.setContent(form.getContent());
        }
        if (!GenericValidator.isBlankOrNull(form.getObjective())) {
            noteBook.setObjective(form.getObjective());
        }
        if (!GenericValidator.isBlankOrNull(form.getProtocol())) {
            noteBook.setProtocol(form.getProtocol());
        }
        if (!GenericValidator.isBlankOrNull(form.getProject())) {
            noteBook.setProject(form.getProject());
        }

        noteBook.setDateCreated(noteBook.getId() != null ? noteBook.getDateCreated() : new Date());
        noteBook.setStatus(noteBook.getId() != null ? noteBook.getStatus() : NoteBookStatus.DRAFT);
        noteBook.setPatient(patientService.getData((form.getPatientId())));
        noteBook.setTechnician(form.getTechnicianId() != null ? systemUserService.get(form.getTechnicianId())
                : systemUserService.get(currentUserId));
        if (form.getAnalyserIds() != null && !form.getAnalyserIds().isEmpty()) {
            noteBook.setAnalysers(
                    form.getAnalyserIds().stream().map(analyserId -> analyzerService.get(analyserId)).toList());
        }

        if (form.getSampleIds() != null && !form.getSampleIds().isEmpty()) {

            noteBook.setSamples(form.getSampleIds().stream().map(sampleId -> sampleItemService.get(sampleId)).toList());
        }

        noteBook.getFiles().removeAll(noteBook.getFiles());
        if (form.getFiles() != null && !form.getFiles().isEmpty()) {
            form.getFiles().stream().forEach(e -> e.setId(null));
            noteBook.getFiles().addAll(form.getFiles());
        }

        return noteBook;
    }

}
