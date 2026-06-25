package org.openelisglobal.dataexchange.openmrs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OpenMrsPaymentStatusTest {

    @Test
    public void fromCode_mapsCashierStatuses() {
        assertEquals(OpenMrsPaymentStatus.PAID, OpenMrsPaymentStatus.fromCode("PAID"));
        assertEquals(OpenMrsPaymentStatus.EXEMPTED, OpenMrsPaymentStatus.fromCode("exempted"));
        assertEquals(OpenMrsPaymentStatus.PENDING, OpenMrsPaymentStatus.fromCode("PENDING"));
    }

    @Test
    public void fromCode_returnsUnknownForBlankOrInvalid() {
        assertEquals(OpenMrsPaymentStatus.UNKNOWN, OpenMrsPaymentStatus.fromCode(null));
        assertEquals(OpenMrsPaymentStatus.UNKNOWN, OpenMrsPaymentStatus.fromCode(""));
        assertEquals(OpenMrsPaymentStatus.UNKNOWN, OpenMrsPaymentStatus.fromCode("NOT_A_STATUS"));
    }

    @Test
    public void allowsSampleCollection_onlyForPaidOrExempted() {
        assertTrue(OpenMrsPaymentStatus.PAID.allowsSampleCollection());
        assertTrue(OpenMrsPaymentStatus.EXEMPTED.allowsSampleCollection());
        assertFalse(OpenMrsPaymentStatus.PENDING.allowsSampleCollection());
        assertFalse(OpenMrsPaymentStatus.NOT_APPLICABLE.allowsSampleCollection());
    }

    @Test
    public void blocksSampleCollection_forPendingAndUnknown() {
        assertTrue(OpenMrsPaymentStatus.PENDING.blocksSampleCollection());
        assertTrue(OpenMrsPaymentStatus.UNKNOWN.blocksSampleCollection());
        assertFalse(OpenMrsPaymentStatus.PAID.blocksSampleCollection());
        assertFalse(OpenMrsPaymentStatus.NOT_APPLICABLE.blocksSampleCollection());
    }
}
