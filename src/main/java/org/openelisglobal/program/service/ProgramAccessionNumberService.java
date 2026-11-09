package org.openelisglobal.program.service;

import org.openelisglobal.common.exception.LIMSInvalidConfigurationException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.provider.validation.IAccessionNumberGenerator;
import org.openelisglobal.common.provider.validation.SiteYearAccessionValidator;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.program.valueholder.ServiceCategory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Accession numbers for program cases created by the backend (OpenMRS imports,
 * frozen → permanent biopsy). When {@code pathologyAccessionByCategory} is on
 * and the site uses the SITEYEARNUM format, histopathology and cytopathology
 * cases draw from their own sequence; everything else uses the general
 * sequence.
 */
@Service
public class ProgramAccessionNumberService {

    @Autowired
    private ConfigurationProperties configurationProperties;
    @Autowired
    private ObjectProvider<AccessionNumberValidatorFactory> validatorFactory;

    public String nextAccessionNumber(ServiceCategory category) {
        IAccessionNumberGenerator generator = mainGenerator();
        if (category != null && configurationProperties
                .isPropertyValueEqual(Property.PATHOLOGY_ACCESSION_BY_SERVICE_CATEGORY, "true")) {
            if (generator instanceof SiteYearAccessionValidator) {
                return ((SiteYearAccessionValidator) generator).incrementServiceCategoryAccessionNumber(category);
            }
            LogEvent.logWarn(this.getClass().getSimpleName(), "nextAccessionNumber",
                    "pathologyAccessionByCategory is on but the accession format is "
                            + configurationProperties.getPropertyValue(Property.AccessionFormat)
                            + "; category sequences need SITEYEARNUM, so the general sequence is used for "
                            + category.getDisplay());
        }
        return generator.getNextAvailableAccessionNumber("", true);
    }

    private IAccessionNumberGenerator mainGenerator() {
        AccessionNumberValidatorFactory factory = validatorFactory.getIfAvailable();
        try {
            IAccessionNumberGenerator generator = factory == null ? null : factory.getGenerator(AccessionFormat.MAIN);
            if (generator != null) {
                return generator;
            }
        } catch (LIMSInvalidConfigurationException e) {
            throw new IllegalStateException("no accession number generator configured", e);
        }
        throw new IllegalStateException("no accession number generator configured");
    }
}
