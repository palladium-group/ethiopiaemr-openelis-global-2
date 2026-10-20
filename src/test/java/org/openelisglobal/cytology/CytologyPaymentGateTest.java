package org.openelisglobal.cytology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dataexchange.openmrs.OpenMrsPaymentStatus;
import org.openelisglobal.program.controller.cytology.CytologySampleForm;
import org.openelisglobal.program.service.ProgramPaymentGateService;
import org.openelisglobal.program.service.cytology.CytologySampleService;
import org.openelisglobal.program.valueholder.cytology.CytologySample.CytologyStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Cytology payment gate: collection is always recorded, but an unpaid OpenMRS
 * order holds the case in AWAITING_PAYMENT until a payment check passes.
 */
public class CytologyPaymentGateTest extends BaseWebContextSensitiveTest {

    private static final int CASE_ID = 3;

    @Autowired
    private CytologySampleService cytologySampleService;
    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;
    private Object serviceTarget;
    private Object originalGate;
    private ProgramPaymentGateService gate;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/cytology.xml");
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update("UPDATE cytology_sample SET status = 'RECEIVED' WHERE id = ?", CASE_ID);

        serviceTarget = AopTestUtils.getUltimateTargetObject(cytologySampleService);
        originalGate = ReflectionTestUtils.getField(serviceTarget, "programPaymentGateService");
        gate = mock(ProgramPaymentGateService.class);
        ReflectionTestUtils.setField(serviceTarget, "programPaymentGateService", gate);
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(serviceTarget, "programPaymentGateService", originalGate);
    }

    @Test
    public void confirmCollection_holdsUnpaidCaseButRecordsCollection() {
        when(gate.checkPayment(any())).thenReturn(OpenMrsPaymentStatus.PENDING);

        OpenMrsPaymentStatus paymentStatus = cytologySampleService.confirmCollection(CASE_ID,
                new CytologySampleForm(), "1");

        assertEquals(OpenMrsPaymentStatus.PENDING, paymentStatus);
        assertEquals(CytologyStatus.AWAITING_PAYMENT, cytologySampleService.get(CASE_ID).getStatus());
        assertEquals(Integer.valueOf(1), jdbcTemplate.queryForObject(
                "SELECT count(*) FROM cytology_sample WHERE id = ? AND collection_confirmed_at IS NOT NULL",
                Integer.class, CASE_ID));
    }

    @Test
    public void confirmCollection_movesPaidSmearCaseToStaining() {
        when(gate.checkPayment(any())).thenReturn(OpenMrsPaymentStatus.PAID);

        cytologySampleService.confirmCollection(CASE_ID, new CytologySampleForm(), "1");

        assertEquals(CytologyStatus.STAINING, cytologySampleService.get(CASE_ID).getStatus());
    }

    @Test
    public void checkPayment_movesPaidFluidCaseToCellBlock() {
        jdbcTemplate.update("UPDATE cytology_sample SET subtype = 'FLUID' WHERE id = ?", CASE_ID);
        when(gate.checkPayment(any())).thenReturn(OpenMrsPaymentStatus.UNKNOWN);
        cytologySampleService.confirmCollection(CASE_ID, new CytologySampleForm(), "1");
        assertEquals(CytologyStatus.AWAITING_PAYMENT, cytologySampleService.get(CASE_ID).getStatus());

        when(gate.checkPayment(any())).thenReturn(OpenMrsPaymentStatus.PAID);
        assertEquals(OpenMrsPaymentStatus.PAID, cytologySampleService.checkPayment(CASE_ID, "1"));

        assertEquals(CytologyStatus.CELL_BLOCK, cytologySampleService.get(CASE_ID).getStatus());
    }

    @Test
    public void updateWithFormValues_cannotMoveCaseOutOfAwaitingPayment() {
        when(gate.checkPayment(any())).thenReturn(OpenMrsPaymentStatus.PENDING);
        cytologySampleService.confirmCollection(CASE_ID, new CytologySampleForm(), "1");

        CytologySampleForm form = new CytologySampleForm();
        form.setStatus(CytologyStatus.STAINING);
        form.setSystemUserId("1");
        try {
            cytologySampleService.updateWithFormValues(CASE_ID, form);
            fail("expected IllegalArgumentException while awaiting payment");
        } catch (IllegalArgumentException expected) {
            // held
        }
        assertEquals(CytologyStatus.AWAITING_PAYMENT, cytologySampleService.get(CASE_ID).getStatus());
    }
}
