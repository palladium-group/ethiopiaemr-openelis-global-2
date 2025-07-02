package org.openelisglobal.patienttype;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.patienttype.service.PatientPatientTypeService;
import org.openelisglobal.patienttype.valueholder.PatientPatientType;
import org.openelisglobal.patienttype.valueholder.PatientType;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertNotNull;

public class PatientPatientTypeServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PatientPatientTypeService patientPatientTypeService;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/patient-type.xml");
    }

    @Test
    public void getPatientTypeForPatient(){
        PatientType patientType = patientPatientTypeService.getPatientTypeForPatient("2");
        assertNotNull(patientType);



    }
}
