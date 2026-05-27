package org.openelisglobal.reception.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.ExternalOrderStatus;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderType;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.reception.dto.RecollectionOrderResult;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

@RunWith(MockitoJUnitRunner.class)
public class ElectronicOrderRecollectionServiceTest {

    @Mock
    private ElectronicOrderService electronicOrderService;
    @Mock
    private IStatusService statusService;
    @Mock
    private SampleItemService sampleItemService;

    @InjectMocks
    private ElectronicOrderRecollectionServiceImpl recollectionService;

    @Before
    public void setUp() {
        when(statusService.getStatusID(ExternalOrderStatus.Entered)).thenReturn("1");
    }

    @Test
    public void extractBaseExternalId_stripsRecollectionSuffix() {
        assertEquals("ORDER-123", ElectronicOrderRecollectionServiceImpl.extractBaseExternalId("ORDER-123-R1"));
        assertEquals("ORDER-123", ElectronicOrderRecollectionServiceImpl.extractBaseExternalId("ORDER-123"));
    }

    @Test
    public void createRecollectionOrder_returnsEmptyWhenNoLinkedOrder() {
        Sample sample = new Sample();
        sample.setId("S1");

        Optional<RecollectionOrderResult> result = recollectionService.createRecollectionOrder(sample, "99");

        assertFalse(result.isPresent());
    }

    @Test
    public void createRecollectionOrder_clonesOrderWithEnteredStatusAndNewExternalId() {
        Sample sample = new Sample();
        sample.setId("S1");
        sample.setClinicalOrderId("EO-1");
        sample.setReferringId("OPENMRS-ORDER-123");

        Patient patient = new Patient();
        patient.setId("P1");

        ElectronicOrder sourceOrder = new ElectronicOrder();
        sourceOrder.setId("EO-1");
        sourceOrder.setExternalId("OPENMRS-ORDER-123");
        sourceOrder.setData("{\"resourceType\":\"Task\",\"status\":\"requested\",\"intent\":\"order\"}");
        sourceOrder.setPatient(patient);
        sourceOrder.setType(ElectronicOrderType.FHIR);
        sourceOrder.setPriority(OrderPriority.STAT);

        when(electronicOrderService.get("EO-1")).thenReturn(sourceOrder);
        when(electronicOrderService.getElectronicOrdersByExternalId("OPENMRS-ORDER-123-R1"))
                .thenReturn(Collections.emptyList());
        when(electronicOrderService.insert(any(ElectronicOrder.class))).thenReturn("EO-2");
        SampleItem sampleItem = new SampleItem();
        TypeOfSample typeOfSample = new TypeOfSample();
        typeOfSample.setId("20");
        sampleItem.setTypeOfSample(typeOfSample);
        when(sampleItemService.getSampleItemsBySampleId("S1")).thenReturn(Collections.singletonList(sampleItem));

        Optional<RecollectionOrderResult> result = recollectionService.createRecollectionOrder(sample, "99");

        assertTrue(result.isPresent());
        assertEquals("EO-2", result.get().getElectronicOrderId());
        assertEquals("OPENMRS-ORDER-123-R1", result.get().getExternalOrderId());

        ArgumentCaptor<ElectronicOrder> captor = ArgumentCaptor.forClass(ElectronicOrder.class);
        verify(electronicOrderService).insert(captor.capture());
        ElectronicOrder inserted = captor.getValue();
        assertEquals("OPENMRS-ORDER-123-R1", inserted.getExternalId());
        assertEquals("1", inserted.getStatusId());
        org.junit.Assert.assertTrue(inserted.getData().contains("CI0050007AAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        org.junit.Assert.assertTrue(inserted.getData().contains("\"20\""));
        assertEquals(patient, inserted.getPatient());
        assertEquals(ElectronicOrderType.FHIR, inserted.getType());
        assertEquals(OrderPriority.STAT, inserted.getPriority());
        assertEquals("99", inserted.getSysUserId());
    }

    @Test
    public void createRecollectionOrder_incrementsSuffixWhenRecollectionAlreadyExists() {
        Sample sample = new Sample();
        sample.setClinicalOrderId("EO-3");
        sample.setReferringId("OPENMRS-ORDER-123-R1");

        ElectronicOrder sourceOrder = new ElectronicOrder();
        sourceOrder.setId("EO-3");
        sourceOrder.setExternalId("OPENMRS-ORDER-123-R1");
        sourceOrder.setData("payload");
        sourceOrder.setType(ElectronicOrderType.FHIR);
        sourceOrder.setPriority(OrderPriority.ROUTINE);

        ElectronicOrder existingRecollection = new ElectronicOrder();
        existingRecollection.setExternalId("OPENMRS-ORDER-123-R1");

        when(electronicOrderService.get("EO-3")).thenReturn(sourceOrder);
        when(electronicOrderService.getElectronicOrdersByExternalId("OPENMRS-ORDER-123-R1"))
                .thenReturn(Collections.singletonList(existingRecollection));
        when(electronicOrderService.getElectronicOrdersByExternalId("OPENMRS-ORDER-123-R2"))
                .thenReturn(Collections.emptyList());
        when(electronicOrderService.insert(any(ElectronicOrder.class))).thenReturn("EO-4");

        Optional<RecollectionOrderResult> result = recollectionService.createRecollectionOrder(sample, "99");

        assertTrue(result.isPresent());
        assertEquals("OPENMRS-ORDER-123-R2", result.get().getExternalOrderId());
    }

    @Test
    public void resolveSourceOrder_usesReferringIdWhenClinicalOrderMissing() {
        Sample sample = new Sample();
        sample.setReferringId("OPENMRS-ORDER-456");

        ElectronicOrder sourceOrder = new ElectronicOrder();
        sourceOrder.setId("EO-9");
        sourceOrder.setExternalId("OPENMRS-ORDER-456");

        when(electronicOrderService.getElectronicOrdersByExternalId("OPENMRS-ORDER-456"))
                .thenReturn(Collections.singletonList(sourceOrder));

        ElectronicOrder resolved = recollectionService.resolveSourceOrder(sample);

        assertEquals("EO-9", resolved.getId());
    }
}
