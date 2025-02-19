package org.openelisglobal.integration.ocl;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { OclIntegrationTestConfig.class })

public class OclZipImporterIntegrationTest {

    @Autowired
    private OclZipImporter oclZipImporter;

    @Test
    public void testImportOclZip() {
        try {
            oclZipImporter.importOclZip();
        } catch (Exception e) {
            fail("Exception during OCL ZIP import: " + e.getMessage());
        }
    }
}
