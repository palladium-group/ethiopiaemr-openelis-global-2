// Java standard library
package org.openelisglobal.integration.ocl;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.testconfiguration.controller.rest.TestAddRestController;
import org.openelisglobal.testconfiguration.form.TestAddForm;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class OclImportInitializer implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger log = LoggerFactory.getLogger(OclImportInitializer.class);
    
    @Autowired
    private OclZipImporter oclZipImporter;

    @Autowired
    private TestAddRestController testAddRestController;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // This triggers the import when the Spring context is refreshed
        try {
            List<JsonNode> oclNodes = oclZipImporter.importOclZip();
            log.info("OCL Import: Found {} nodes to process.", oclNodes.size());
            
            int conceptCount = 0;
            int testsCreated = 0;
            int testsSkipped = 0;
            
            OclToOpenElisMapper mapper = new OclToOpenElisMapper();
            
            for (JsonNode node : oclNodes) {
                // If the node is a Collection Version, get its concepts array
                if (node.has("concepts") && node.get("concepts").isArray()) {
                    log.info("OCL Import: Node has a concepts array of size {}.", node.get("concepts").size());
                    
                    // Map all concepts in this node to TestAddForms
                    List<TestAddForm> testForms = mapper.mapConceptsToTestAddForms(node);
                    
                    for (TestAddForm form : testForms) {
                        conceptCount++;
                        
                        try {
                            log.info("OCL Import: Processing concept #{} - attempting to create test", conceptCount);
                            
                            // Use the TestAddRestController to create the test
                            TestAddForm resultForm = testAddRestController.postTestAdd(null, form, null);
                            // TODO: Implement robust error checking based on TestAddForm fields
                            if (resultForm != null) {
                                testsCreated++;
                                log.info("OCL Import: Successfully created test #{}", testsCreated);
                            } else {
                                testsSkipped++;
                                log.warn("OCL Import: Failed to create test - result: {}", resultForm);
                            }
                            
                        } catch (Exception ex) {
                            testsSkipped++;
                            log.error("OCL Import: Failed to create test for concept #{}", conceptCount, ex);
                        }
                    }
                } else if (node.isArray()) {
                    // Handle direct array of concepts
                    log.info("OCL Import: Node is a direct array of size {}.", node.size());
                    
                    for (JsonNode concept : node) {
                        conceptCount++;
                        
                        try {
                            log.info("OCL Import: Processing concept #{} with id: {}", conceptCount, 
                                    concept.has("id") ? concept.get("id").asText() : "unknown");
                            
                            // Create a wrapper node for the single concept
                            JsonNode wrapperNode = mapper.createConceptWrapper(concept);
                            List<TestAddForm> testForms = mapper.mapConceptsToTestAddForms(wrapperNode);
                            
                            for (TestAddForm form : testForms) {
                                try {
                                    org.springframework.validation.BindingResult bindingResultArray = new org.springframework.validation.BeanPropertyBindingResult(form, "testAddForm");
                                    TestAddForm resultForm = testAddRestController.postTestAdd(null, form, bindingResultArray);
                                    // TODO: Implement robust error checking based on TestAddForm fields
                                    if (resultForm != null) {
                                        testsCreated++;
                                        log.info("OCL Import: Successfully created test #{}", testsCreated);
                                    } else {
                                        testsSkipped++;
                                        log.warn("OCL Import: Failed to create test - result: {}", resultForm);
                                    }
                                } catch (Exception ex) {
                                    testsSkipped++;
                                    log.error("OCL Import: Failed to create test for concept #{}", conceptCount, ex);
                                }
                            }
                            
                        } catch (Exception ex) {
                            testsSkipped++;
                            log.error("OCL Import: Failed to process concept #{}", conceptCount, ex);
                        }
                    }
                } else {
                    // Handle single concept
                    conceptCount++;
                    
                    try {
                        log.info("OCL Import: Processing single concept with id: {}", 
                                node.has("id") ? node.get("id").asText() : "unknown");
                        
                        // Create a wrapper node for the single concept
                        JsonNode wrapperNode = mapper.createConceptWrapper(node);
                        List<TestAddForm> testForms = mapper.mapConceptsToTestAddForms(wrapperNode);
                        
                        for (TestAddForm form : testForms) {
                            try {
                                org.springframework.validation.BindingResult bindingResultSingle = new org.springframework.validation.BeanPropertyBindingResult(form, "testAddForm");
                                TestAddForm resultForm = testAddRestController.postTestAdd(null, form, bindingResultSingle);
                                // TODO: Implement robust error checking based on TestAddForm fields
                                if (resultForm != null) {
                                    testsCreated++;
                                    log.info("OCL Import: Successfully created test #{}", testsCreated);
                                } else {
                                    testsSkipped++;
                                    log.warn("OCL Import: Failed to create test - result: {}", resultForm);
                                }
                            } catch (Exception ex) {
                                testsSkipped++;
                                log.error("OCL Import: Failed to create test for single concept", ex);
                            }
                        }
                        
                    } catch (Exception ex) {
                        testsSkipped++;
                        log.error("OCL Import: Failed to process single concept", ex);
                    }
                }
            }
            
            log.info("OCL Import: Finished processing. Total concepts processed: {}, Tests created: {}, Tests skipped: {}", 
                    conceptCount, testsCreated, testsSkipped);
                    
        } catch (java.io.IOException e) {
            log.error("OCL Import failed during file processing", e);
        }
        
        // Refresh display lists and clear caches
        try {
            DisplayListService displayListService = SpringContext.getBean(DisplayListService.class);
            displayListService.refreshList(DisplayListService.ListType.PANELS_INACTIVE);
            displayListService.refreshList(DisplayListService.ListType.PANELS);
            displayListService.refreshList(DisplayListService.ListType.TEST_SECTION_ACTIVE);
            displayListService.refreshList(DisplayListService.ListType.TEST_SECTION_BY_NAME);
            displayListService.refreshList(DisplayListService.ListType.TEST_SECTION_INACTIVE);
            SpringContext.getBean(TypeOfSampleService.class).clearCache();
            
            log.info("OCL Import: Successfully refreshed display lists and cleared caches");
        } catch (Exception e) {
            log.error("Failed to refresh display lists or clear cache after OCL import", e);
        }
    }
}