package org.openelisglobal.resultvalidation.service;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.notification.service.TestNotificationService;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.referral.service.ReferralResultService;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.service.ResultSignatureService;
import org.openelisglobal.resultvalidation.bean.AnalysisItem;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.testresult.service.TestResultService;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class ResultValidationServiceImplTest {

    @Mock
    private AnalysisService analysisService;
    @Mock
    private ResultService resultService;
    @Mock
    private NoteService noteService;
    @Mock
    private SampleService sampleService;
    @Mock
    private TestNotificationService testNotificationService;
    @Mock
    private AnalyzerService analyzerService;
    @Mock
    private AnalyzerTestMappingService analyzerTestMappingService;
    @Mock
    private PanelService panelService;
    @Mock
    private IStatusService statusService;
    @Mock
    private DefaultConfigurationProperties configurationProperties;
    @Mock
    private ResultSignatureService resultSignatureService;
    @Mock
    private ReferralResultService referralResultService;
    @Mock
    private TestResultService testResultService;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private AutowireCapableBeanFactory autowireCapableBeanFactory;

    @InjectMocks
    private ResultValidationServiceImpl resultValidationService;

    private static final String CBC_PANEL_ID = "1";
    private static final String ACCESSION_NUMBER = "12345";
    private static final String SYS_USER_ID = "1";

    private Object oldFactory;
    private Object oldContext;

    @Before
    public void setUp() {
        // Save old SpringContext state to prevent pollution
        oldFactory = ReflectionTestUtils.getField(SpringContext.class, "factory");
        oldContext = ReflectionTestUtils.getField(SpringContext.class, "context");

        // Mock SpringContext factory to return our mocks
        ReflectionTestUtils.setField(SpringContext.class, "factory", autowireCapableBeanFactory);
        ReflectionTestUtils.setField(SpringContext.class, "context", applicationContext);

        lenient().when(autowireCapableBeanFactory.getBean(IStatusService.class)).thenReturn(statusService);
        lenient().when(autowireCapableBeanFactory.getBean(DefaultConfigurationProperties.class))
                .thenReturn(configurationProperties);
        lenient().when(autowireCapableBeanFactory.getBean(ResultService.class)).thenReturn(resultService);
        lenient().when(autowireCapableBeanFactory.getBean(TestResultService.class)).thenReturn(testResultService);
        lenient().when(autowireCapableBeanFactory.getBean(ResultSignatureService.class))
                .thenReturn(resultSignatureService);
        lenient().when(autowireCapableBeanFactory.getBean(ReferralResultService.class))
                .thenReturn(referralResultService);

        lenient().when(configurationProperties.getPropertyValue(Property.DEFAULT_DATE_LOCALE)).thenReturn("en_US");

        lenient().when(panelService.getIdForPanelName("Complete Blood Count")).thenReturn(CBC_PANEL_ID);
        lenient().when(statusService.getStatusID(AnalysisStatus.NotStarted)).thenReturn("1");
        lenient().when(statusService.getStatusID(AnalysisStatus.TechnicalAcceptance)).thenReturn("2");
        lenient().when(statusService.getStatusID(AnalysisStatus.Finalized)).thenReturn("3");
        lenient().when(statusService.getStatusID(AnalysisStatus.Canceled)).thenReturn("4");
        lenient().when(statusService.getStatusID(AnalysisStatus.NonConforming_depricated)).thenReturn("5");
        lenient().when(statusService.getStatusID(OrderStatus.Finished)).thenReturn("6");

        ReflectionTestUtils.setField(resultValidationService, "cbcPanelName", "Complete Blood Count");
    }

    @After
    public void tearDown() {
        // Restore old SpringContext state
        ReflectionTestUtils.setField(SpringContext.class, "factory", oldFactory);
        ReflectionTestUtils.setField(SpringContext.class, "context", oldContext);
    }

    @Test
    public void testAutoCancelCbcTests_SuccessfulCancellation() {
        AnalysisItem item1 = new AnalysisItem();
        item1.setAccessionNumber(ACCESSION_NUMBER);
        item1.setAnalysisId("A1");
        item1.setTestId("T1");

        AnalysisItem item2 = new AnalysisItem();
        item2.setAccessionNumber(ACCESSION_NUMBER);
        item2.setAnalysisId("A2");
        item2.setTestId("T2");

        List<AnalysisItem> resultItemList = Arrays.asList(item1, item2);

        Analysis analysis1 = new Analysis();
        analysis1.setId("A1");
        Panel panel = new Panel();
        panel.setId(CBC_PANEL_ID);
        analysis1.setPanel(panel);
        analysis1.setStatusId("3");
        org.openelisglobal.test.valueholder.Test test1 = new org.openelisglobal.test.valueholder.Test();
        test1.setId("T1");
        analysis1.setTest(test1);

        Analysis analysis2 = new Analysis();
        analysis2.setId("A2");
        analysis2.setPanel(panel);
        analysis2.setStatusId("3");
        org.openelisglobal.test.valueholder.Test test2 = new org.openelisglobal.test.valueholder.Test();
        test2.setId("T2");
        analysis2.setTest(test2);

        when(analysisService.getAnalysisById("A1")).thenReturn(analysis1);
        when(analysisService.getAnalysisById("A2")).thenReturn(analysis2);

        Analyzer analyzer = new Analyzer();
        analyzer.setId("AZ1");
        analyzer.setStatus(Analyzer.AnalyzerStatus.ACTIVE);
        when(analyzerService.getAllWithTypes()).thenReturn(Arrays.asList(analyzer));

        AnalyzerTestMapping map1 = new AnalyzerTestMapping();
        map1.setTestId("T1");
        AnalyzerTestMapping map2 = new AnalyzerTestMapping();
        map2.setTestId("T2");
        when(analyzerTestMappingService.getAllForAnalyzer("AZ1")).thenReturn(Arrays.asList(map1, map2));

        Sample sample = mock(Sample.class);
        when(sample.getId()).thenReturn("S1");
        when(sampleService.getSampleByAccessionNumber(ACCESSION_NUMBER)).thenReturn(sample);
        when(sampleService.get("S1")).thenReturn(sample);

        Analysis analysis3 = new Analysis();
        analysis3.setId("A3");
        analysis3.setPanel(panel);
        org.openelisglobal.test.valueholder.Test test3 = new org.openelisglobal.test.valueholder.Test();
        test3.setId("T3");
        analysis3.setTest(test3);
        analysis3.setStatusId("1");

        when(analysisService.getAnalysesBySampleId("S1")).thenReturn(Arrays.asList(analysis1, analysis2, analysis3));

        resultValidationService.persistdata(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), resultItemList,
                new ArrayList<>(), new ArrayList<>(), null, new ArrayList<>(), SYS_USER_ID);

        verify(analysisService).updateAnalysises(argThat(list -> list.contains(analysis3) && list.size() == 1),
                anyList(), eq(SYS_USER_ID));
    }

    @Test
    public void testAutoCancelCbcTests_NoMatchingAnalyzer() {
        AnalysisItem item1 = new AnalysisItem();
        item1.setAccessionNumber(ACCESSION_NUMBER);
        item1.setAnalysisId("A1");
        item1.setTestId("T1");

        List<AnalysisItem> resultItemList = Arrays.asList(item1);

        Analysis analysis1 = new Analysis();
        analysis1.setId("A1");
        Panel panel = new Panel();
        panel.setId(CBC_PANEL_ID);
        analysis1.setPanel(panel);
        analysis1.setStatusId("3");
        org.openelisglobal.test.valueholder.Test test1 = new org.openelisglobal.test.valueholder.Test();
        test1.setId("T1");
        analysis1.setTest(test1);

        when(analysisService.getAnalysisById("A1")).thenReturn(analysis1);

        Analyzer analyzer = new Analyzer();
        analyzer.setId("AZ1");
        analyzer.setStatus(Analyzer.AnalyzerStatus.ACTIVE);
        when(analyzerService.getAllWithTypes()).thenReturn(Arrays.asList(analyzer));

        AnalyzerTestMapping map1 = new AnalyzerTestMapping();
        map1.setTestId("T1");
        AnalyzerTestMapping map2 = new AnalyzerTestMapping();
        map2.setTestId("T2");
        when(analyzerTestMappingService.getAllForAnalyzer("AZ1")).thenReturn(Arrays.asList(map1, map2));

        Sample sample = mock(Sample.class);
        when(sample.getId()).thenReturn("S1");
        when(sampleService.getSampleByAccessionNumber(ACCESSION_NUMBER)).thenReturn(sample);
        when(sampleService.get("S1")).thenReturn(sample);

        resultValidationService.persistdata(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), resultItemList,
                new ArrayList<>(), new ArrayList<>(), null, new ArrayList<>(), SYS_USER_ID);

        verify(analysisService, never()).updateAnalysises(anyList(), anyList(), anyString());
    }

    @Test
    public void testAutoCancelCbcTests_IgnoresOtherPanels() {
        AnalysisItem item1 = new AnalysisItem();
        item1.setAccessionNumber(ACCESSION_NUMBER);
        item1.setAnalysisId("A1");
        item1.setTestId("T1");

        List<AnalysisItem> resultItemList = Arrays.asList(item1);

        Analysis analysis1 = new Analysis();
        analysis1.setId("A1");
        Panel panel = new Panel();
        panel.setId("OTHER_PANEL");
        analysis1.setPanel(panel);
        analysis1.setStatusId("3");
        org.openelisglobal.test.valueholder.Test test1 = new org.openelisglobal.test.valueholder.Test();
        test1.setId("T1");
        analysis1.setTest(test1);

        Sample sample = mock(Sample.class);
        when(sample.getId()).thenReturn("S1");
        when(sampleService.getSampleByAccessionNumber(ACCESSION_NUMBER)).thenReturn(sample);
        when(sampleService.get("S1")).thenReturn(sample);

        when(analysisService.getAnalysisById("A1")).thenReturn(analysis1);

        resultValidationService.persistdata(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), resultItemList,
                new ArrayList<>(), new ArrayList<>(), null, new ArrayList<>(), SYS_USER_ID);

        verify(analyzerService, never()).getAllWithTypes();
    }

    @Test
    public void testAutoCancelCbcTests_SkipsInactiveAnalyzers() {
        AnalysisItem item1 = new AnalysisItem();
        item1.setAccessionNumber(ACCESSION_NUMBER);
        item1.setAnalysisId("A1");
        item1.setTestId("T1");
        List<AnalysisItem> resultItemList = Arrays.asList(item1);

        Analysis analysis1 = new Analysis();
        analysis1.setId("A1");
        Panel panel = new Panel();
        panel.setId(CBC_PANEL_ID);
        analysis1.setPanel(panel);
        analysis1.setStatusId("3");
        org.openelisglobal.test.valueholder.Test test1 = new org.openelisglobal.test.valueholder.Test();
        test1.setId("T1");
        analysis1.setTest(test1);
        when(analysisService.getAnalysisById("A1")).thenReturn(analysis1);

        Analyzer analyzer = new Analyzer();
        analyzer.setId("AZ1");
        analyzer.setStatus(Analyzer.AnalyzerStatus.INACTIVE);
        when(analyzerService.getAllWithTypes()).thenReturn(Arrays.asList(analyzer));

        Sample sample = mock(Sample.class);
        when(sample.getId()).thenReturn("S1");
        when(sampleService.getSampleByAccessionNumber(ACCESSION_NUMBER)).thenReturn(sample);
        when(sampleService.get("S1")).thenReturn(sample);

        resultValidationService.persistdata(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), resultItemList,
                new ArrayList<>(), new ArrayList<>(), null, new ArrayList<>(), SYS_USER_ID);

        verify(analyzerTestMappingService, never()).getAllForAnalyzer(anyString());
    }

    @Test
    public void testAutoCancelCbcTests_RespectsTerminalStatuses() {
        AnalysisItem item1 = new AnalysisItem();
        item1.setAccessionNumber(ACCESSION_NUMBER);
        item1.setAnalysisId("A1");
        item1.setTestId("T1");
        List<AnalysisItem> resultItemList = Arrays.asList(item1);

        Analysis analysis1 = new Analysis();
        analysis1.setId("A1");
        Panel panel = new Panel();
        panel.setId(CBC_PANEL_ID);
        analysis1.setPanel(panel);
        analysis1.setStatusId("3");
        org.openelisglobal.test.valueholder.Test test1 = new org.openelisglobal.test.valueholder.Test();
        test1.setId("T1");
        analysis1.setTest(test1);
        when(analysisService.getAnalysisById("A1")).thenReturn(analysis1);

        Analyzer analyzer = new Analyzer();
        analyzer.setId("AZ1");
        analyzer.setStatus(Analyzer.AnalyzerStatus.ACTIVE);
        when(analyzerService.getAllWithTypes()).thenReturn(Arrays.asList(analyzer));
        AnalyzerTestMapping map1 = new AnalyzerTestMapping();
        map1.setTestId("T1");
        when(analyzerTestMappingService.getAllForAnalyzer("AZ1")).thenReturn(Arrays.asList(map1));

        Sample sample = mock(Sample.class);
        when(sample.getId()).thenReturn("S1");
        when(sampleService.getSampleByAccessionNumber(ACCESSION_NUMBER)).thenReturn(sample);
        when(sampleService.get("S1")).thenReturn(sample);

        Analysis analysis3 = new Analysis();
        analysis3.setId("A3");
        analysis3.setPanel(panel);
        org.openelisglobal.test.valueholder.Test test3 = new org.openelisglobal.test.valueholder.Test();
        test3.setId("T3");
        analysis3.setTest(test3);
        analysis3.setStatusId("3");

        when(analysisService.getAnalysesBySampleId("S1")).thenReturn(Arrays.asList(analysis1, analysis3));

        resultValidationService.persistdata(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), resultItemList,
                new ArrayList<>(), new ArrayList<>(), null, new ArrayList<>(), SYS_USER_ID);

        verify(analysisService, never()).updateAnalysises(anyList(), anyList(), anyString());
    }

    @Test
    public void testAutoCancelCbcTests_TriggersSampleFinished() {
        AnalysisItem item1 = new AnalysisItem();
        item1.setAccessionNumber(ACCESSION_NUMBER);
        item1.setAnalysisId("A1");
        item1.setTestId("T1");

        AnalysisItem item2 = new AnalysisItem();
        item2.setAccessionNumber(ACCESSION_NUMBER);
        item2.setAnalysisId("A2");
        item2.setTestId("T2");

        List<AnalysisItem> resultItemList = Arrays.asList(item1, item2);

        Analysis analysis1 = new Analysis();
        analysis1.setId("A1");
        Panel panel = mock(Panel.class);
        when(panel.getId()).thenReturn(CBC_PANEL_ID);
        analysis1.setPanel(panel);
        analysis1.setStatusId("3");
        org.openelisglobal.test.valueholder.Test test1 = new org.openelisglobal.test.valueholder.Test();
        test1.setId("T1");
        analysis1.setTest(test1);

        Analysis analysis2 = new Analysis();
        analysis2.setId("A2");
        analysis2.setPanel(panel);
        analysis2.setStatusId("3");
        org.openelisglobal.test.valueholder.Test test2 = new org.openelisglobal.test.valueholder.Test();
        test2.setId("T2");
        analysis2.setTest(test2);

        when(analysisService.getAnalysisById("A1")).thenReturn(analysis1);
        when(analysisService.getAnalysisById("A2")).thenReturn(analysis2);

        Analyzer analyzer = new Analyzer();
        analyzer.setId("AZ1");
        analyzer.setStatus(Analyzer.AnalyzerStatus.ACTIVE);
        when(analyzerService.getAllWithTypes()).thenReturn(Arrays.asList(analyzer));

        AnalyzerTestMapping map1 = new AnalyzerTestMapping();
        map1.setTestId("T1");
        AnalyzerTestMapping map2 = new AnalyzerTestMapping();
        map2.setTestId("T2");
        when(analyzerTestMappingService.getAllForAnalyzer("AZ1")).thenReturn(Arrays.asList(map1, map2));

        Sample sample = new Sample();
        sample.setId("S1");
        sample.setAccessionNumber(ACCESSION_NUMBER);
        when(sampleService.getSampleByAccessionNumber(ACCESSION_NUMBER)).thenReturn(sample);
        when(sampleService.get("S1")).thenReturn(sample);

        Analysis analysis3 = new Analysis();
        analysis3.setId("A3");
        analysis3.setPanel(panel);
        org.openelisglobal.test.valueholder.Test test3 = new org.openelisglobal.test.valueholder.Test();
        test3.setId("T3");
        analysis3.setTest(test3);
        analysis3.setStatusId("1");

        when(analysisService.getAnalysesBySampleId("S1")).thenReturn(Arrays.asList(analysis1, analysis2, analysis3));

        ArrayList<Sample> sampleUpdateList = new ArrayList<>();
        resultValidationService.persistdata(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), resultItemList,
                sampleUpdateList, new ArrayList<>(), null, new ArrayList<>(), SYS_USER_ID);

        verify(analysisService).updateAnalysises(argThat(list -> list.contains(analysis3)), anyList(), eq(SYS_USER_ID));

        boolean sampleMarkedFinished = sampleUpdateList.stream()
                .anyMatch(s -> s.getId().equals("S1") && "6".equals(s.getStatusId()));
        org.junit.Assert.assertTrue("Sample should be marked as finished", sampleMarkedFinished);
    }

    @Test
    public void testAutoCancelCbcTests_SubsetMatch() {
        AnalysisItem item1 = new AnalysisItem();
        item1.setAccessionNumber(ACCESSION_NUMBER);
        item1.setAnalysisId("A1");
        item1.setTestId("T1");

        AnalysisItem item2 = new AnalysisItem();
        item2.setAccessionNumber(ACCESSION_NUMBER);
        item2.setAnalysisId("A2");
        item2.setTestId("T2");

        List<AnalysisItem> resultItemList = Arrays.asList(item1, item2);

        Panel panel = mock(Panel.class);
        when(panel.getId()).thenReturn(CBC_PANEL_ID);

        Analysis analysis1 = new Analysis();
        analysis1.setId("A1");
        analysis1.setPanel(panel);
        analysis1.setStatusId("3");
        org.openelisglobal.test.valueholder.Test test1 = new org.openelisglobal.test.valueholder.Test();
        test1.setId("T1");
        analysis1.setTest(test1);

        Analysis analysis2 = new Analysis();
        analysis2.setId("A2");
        analysis2.setPanel(panel);
        analysis2.setStatusId("3");
        org.openelisglobal.test.valueholder.Test test2 = new org.openelisglobal.test.valueholder.Test();
        test2.setId("T2");
        analysis2.setTest(test2);

        when(analysisService.getAnalysisById("A1")).thenReturn(analysis1);
        when(analysisService.getAnalysisById("A2")).thenReturn(analysis2);

        Analyzer analyzer = new Analyzer();
        analyzer.setId("AZ1");
        analyzer.setStatus(Analyzer.AnalyzerStatus.ACTIVE);
        when(analyzerService.getAllWithTypes()).thenReturn(Arrays.asList(analyzer));

        AnalyzerTestMapping map1 = new AnalyzerTestMapping();
        map1.setTestId("T1");
        when(analyzerTestMappingService.getAllForAnalyzer("AZ1")).thenReturn(Arrays.asList(map1));

        Sample sample = new Sample();
        sample.setId("S1");
        sample.setAccessionNumber(ACCESSION_NUMBER);
        when(sampleService.getSampleByAccessionNumber(ACCESSION_NUMBER)).thenReturn(sample);
        when(sampleService.get("S1")).thenReturn(sample);

        Analysis analysis3 = new Analysis();
        analysis3.setId("A3");
        analysis3.setPanel(panel);
        org.openelisglobal.test.valueholder.Test test3 = new org.openelisglobal.test.valueholder.Test();
        test3.setId("T3");
        analysis3.setTest(test3);
        analysis3.setStatusId("1");

        when(analysisService.getAnalysesBySampleId("S1")).thenReturn(Arrays.asList(analysis1, analysis2, analysis3));

        ArrayList<Sample> sampleUpdateList = new ArrayList<>();
        resultValidationService.persistdata(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), resultItemList,
                sampleUpdateList, new ArrayList<>(), null, new ArrayList<>(), SYS_USER_ID);

        verify(analysisService).updateAnalysises(argThat(list -> list.size() == 1 && list.contains(analysis3)),
                anyList(), eq(SYS_USER_ID));

        org.junit.Assert.assertEquals("4", analysis3.getStatusId());
    }
}
