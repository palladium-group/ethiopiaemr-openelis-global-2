package org.openelisglobal.referral;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.referral.service.ReferralTypeService;
import org.springframework.beans.factory.annotation.Autowired;

public class ReferralTypeServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ReferralTypeService referralTypeService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/referral-type.xml");
    }

    @Test
    public void getReferralTypeByName_ShouldReturnAReferralTypeMatchingTheNamePassedAsParameter() {

    }
}
