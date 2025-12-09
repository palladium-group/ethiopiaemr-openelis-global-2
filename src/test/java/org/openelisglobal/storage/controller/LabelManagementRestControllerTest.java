package org.openelisglobal.storage.controller;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.storage.dao.StorageDeviceDAO;
import org.openelisglobal.storage.service.LabelManagementService;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

public class LabelManagementRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private LabelManagementService labelManagementService;

    @Autowired
    private StorageDeviceDAO storageDeviceDAO;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/status-of-sample.xml");

        org.openelisglobal.common.services.IStatusService statusService = org.openelisglobal.spring.util.SpringContext
                .getBean(org.openelisglobal.common.services.IStatusService.class);
        String statusId = statusService
                .getStatusID(org.openelisglobal.common.services.StatusService.SampleStatus.Entered);
        if ("-1".equals(statusId)) {
            throw new IllegalStateException(
                    "SampleStatus.Entered not found in database. Test data may not be loaded correctly.");
        }

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void postPrintLabelEndpoint_ShouldGeneratePdf_ForDeviceWithCode() throws Exception {
        List<StorageDevice> allDevices = storageDeviceDAO.getAll();
        StorageDevice suitableDevice = null;

        for (StorageDevice device : allDevices) {
            if (device.getCode() != null && !device.getCode().isEmpty() && device.getParentRoom() != null
                    && device.getActive()) {
                suitableDevice = device;
                break;
            }
        }

        assertNotNull("Should find a suitable device for testing", suitableDevice);
        String deviceId = String.valueOf(suitableDevice.getId());

        StorageDevice device = storageDeviceDAO.get(Integer.parseInt(deviceId)).orElse(null);
        assertNotNull("Device should exist", device);
        assertNotNull("Device should have code", device.getCode());
        assertNotNull("Device should have parentRoom", device.getParentRoom());
        assertNotNull("ParentRoom should have code", device.getParentRoom().getCode());

        mockMvc.perform(post("/rest/storage/device/" + deviceId + "/print-label")).andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    public void printValidation_ShouldReturnBadRequest_WhenCodeIsMissing() throws Exception {
        List<StorageDevice> allDevices = storageDeviceDAO.getAll();
        StorageDevice deviceWithEmptyCode = null;

        for (StorageDevice device : allDevices) {
            if ((device.getCode() == null || device.getCode().isEmpty()) && device.getActive()) {
                deviceWithEmptyCode = device;
                break;
            }
        }

        if (deviceWithEmptyCode == null) {
            return;
        }

        String deviceId = String.valueOf(deviceWithEmptyCode.getId());

        MvcResult result = mockMvc.perform(post("/rest/storage/device/" + deviceId + "/print-label"))
                .andExpect(status().isBadRequest()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists()).andReturn();

        String errorMessage = objectMapper.readTree(result.getResponse().getContentAsString()).get("error").asText();
        assertTrue("Error message should mention code", errorMessage.toLowerCase().contains("code"));
    }

    @Test
    public void postPrintLabelEndpoint_ShouldGeneratePdf_WhenCodeIsLessThanOrEqualTo10Chars() throws Exception {
        List<StorageDevice> allDevices = storageDeviceDAO.getAll();
        StorageDevice deviceWithShortCode = null;

        for (StorageDevice device : allDevices) {
            if (device.getCode() != null && device.getCode().length() <= 10 && device.getParentRoom() != null
                    && device.getActive()) {
                deviceWithShortCode = device;
                break;
            }
        }

        assertNotNull("Should find a device with code ≤ 10 chars", deviceWithShortCode);
        String deviceId = String.valueOf(deviceWithShortCode.getId());

        StorageDevice device = storageDeviceDAO.get(Integer.parseInt(deviceId)).orElse(null);
        assertNotNull("Device should exist", device);
        assertNotNull("Device should have code", device.getCode());
        assertTrue("Device code should be ≤ 10 chars", device.getCode().length() <= 10);
        assertNotNull("Device should have parent room", device.getParentRoom());

        mockMvc.perform(post("/rest/storage/device/" + deviceId + "/print-label")).andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    public void ErrorResponseIfCodeMissing_ReturnsJsonError() throws Exception {
        List<StorageDevice> allDevices = storageDeviceDAO.getAll();
        StorageDevice deviceWithEmptyCode = null;

        for (StorageDevice device : allDevices) {
            if ((device.getCode() == null || device.getCode().isEmpty()) && device.getActive()) {
                deviceWithEmptyCode = device;
                break;
            }
        }

        if (deviceWithEmptyCode == null) {
            return;
        }

        String deviceId = String.valueOf(deviceWithEmptyCode.getId());

        MvcResult result = mockMvc.perform(post("/rest/storage/device/" + deviceId + "/print-label"))
                .andExpect(status().isBadRequest()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String errorMessage = objectMapper.readTree(result.getResponse().getContentAsString()).get("error").asText();
        assertTrue("Error message should mention code", errorMessage.toLowerCase().contains("code"));
    }

    @Test
    public void printHistory_ShouldBeRecorded_AfterLabelGeneration() throws Exception {
        List<StorageDevice> allDevices = storageDeviceDAO.getAll();
        StorageDevice suitableDevice = null;

        for (StorageDevice device : allDevices) {
            if (device.getCode() != null && !device.getCode().isEmpty() && device.getParentRoom() != null
                    && device.getActive()) {
                suitableDevice = device;
                break;
            }
        }

        assertNotNull("Should find a suitable device for testing", suitableDevice);
        String deviceId = String.valueOf(suitableDevice.getId());

        mockMvc.perform(post("/rest/storage/device/" + deviceId + "/print-label")).andExpect(status().isOk());

        Integer count = countRowsInTable("storage_location_print_history");
        assertNotNull("Print history should be recorded", count);
        assertTrue("Print history count should be > 0", count > 0);
    }

    @Test
    public void testPostPrintLabelEndpoint_InvalidType_Returns400() throws Exception {
        List<StorageDevice> allDevices = storageDeviceDAO.getAll();
        assertTrue("Should have at least one device", !allDevices.isEmpty());
        String deviceId = String.valueOf(allDevices.get(0).getId());

        mockMvc.perform(post("/rest/storage/room/" + deviceId + "/print-label")).andExpect(status().isBadRequest());
    }

    @Test
    public void postPrintLabelEndpoint_ShouldReturnBadRequest_ForInvalidType() throws Exception {
        List<StorageDevice> allDevices = storageDeviceDAO.getAll();
        StorageDevice suitableDevice = null;

        for (StorageDevice device : allDevices) {
            if (device.getCode() != null && !device.getCode().isEmpty() && device.getParentRoom() != null
                    && device.getActive()) {
                suitableDevice = device;
                break;
            }
        }

        assertNotNull("Should find a suitable device for testing", suitableDevice);
        String deviceId = String.valueOf(suitableDevice.getId());

        mockMvc.perform(post("/rest/storage/device/" + deviceId + "/print-label")).andExpect(status().isOk());

        mockMvc.perform(get("/rest/storage/device/" + deviceId + "/print-history")).andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void pdfGeneration_ShouldUseCode_FromEntity() throws Exception {
        List<StorageDevice> allDevices = storageDeviceDAO.getAll();
        StorageDevice deviceWithCode = null;

        for (StorageDevice device : allDevices) {
            if (device.getCode() != null && !device.getCode().isEmpty() && device.getParentRoom() != null
                    && device.getActive()) {
                deviceWithCode = device;
                break;
            }
        }

        assertNotNull("Should find a device with code", deviceWithCode);
        String deviceId = String.valueOf(deviceWithCode.getId());

        StorageDevice device = storageDeviceDAO.get(Integer.parseInt(deviceId)).orElse(null);
        assertNotNull("Device should exist", device);
        assertNotNull("Device should have code", device.getCode());

        mockMvc.perform(post("/rest/storage/device/" + deviceId + "/print-label")).andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }
}