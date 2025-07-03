package org.openelisglobal.sampleqaevent;

import static org.junit.Assert.*;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleqaevent.service.SampleQaEventService;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;
import org.springframework.beans.factory.annotation.Autowired;

public class SampleQaEventServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleQaEventService qaEventService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/sample-qa-event.xml");

    }

    @Test
    public void getData_ShouldReturnDataForASpecificSampleQaEvent() {
        SampleQaEvent sampleQaEvent = qaEventService.get("3");
        qaEventService.getData(sampleQaEvent);
        assertNotNull(sampleQaEvent);
        assertEquals(Timestamp.valueOf("2025-06-25 12:00:00"), sampleQaEvent.getEnteredDate());
    }

    @Test
    public void getData_ShouldReturnDataUsingASampleQaEventID() {
        SampleQaEvent sampleQaEvent = qaEventService.getData("2");
        assertNotNull(sampleQaEvent);
        assertEquals(Timestamp.valueOf("2025-06-25 10:15:00"), sampleQaEvent.getLastupdated());
    }

    @Test
    public void getAllUncompletedEvents_ShouldReturnAllUncompletedSampleQaEvents() {
        List<SampleQaEvent> sampleQaEvents = qaEventService.getAllUncompleatedEvents();
        assertNotNull(sampleQaEvents);
        assertEquals(0, sampleQaEvents.size());
    }

    @Test
    public void getSampleQaEventsBySample_ShouldReturnAllSampleQaEvents_UsingASample() {
        Sample sample = new Sample();
        sample.setId("402");
        List<SampleQaEvent> sampleQaEvents = qaEventService.getSampleQaEventsBySample(sample);
        assertNotNull(sampleQaEvents);
        assertEquals(2, sampleQaEvents.size());
        assertEquals(Timestamp.valueOf("2025-06-25 10:45:00"), sampleQaEvents.get(0).getEnteredDate());
    }

    @Test
    public void getSampleQaEventsByUpdatedDate_ShouldReturnAllSampleQaEventsThatLieInTheParameterUpdateDates()
            throws ParseException {
        List<SampleQaEvent> sampleQaEvents = qaEventService.getSampleQaEventsByUpdatedDate(Date.valueOf("2025-05-25"),
                Date.valueOf("2025-07-25"));
        assertNotNull(sampleQaEvents);
        assertEquals(2, sampleQaEvents.size());
        assertEquals("2", sampleQaEvents.get(0).getId());
    }

    @Test
    public void getSampleQaEventBySampleAndQaEvent_ShouldReturnASampleQaEvent_UsingASampleAndAQaEvent() {
        SampleQaEvent sampleQaEvent = qaEventService.get("2");
        SampleQaEvent returnedSampleQaEvent = qaEventService.getSampleQaEventBySampleAndQaEvent(sampleQaEvent);
        assertNotNull(returnedSampleQaEvent);
        assertEquals("2", returnedSampleQaEvent.getId());
        assertEquals(Timestamp.valueOf("2025-06-25 10:45:00"), returnedSampleQaEvent.getEnteredDate());

    }

}
