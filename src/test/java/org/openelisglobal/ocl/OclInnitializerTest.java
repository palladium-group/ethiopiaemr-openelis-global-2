package org.openelisglobal.ocl;

import static org.junit.Assert.fail;

import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class OclInnitializerTest extends BaseWebContextSensitiveTest {
    private static final Logger log = LoggerFactory.getLogger(OclZipImporterIntegrationTest.class);

    @Autowired
    private OclZipImporter oclZipImporter;

    @Autowired
    OclImportInitializer oclImportInitializer;

    @Autowired
    TypeOfSampleService typeOfSampleService;

    @Autowired
    LocalizationService localizationService;

    private static String oclDirPath;
    private static String sampleType = "Whole Blood";

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/ocl-import.xml");
        if (oclZipImporter == null) {
            fail("OclZipImporter bean not autowired. Check Spring configuration.");
        }
        oclDirPath = this.getClass().getClassLoader().getResource("ocl").getFile();
    }

    @Test
    public void testImportOclPackage_validZip() throws IOException {
        java.io.File tempFile = java.io.File.createTempFile("ocl_imported", ".flag");
        oclImportInitializer.performOclImport(oclDirPath, tempFile.getAbsolutePath());
    }
}
