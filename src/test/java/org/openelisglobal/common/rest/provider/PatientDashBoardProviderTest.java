package org.openelisglobal.common.rest.provider;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public class PatientDashBoardProviderTest extends BaseWebContextSensitiveTest {

    private static final int RECEPTION_USER_ID = 9001;

    private static final int VIEWER_USER_ID = 9002;

    private MockHttpSession receptionSession;

    private MockHttpSession viewerSession;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/home-dashboard-queue-test-data.xml");

        receptionSession = buildSession(RECEPTION_USER_ID);
        viewerSession = buildSession(VIEWER_USER_ID);
    }

    @Test
    public void getPendingReceptionQueue_withoutRole_returnsEmptyQueue() throws Exception {
        performQueueGet("/rest/home-dashboard/PENDING_RECEPTION", viewerSession).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.queue.totalItems").value(0))
                .andExpect(jsonPath("$.queue.items.length()").value(0));
    }

    @Test
    public void getPendingReceptionQueue_withRole_groupsPanelAndStandaloneRowsWithPatientFields() throws Exception {
        performQueueGet("/rest/home-dashboard/PENDING_RECEPTION", receptionSession).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.queue.totalItems").value(4))
                .andExpect(jsonPath("$.queue.items[0].accessionNumber").value("2026-005678"))
                .andExpect(jsonPath("$.queue.items[0].patientName").value("John Smith"))
                .andExpect(jsonPath("$.queue.items[0].subjectNumber").value("555-111-B"))
                .andExpect(jsonPath("$.queue.items[0].patientNationalId").value("ET-222"))
                .andExpect(jsonPath("$.queue.items[0].testCount").value(1))
                .andExpect(jsonPath("$.queue.items[1].accessionNumber").value("2026-009999"))
                .andExpect(jsonPath("$.queue.items[1].patientName").value("Abebe Kebede"))
                .andExpect(jsonPath("$.queue.items[1].subjectNumber").value("334-422-A"))
                .andExpect(jsonPath("$.queue.items[2].accessionNumber").value("2026-001234"))
                .andExpect(jsonPath("$.queue.items[2].testCount").value(1))
                .andExpect(jsonPath("$.queue.items[3].accessionNumber").value("2026-001234"))
                .andExpect(jsonPath("$.queue.items[3].testCount").value(1));
    }

    @Test
    public void getPendingReceptionQueue_filtersByPatientQuery() throws Exception {
        performQueueGet("/rest/home-dashboard/PENDING_RECEPTION", receptionSession, "patientQuery", "abebe")
                .andExpect(status().isOk()).andExpect(jsonPath("$.queue.totalItems").value(3))
                .andExpect(jsonPath("$.queue.items[0].accessionNumber").value("2026-009999"))
                .andExpect(jsonPath("$.queue.items[1].accessionNumber").value("2026-001234"));

        performQueueGet("/rest/home-dashboard/PENDING_RECEPTION", receptionSession, "patientQuery", "334-422")
                .andExpect(status().isOk()).andExpect(jsonPath("$.queue.totalItems").value(3));

        performQueueGet("/rest/home-dashboard/PENDING_RECEPTION", receptionSession, "patientQuery", "et-222")
                .andExpect(status().isOk()).andExpect(jsonPath("$.queue.totalItems").value(1))
                .andExpect(jsonPath("$.queue.items[0].accessionNumber").value("2026-005678"));
    }

    @Test
    public void getPendingReceptionQueue_filtersByLabNumber() throws Exception {
        performQueueGet("/rest/home-dashboard/PENDING_RECEPTION", receptionSession, "labNumber", "1234")
                .andExpect(status().isOk()).andExpect(jsonPath("$.queue.totalItems").value(2))
                .andExpect(jsonPath("$.queue.items[0].accessionNumber").value("2026-001234"))
                .andExpect(jsonPath("$.queue.items[1].accessionNumber").value("2026-001234"));
    }

    @Test
    public void getPendingReceptionQueue_paginatesResults() throws Exception {
        mockMvc.perform(get("/rest/home-dashboard/PENDING_RECEPTION").param("page", "1").param("pageSize", "1")
                .session(receptionSession)).andExpect(status().isOk()).andExpect(jsonPath("$.queue.page").value(1))
                .andExpect(jsonPath("$.queue.pageSize").value(1)).andExpect(jsonPath("$.queue.totalItems").value(4))
                .andExpect(jsonPath("$.queue.totalPages").value(4))
                .andExpect(jsonPath("$.queue.items.length()").value(1))
                .andExpect(jsonPath("$.queue.items[0].accessionNumber").value("2026-005678"));

        mockMvc.perform(get("/rest/home-dashboard/PENDING_RECEPTION").param("page", "2").param("pageSize", "1")
                .session(receptionSession)).andExpect(status().isOk()).andExpect(jsonPath("$.queue.page").value(2))
                .andExpect(jsonPath("$.queue.items[0].accessionNumber").value("2026-009999"));
    }

    @Test
    public void getPendingReceptionQueue_filtersByTestSection() throws Exception {
        performQueueGet("/rest/home-dashboard/PENDING_RECEPTION", receptionSession, "testSectionId", "9001")
                .andExpect(status().isOk()).andExpect(jsonPath("$.queue.totalItems").value(2))
                .andExpect(jsonPath("$.queue.items[0].accessionNumber").value("2026-005678"))
                .andExpect(jsonPath("$.queue.items[1].accessionNumber").value("2026-001234"));

        performQueueGet("/rest/home-dashboard/PENDING_RECEPTION", receptionSession, "testSectionId", "9002")
                .andExpect(status().isOk()).andExpect(jsonPath("$.queue.totalItems").value(2))
                .andExpect(jsonPath("$.queue.items[0].accessionNumber").value("2026-009999"))
                .andExpect(jsonPath("$.queue.items[1].accessionNumber").value("2026-001234"));
    }

    @Test
    public void getOrdersInProgressQueue_filtersByTestSection() throws Exception {
        performQueueGet("/rest/home-dashboard/ORDERS_IN_PROGRESS", receptionSession, "testSectionId", "9001")
                .andExpect(status().isOk()).andExpect(jsonPath("$.queue.totalItems").value(0));

        performQueueGet("/rest/home-dashboard/ORDERS_IN_PROGRESS", receptionSession, "testSectionId", "9002")
                .andExpect(status().isOk()).andExpect(jsonPath("$.queue.totalItems").value(1))
                .andExpect(jsonPath("$.queue.items[0].accessionNumber").value("2026-005678"));
    }

    @Test
    public void getOrdersInProgressQueue_returnsGroupedQueue() throws Exception {
        performQueueGet("/rest/home-dashboard/ORDERS_IN_PROGRESS", receptionSession).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.queue.totalItems").value(1))
                .andExpect(jsonPath("$.queue.items[0].accessionNumber").value("2026-005678"))
                .andExpect(jsonPath("$.queue.items[0].patientName").value("John Smith"))
                .andExpect(jsonPath("$.queue.items[0].testCount").value(1))
                .andExpect(jsonPath("$.queue.items[0].orderDateSort").doesNotExist());
    }

    private ResultActions performQueueGet(String url, MockHttpSession session, String... extraParams) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(url).param("page", "1").session(session);
        for (int i = 0; i + 1 < extraParams.length; i += 2) {
            requestBuilder.param(extraParams[i], extraParams[i + 1]);
        }
        return mockMvc.perform(requestBuilder);
    }

    private MockHttpSession buildSession(int systemUserId) {
        MockHttpSession session = new MockHttpSession();
        UserSessionData userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(systemUserId);
        session.setAttribute(IActionConstants.USER_SESSION_DATA, userSessionData);
        return session;
    }
}
