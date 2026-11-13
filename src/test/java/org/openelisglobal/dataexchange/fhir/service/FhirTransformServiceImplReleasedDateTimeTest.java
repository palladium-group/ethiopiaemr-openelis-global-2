package org.openelisglobal.dataexchange.fhir.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import org.junit.Test;
import org.openelisglobal.analysis.valueholder.Analysis;

/**
 * {@code ANALYSIS.RELEASED_DATE} is persisted with date-only precision, so the
 * result Observation's issued/effective time must take its time-of-day from the
 * analysis {@code lastupdated} stamp written by the validation save — otherwise
 * OpenMRS shows every synced result at 00:00.
 */
public class FhirTransformServiceImplReleasedDateTimeTest {

    private Analysis analysis(String releasedDate, String lastupdated) {
        // mocked: the real setReleasedDate pulls in DateUtil, which needs Spring
        Analysis analysis = mock(Analysis.class);
        when(analysis.getReleasedDate()).thenReturn(releasedDate == null ? null : java.sql.Date.valueOf(releasedDate));
        when(analysis.getLastupdated()).thenReturn(lastupdated == null ? null : Timestamp.valueOf(lastupdated));
        return analysis;
    }

    @Test
    public void usesLastupdatedTimeWhenSameDayAsRelease() {
        Analysis analysis = analysis("2026-10-19", "2026-10-19 14:35:12");

        assertEquals(Timestamp.valueOf("2026-10-19 14:35:12").getTime(),
                FhirTransformServiceImpl.getReleasedDateTime(analysis).getTime());
    }

    @Test
    public void fallsBackToReleasedDateWhenLastupdatedIsAnotherDay() {
        Analysis analysis = analysis("2026-10-19", "2026-10-21 09:00:00");

        assertEquals(java.sql.Date.valueOf("2026-10-19").getTime(),
                FhirTransformServiceImpl.getReleasedDateTime(analysis).getTime());
    }

    @Test
    public void fallsBackToReleasedDateWhenNoLastupdated() {
        Analysis analysis = analysis("2026-10-19", null);

        assertEquals(java.sql.Date.valueOf("2026-10-19").getTime(),
                FhirTransformServiceImpl.getReleasedDateTime(analysis).getTime());
    }

    @Test
    public void returnsNullWhenNotReleased() {
        assertNull(FhirTransformServiceImpl.getReleasedDateTime(analysis(null, "2026-10-19 14:35:12")));
    }
}
