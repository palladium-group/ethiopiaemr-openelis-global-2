package org.openelisglobal.barcode;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
 import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

public class BarcodeConfigurationRestControllerTest extends BaseWebContextSensitiveTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/barcode-lable-info.xml");
    }

    @Test
    public void getUrl() throws Exception {
        MvcResult urlResult = super.mockMvc
                .perform(get("/rest/BarcodeConfiguration").accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        String formJson = urlResult.getResponse().getContentAsString();
        int status = urlResult.getResponse().getStatus();
        System.out.println(status + " " + formJson);

        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, Object> formMap = objectMapper.readValue(formJson, new
     TypeReference<Map<String, Object>>() {
        });
     assertEquals("BarcodeConfigurationForm", formMap.get("formName"));
         assertEquals("MasterListsPage", formMap.get("cancelAction"));
         assertEquals("POST", formMap.get("cancelMethod"));

    }
}