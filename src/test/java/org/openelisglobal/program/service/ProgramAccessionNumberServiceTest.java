package org.openelisglobal.program.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.provider.validation.IAccessionNumberValidator;
import org.openelisglobal.common.provider.validation.IAccessionNumberValidator.ValidationResults;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.program.valueholder.ServiceCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Histopathology and cytopathology cases draw from their own accession
 * sequences (site prefix + PATH/CYTO + year), separate from the general one,
 * and those numbers pass the SITEYEARNUM validator.
 */
public class ProgramAccessionNumberServiceTest extends BaseWebContextSensitiveTest {

    private static final String SITE = "TST01";

    @Autowired
    private ProgramAccessionNumberService accessionNumberService;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private AccessionNumberValidatorFactory validatorFactory;

    private JdbcTemplate jdbcTemplate;
    private String oldPrefix;
    private String oldValidate;
    private String oldFlag;
    private String year;

    @Before
    public void init() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        ConfigurationProperties config = ConfigurationProperties.getInstance();
        oldPrefix = config.getPropertyValue(Property.ACCESSION_NUMBER_PREFIX);
        oldValidate = config.getPropertyValue(Property.ACCESSION_NUMBER_VALIDATE);
        oldFlag = config.getPropertyValue(Property.PATHOLOGY_ACCESSION_BY_SERVICE_CATEGORY);
        config.setPropertyValue(Property.ACCESSION_NUMBER_PREFIX, SITE);
        config.setPropertyValue(Property.ACCESSION_NUMBER_VALIDATE, "true");
        config.setPropertyValue(Property.PATHOLOGY_ACCESSION_BY_SERVICE_CATEGORY, "true");
        year = DateUtil.getTwoDigitYear();
        deleteCounters();
    }

    @After
    public void tearDown() {
        deleteCounters();
        ConfigurationProperties config = ConfigurationProperties.getInstance();
        config.setPropertyValue(Property.ACCESSION_NUMBER_PREFIX, oldPrefix);
        config.setPropertyValue(Property.ACCESSION_NUMBER_VALIDATE, oldValidate);
        config.setPropertyValue(Property.PATHOLOGY_ACCESSION_BY_SERVICE_CATEGORY, oldFlag);
    }

    @Test
    public void histopathologyAndCytopathologyHaveTheirOwnSequences() {
        assertEquals(SITE + "PATH" + year + "000000001",
                accessionNumberService.nextAccessionNumber(ServiceCategory.HISTOPATHOLOGY));
        assertEquals(SITE + "PATH" + year + "000000002",
                accessionNumberService.nextAccessionNumber(ServiceCategory.HISTOPATHOLOGY));
        assertEquals(SITE + "CYTO" + year + "000000001",
                accessionNumberService.nextAccessionNumber(ServiceCategory.CYTOPATHOLOGY));
        assertEquals(SITE + year + "0000000000001", accessionNumberService.nextAccessionNumber(null));

        assertEquals(Long.valueOf(2), jdbcTemplate.queryForObject(
                "SELECT cur_val FROM accession_number_info WHERE prefix = ?", Long.class, SITE + "PATH" + year));
        assertEquals(Long.valueOf(1), jdbcTemplate
                .queryForObject("SELECT cur_val FROM accession_number_info WHERE prefix = ?", Long.class, SITE + year));
    }

    @Test
    public void categoryNumberAlreadyOnASample_isSkipped() throws Exception {
        executeDataSetWithStateManagement("testdata/pathology-sample.xml");
        String taken = SITE + "PATH" + year + "000000001";
        jdbcTemplate.update("UPDATE sample SET accession_number = ? WHERE id = 2", taken);

        String issued = accessionNumberService.nextAccessionNumber(ServiceCategory.HISTOPATHOLOGY);

        assertEquals(SITE + "PATH" + year + "000000002", issued);
        assertNotEquals(taken, issued);
    }

    @Test
    public void flagOff_everyCaseUsesTheGeneralSequence() {
        ConfigurationProperties.getInstance().setPropertyValue(Property.PATHOLOGY_ACCESSION_BY_SERVICE_CATEGORY,
                "false");

        String number = accessionNumberService.nextAccessionNumber(ServiceCategory.HISTOPATHOLOGY);

        assertEquals(SITE + year + "0000000000001", number);
        assertFalse(number.contains("PATH"));
    }

    @Test
    public void categoryNumbersAreValidAccessionNumbers() throws Exception {
        IAccessionNumberValidator validator = validatorFactory.getValidator(AccessionFormat.MAIN);
        String path = accessionNumberService.nextAccessionNumber(ServiceCategory.HISTOPATHOLOGY);
        String cyto = accessionNumberService.nextAccessionNumber(ServiceCategory.CYTOPATHOLOGY);
        String general = accessionNumberService.nextAccessionNumber(null);

        for (String number : new String[] { path, cyto, general }) {
            assertEquals(number, 20, number.length());
            assertEquals(number, ValidationResults.SUCCESS, validator.validFormat(number, true));
        }
        assertNotEquals(path.substring(SITE.length()), cyto.substring(SITE.length()));
    }

    @Test
    public void malformedCategoryNumbersAreRejected() throws Exception {
        IAccessionNumberValidator validator = validatorFactory.getValidator(AccessionFormat.MAIN);
        String lastYear = String.format("%02d", (Integer.parseInt(year) + 99) % 100);

        assertEquals(ValidationResults.LENGTH_FAIL, validator.validFormat(SITE + "PATH" + year + "00001", true));
        assertEquals(ValidationResults.FORMAT_FAIL, validator.validFormat(SITE + "PATH" + year + "00000A001", true));
        assertEquals(ValidationResults.YEAR_FAIL, validator.validFormat(SITE + "CYTO" + lastYear + "000000001", true));
        assertEquals(ValidationResults.SUCCESS, validator.validFormat(SITE + "CYTO" + lastYear + "000000001", false));
        assertTrue(validator.validFormat(SITE + "XXXX" + year + "000000001", true) != ValidationResults.SUCCESS);
    }

    private void deleteCounters() {
        jdbcTemplate.update("DELETE FROM accession_number_info WHERE prefix LIKE ?", SITE + "%");
    }
}
