package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dataexchange.openmrs.OpenMrsPaymentStatus;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.service.ProgramPaymentGateService;
import org.openelisglobal.program.valueholder.pathology.PathologyBlock;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Pathology payment gate: collection is always recorded, but an unpaid OpenMRS
 * order holds the case in AWAITING_PAYMENT until a payment check passes.
 */
public class PathologyPaymentGateTest extends BaseWebContextSensitiveTest {

    private static final int CASE_ID = 2;
    private static final String USER_ID = "1001";

    @Autowired
    private PathologySampleService pathologySampleService;
    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;
    private Object serviceTarget;
    private Object originalGate;
    private ProgramPaymentGateService gate;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/pathology-sample.xml");
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update("UPDATE pathology_sample SET status = 'RECEIVED' WHERE id = ?", CASE_ID);
        jdbcTemplate.update("UPDATE sample SET collection_date = NULL WHERE id = 2");

        serviceTarget = AopTestUtils.getUltimateTargetObject(pathologySampleService);
        originalGate = ReflectionTestUtils.getField(serviceTarget, "programPaymentGateService");
        gate = mock(ProgramPaymentGateService.class);
        ReflectionTestUtils.setField(serviceTarget, "programPaymentGateService", gate);
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(serviceTarget, "programPaymentGateService", originalGate);
    }

    @Test
    public void confirmReceived_holdsUnpaidCaseButRecordsCollection() {
        when(gate.checkPayment(any())).thenReturn(OpenMrsPaymentStatus.PENDING);

        OpenMrsPaymentStatus paymentStatus = pathologySampleService.confirmReceived(CASE_ID, USER_ID);

        assertEquals(OpenMrsPaymentStatus.PENDING, paymentStatus);
        assertEquals(PathologyStatus.AWAITING_PAYMENT, pathologySampleService.get(CASE_ID).getStatus());
        assertEquals(Integer.valueOf(1),
                jdbcTemplate.queryForObject("SELECT count(*) FROM sample WHERE id = 2 AND collection_date IS NOT NULL",
                        Integer.class));
    }

    @Test
    public void confirmReceived_movesPaidCaseToGrossing() {
        when(gate.checkPayment(any())).thenReturn(OpenMrsPaymentStatus.PAID);

        pathologySampleService.confirmReceived(CASE_ID, USER_ID);

        assertEquals(PathologyStatus.GROSSING, pathologySampleService.get(CASE_ID).getStatus());
    }

    @Test
    public void confirmReceived_neverHoldsFrozenSection() {
        jdbcTemplate.update("UPDATE pathology_sample SET subtype = 'FROZEN' WHERE id = ?", CASE_ID);

        OpenMrsPaymentStatus paymentStatus = pathologySampleService.confirmReceived(CASE_ID, USER_ID);

        assertEquals(OpenMrsPaymentStatus.NOT_APPLICABLE, paymentStatus);
        assertEquals(PathologyStatus.GROSSING, pathologySampleService.get(CASE_ID).getStatus());
        verify(gate, never()).checkPayment(any());
    }

    @Test
    public void checkPayment_keepsCaseHeldUntilPaid() {
        when(gate.checkPayment(any())).thenReturn(OpenMrsPaymentStatus.PENDING);
        pathologySampleService.confirmReceived(CASE_ID, USER_ID);

        assertEquals(OpenMrsPaymentStatus.PENDING, pathologySampleService.checkPayment(CASE_ID, USER_ID));
        assertEquals(PathologyStatus.AWAITING_PAYMENT, pathologySampleService.get(CASE_ID).getStatus());

        when(gate.checkPayment(any())).thenReturn(OpenMrsPaymentStatus.EXEMPTED);
        assertEquals(OpenMrsPaymentStatus.EXEMPTED, pathologySampleService.checkPayment(CASE_ID, USER_ID));
        assertEquals(PathologyStatus.GROSSING, pathologySampleService.get(CASE_ID).getStatus());
    }

    @Test
    public void checkPayment_ignoresCaseNotAwaitingPayment() {
        assertNull(pathologySampleService.checkPayment(3, USER_ID));
        assertEquals(PathologyStatus.READY_PATHOLOGIST, pathologySampleService.get(3).getStatus());
        verify(gate, never()).checkPayment(any());
    }

    @Test
    public void sendToProcessing_rejectedWhileAwaitingPayment() {
        when(gate.checkPayment(any())).thenReturn(OpenMrsPaymentStatus.PENDING);
        pathologySampleService.confirmReceived(CASE_ID, USER_ID);

        try {
            pathologySampleService.sendToProcessing(CASE_ID, "gross", List.of(new PathologyBlock()), USER_ID);
            fail("expected IllegalArgumentException while awaiting payment");
        } catch (IllegalArgumentException expected) {
            // held
        }
        assertEquals(PathologyStatus.AWAITING_PAYMENT, pathologySampleService.get(CASE_ID).getStatus());
    }

    @Test
    public void updateWithFormValues_cannotMoveCaseOutOfAwaitingPayment() {
        when(gate.checkPayment(any())).thenReturn(OpenMrsPaymentStatus.PENDING);
        pathologySampleService.confirmReceived(CASE_ID, USER_ID);

        PathologySampleForm form = new PathologySampleForm();
        form.setStatus(PathologyStatus.GROSSING);
        form.setSystemUserId(USER_ID);
        try {
            pathologySampleService.updateWithFormValues(CASE_ID, form);
            fail("expected IllegalArgumentException while awaiting payment");
        } catch (IllegalArgumentException expected) {
            // held
        }
        assertEquals(PathologyStatus.AWAITING_PAYMENT, pathologySampleService.get(CASE_ID).getStatus());
    }
}
