package org.openelisglobal.reception.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;

@RunWith(MockitoJUnitRunner.class)
public class ReceptionApprovalSupportTest {

    @Mock
    private ConfigurationProperties configurationProperties;

    @Mock
    private IStatusService statusService;

    @InjectMocks
    private ReceptionApprovalSupport receptionApprovalSupport;

  @Test
  public void resolveInitialAnalysisStatusId_whenSampleRejected_returnsSampleRejected() {
    when(statusService.getStatusID(AnalysisStatus.SampleRejected)).thenReturn("10");

    assertEquals(
        "10",
        receptionApprovalSupport.resolveInitialAnalysisStatusId(true));
  }

  @Test
  public void resolveInitialAnalysisStatusId_whenReceptionRequired_returnsPendingReception() {
    when(statusService.getStatusID(AnalysisStatus.PendingReception)).thenReturn("20");
    when(configurationProperties.isPropertyValueEqual(Property.RECEPTION_APPROVAL_REQUIRED, "true"))
        .thenReturn(true);

    assertEquals(
        "20",
        receptionApprovalSupport.resolveInitialAnalysisStatusId(false));
  }

  @Test
  public void resolveInitialAnalysisStatusId_whenReceptionNotRequired_returnsNotTested() {
    when(statusService.getStatusID(AnalysisStatus.NotStarted)).thenReturn("4");
    when(configurationProperties.isPropertyValueEqual(Property.RECEPTION_APPROVAL_REQUIRED, "true"))
        .thenReturn(false);

    assertEquals(
        "4",
        receptionApprovalSupport.resolveInitialAnalysisStatusId(false));
  }

  @Test
  public void isReceptionApprovalRequired_whenConfigTrue_returnsTrue() {
    when(configurationProperties.isPropertyValueEqual(Property.RECEPTION_APPROVAL_REQUIRED, "true"))
        .thenReturn(true);

    org.junit.Assert.assertTrue(receptionApprovalSupport.isReceptionApprovalRequired());
  }
}
