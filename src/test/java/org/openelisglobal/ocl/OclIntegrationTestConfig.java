package org.openelisglobal.integration.ocl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OclIntegrationTestConfig {

    @Bean
    public OclZipImporter oclZipImporter() {
        return new OclZipImporter();
    }
}
