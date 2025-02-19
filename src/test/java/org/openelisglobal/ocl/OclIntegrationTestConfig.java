package org.openelisglobal.integration.ocl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:common.properties")
public class OclIntegrationTestConfig {

    @Bean
    public OclZipImporter oclZipImporter() {
        return new OclZipImporter();
    }
}
