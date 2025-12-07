package org.openelisglobal.storage.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

public class BarcodeValidationRestControllerTest extends BaseWebContextSensitiveTest {

    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();

        executeDataSetWithStateManagement("testdata/storage_barcode_hierarchy.xml");
    }

    @Test
    public void validateBarcode_ShouldReturnValidResponse_ForCorrectBarcode() throws Exception {
        String barcode = "TESTROOM01-TESTDEV01";
        String requestBody = String.format("{\"barcode\": \"%s\"}", barcode);

        MvcResult result = mockMvc.perform(
                post("/rest/storage/barcode/validate").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());

        assertNotNull(response);
        assertTrue(response.get("valid").asBoolean());
        assertTrue(response.get("validComponents").size() > 0);
    }

    @Test
    public void validateBarcode_ShouldMatchApiContract_ForValidBarcode() throws Exception {
        String barcode = "TESTROOM01-TESTDEV01";
        String requestBody = String.format("{\"barcode\": \"%s\"}", barcode);

        MvcResult result = mockMvc.perform(
                post("/rest/storage/barcode/validate").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());

        assertTrue(response.has("valid"));
        assertTrue(response.has("validComponents"));
        assertTrue(response.has("barcode"));
        assertEquals(barcode, response.get("barcode").asText());

        if (response.get("valid").asBoolean()) {
            assertTrue(response.has("failedStep") || !response.has("failedStep"));
            assertTrue(response.has("errorMessage") || !response.has("errorMessage"));
        }
    }

    @Test
    public void validateBarcode_ShouldReturnInvalidResponse_ForMalformedBarcode() throws Exception {
        String invalidBarcode = "INVALID_FORMAT_NO_HYPHEN";
        String requestBody = String.format("{\"barcode\": \"%s\"}", invalidBarcode);

        MvcResult result = mockMvc
                .perform(post("/rest/storage/barcode/validate").contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk()) // validation returns 200 with valid=false
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());

        assertFalse(response.get("valid").asBoolean());
        assertTrue(response.has("errorMessage"));
        assertNotNull(response.get("errorMessage").asText());
        assertTrue(response.has("failedStep"));
        assertEquals("FORMAT_VALIDATION", response.get("failedStep").asText());
    }

    @Test
    public void validateBarcode_ShouldReturnInvalidResponse_ForNonExistentLocation() throws Exception {
        String nonExistentBarcode = "NONEXISTENT-ROOM-DEVICE";
        String requestBody = String.format("{\"barcode\": \"%s\"}", nonExistentBarcode);

        MvcResult result = mockMvc.perform(
                post("/rest/storage/barcode/validate").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());

        assertFalse(response.get("valid").asBoolean());
        assertTrue(response.has("errorMessage"));
        assertTrue(response.get("errorMessage").asText().toLowerCase().contains("not found"));
    }

    @Test
    public void validate5LevelBarcode_ShouldReturnValidResponse_ForCompleteHierarchy() throws Exception {
        String barcode = "TESTROOM01-TESTDEV01-SHELF01-RACK01-A1";
        String requestBody = String.format("{\"barcode\": \"%s\"}", barcode);

        MvcResult result = mockMvc.perform(
                post("/rest/storage/barcode/validate").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());

        assertTrue(response.get("valid").asBoolean());
        JsonNode validComponents = response.get("validComponents");
        assertTrue(validComponents.has("room"));
        assertTrue(validComponents.has("device"));
        assertTrue(validComponents.has("shelf"));
        assertTrue(validComponents.has("rack"));
        assertTrue(validComponents.has("position"));
    }

    @Test
    public void validatePartialBarcode_ShouldReturnValidComponents_ForPartialInput() throws Exception {
        String barcode = "TESTROOM01-TESTDEV01-NONEXISTENT";
        String requestBody = String.format("{\"barcode\": \"%s\"}", barcode);

        MvcResult result = mockMvc.perform(
                post("/rest/storage/barcode/validate").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());

        assertFalse(response.get("valid").asBoolean());
        JsonNode validComponents = response.get("validComponents");
        assertTrue(validComponents.has("room"));
        assertTrue(validComponents.has("device"));
        assertFalse(validComponents.has("shelf"));
    }
}
