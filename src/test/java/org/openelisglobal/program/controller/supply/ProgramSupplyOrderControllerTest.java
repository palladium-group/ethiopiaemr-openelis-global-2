package org.openelisglobal.program.controller.supply;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.service.cytology.CytologySampleService;
import org.openelisglobal.program.service.supply.ProgramSupplyOrderException;
import org.openelisglobal.program.service.supply.ProgramSupplyOrderException.Code;
import org.openelisglobal.program.service.supply.ProgramSupplyOrderService;
import org.openelisglobal.program.valueholder.cytology.CytologySample;
import org.openelisglobal.program.valueholder.cytology.CytologySample.CytologyStatus;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.program.valueholder.supply.ProgramSupplyRequestDisplayItem;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ProgramSupplyOrderControllerTest {

    private static final String BODY = "{\"items\":[{\"conceptUuid\":\"c1\",\"quantity\":1,\"urgency\":\"ROUTINE\"}]}";

    @Mock
    private ProgramSupplyOrderService supplyOrderService;
    @Mock
    private PathologySampleService pathologySampleService;
    @Mock
    private CytologySampleService cytologySampleService;

    @InjectMocks
    private ProgramSupplyOrderController controller;

    private MockMvc mockMvc;
    private UserSessionData usd;
    private Sample sample;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        usd = new UserSessionData();
        usd.setSytemUserId(1001);
        sample = new Sample();
        sample.setId("2");
    }

    @Test
    public void create_passesCaseSampleAndCurrentUser() throws Exception {
        when(pathologySampleService.get(2)).thenReturn(pathologyCase(PathologyStatus.GROSSING));
        ProgramSupplyRequestDisplayItem created = new ProgramSupplyRequestDisplayItem();
        created.setStatus("SENT");
        when(supplyOrderService.create(eq(sample), eq(false), any(), eq("1001"))).thenReturn(created);

        MockHttpServletResponse response = perform(post("/rest/pathology/caseView/2/supplyRequests").content(BODY));

        assertEquals(200, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"status\":\"SENT\""));
    }

    @Test
    public void list_saysWhetherTheCaseCameFromOpenMrs() throws Exception {
        when(pathologySampleService.get(2)).thenReturn(pathologyCase(PathologyStatus.GROSSING));
        when(supplyOrderService.listForSample(sample)).thenReturn(List.of(new ProgramSupplyRequestDisplayItem()));

        String walkIn = perform(get("/rest/pathology/caseView/2/supplyRequests")).getContentAsString();
        sample.setReferringId("0773d273-e32c-4fc2-85e5-f82bdbedb598");
        String fromOpenMrs = perform(get("/rest/pathology/caseView/2/supplyRequests")).getContentAsString();

        assertTrue(walkIn, walkIn.contains("\"fromOpenMrs\":false"));
        assertTrue(fromOpenMrs, fromOpenMrs.contains("\"fromOpenMrs\":true"));
        assertTrue(fromOpenMrs, fromOpenMrs.contains("\"requests\":[{"));
    }

    @Test
    public void create_tellsServiceWhenPathologyCaseIsCompleted() throws Exception {
        when(pathologySampleService.get(2)).thenReturn(pathologyCase(PathologyStatus.COMPLETED));

        perform(post("/rest/pathology/caseView/2/supplyRequests").content(BODY));

        verify(supplyOrderService).create(eq(sample), eq(true), any(), anyString());
    }

    @Test
    public void create_treatsRejectedCytologyCaseAsClosed() throws Exception {
        CytologySample cytologySample = new CytologySample();
        cytologySample.setSample(sample);
        cytologySample.setStatus(CytologyStatus.REJECTED);
        when(cytologySampleService.get(3)).thenReturn(cytologySample);

        perform(post("/rest/cytology/caseView/3/supplyRequests").content(BODY));

        verify(supplyOrderService).create(eq(sample), eq(true), any(), anyString());
    }

    @Test
    public void businessRefusals_mapToConflictWithCode() throws Exception {
        when(pathologySampleService.get(2)).thenReturn(pathologyCase(PathologyStatus.GROSSING));
        when(supplyOrderService.create(any(), anyBoolean(), any(), anyString()))
                .thenThrow(new ProgramSupplyOrderException(Code.NO_ACTIVE_VISIT, "no visit"));

        MockHttpServletResponse response = perform(post("/rest/pathology/caseView/2/supplyRequests").content(BODY));

        assertEquals(409, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":\"NO_ACTIVE_VISIT\""));
    }

    @Test
    public void openMrsProblems_mapToServiceUnavailable() throws Exception {
        when(supplyOrderService.searchCatalog("glo"))
                .thenThrow(new ProgramSupplyOrderException(Code.OPENMRS_UNAVAILABLE, "down"));

        assertEquals(503, perform(get("/rest/pathology/supplyOrder/catalog?q=glo")).getStatus());
    }

    @Test
    public void invalidLines_mapToBadRequest_andUnknownCaseToNotFound() throws Exception {
        when(pathologySampleService.get(2)).thenReturn(pathologyCase(PathologyStatus.GROSSING));
        when(supplyOrderService.create(any(), anyBoolean(), any(), anyString()))
                .thenThrow(new ProgramSupplyOrderException(Code.INVALID_ITEMS, "bad"));
        assertEquals(400, perform(post("/rest/pathology/caseView/2/supplyRequests").content(BODY)).getStatus());

        assertEquals(404, perform(get("/rest/pathology/caseView/99/supplyRequests")).getStatus());
    }

    private PathologySample pathologyCase(PathologyStatus status) {
        PathologySample pathologySample = new PathologySample();
        pathologySample.setSample(sample);
        pathologySample.setStatus(status);
        return pathologySample;
    }

    private MockHttpServletResponse perform(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.sessionAttr("userSessionData", usd).contentType(MediaType.APPLICATION_JSON))
                .andReturn().getResponse();
    }
}
