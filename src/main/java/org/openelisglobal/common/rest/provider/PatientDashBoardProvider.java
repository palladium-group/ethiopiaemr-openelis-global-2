package org.openelisglobal.common.rest.provider;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.rest.provider.bean.homedashboard.AverageTimeDisplayBean;
import org.openelisglobal.common.rest.provider.bean.homedashboard.DashBoardMetrics;
import org.openelisglobal.common.rest.provider.bean.homedashboard.DashBoardTile;
import org.openelisglobal.common.rest.provider.bean.homedashboard.DashboardQueueItemDTO;
import org.openelisglobal.common.rest.provider.bean.homedashboard.OrderDisplayBean;
import org.openelisglobal.common.rest.provider.form.PatientDashBoardForm;
import org.openelisglobal.common.rest.util.PatientDashBoardPaging;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.ExternalOrderStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.reception.dao.ReceptionDAO;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/rest/")
public class PatientDashBoardProvider extends BaseRestController {

    @Autowired
    AnalysisService analysisService;

    @Autowired
    IStatusService iStatusService;

    @Autowired
    ElectronicOrderService electronicOrderService;

    @Autowired
    SampleHumanService sampleHumanService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private FhirUtil fhirUtil;

    @Autowired
    private FhirConfig fhirConfig;

    @Autowired
    private TestService testService;

    @Autowired
    SystemUserService systemUserService;
    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private DashboardQueueMapper dashboardQueueMapper;

    @Autowired
    private ReceptionDAO receptionDAO;

    private double calculateAverageReceptionToValidationTime() {
        List<Analysis> analyses = analysisService.getAnalysesCompletedOnByStatusId(DateUtil.getNowAsSqlDate(),
                iStatusService.getStatusID(AnalysisStatus.Finalized));

        List<Long> hours = new ArrayList<>();
        analyses.forEach(analysis -> {
            // Convert java.sql.Date to java.time.LocalDate
            LocalDate localStartDate = analysis.getStartedDate().toLocalDate();
            LocalDate localEndDate = analysis.getReleasedDate().toLocalDate();
            // Calculate time difference in hours
            Long hoursDiff = Duration.between(localStartDate.atStartOfDay(), localEndDate.atStartOfDay()).toHours();
            hours.add(hoursDiff);
        });

        long sum = 0;
        if (!hours.isEmpty()) {
            for (Long h : hours) {
                sum += h;
            }
        }
        return hours.isEmpty() ? 0.0 : (double) sum / hours.size();
    }

    private double calculateAverageReceptionToResultTime() {
        Set<Integer> statusIdSet = new HashSet<>();
        statusIdSet.add(Integer.parseInt(iStatusService.getStatusID(AnalysisStatus.SampleRejected)));
        List<Analysis> analyses = analysisService
                .getAnalysesResultEnteredOnExcludedByStatusId(DateUtil.getNowAsSqlDate(), statusIdSet);

        List<Long> hours = new ArrayList<>();
        analyses.forEach(analysis -> {
            // Convert java.sql.Date to java.time.LocalDate
            LocalDate localStartDate = analysis.getStartedDate().toLocalDate();
            LocalDate localEndDate = analysis.getCompletedDate().toLocalDate();
            // Calculate time difference in hours
            Long hoursDiff = Duration.between(localStartDate.atStartOfDay(), localEndDate.atStartOfDay()).toHours();
            hours.add(hoursDiff);
        });

        long sum = 0;
        if (!hours.isEmpty()) {
            for (Long h : hours) {
                sum += h;
            }
        }
        return hours.isEmpty() ? 0.0 : (double) sum / hours.size();
    }

    private double calculateAverageResultToValidationTime() {
        List<Analysis> analyses = analysisService.getAnalysesCompletedOnByStatusId(DateUtil.getNowAsSqlDate(),
                iStatusService.getStatusID(AnalysisStatus.Finalized));

        List<Long> hours = new ArrayList<>();
        analyses.forEach(analysis -> {
            // Convert java.sql.Date to java.time.LocalDate
            LocalDate localStartDate = analysis.getCompletedDate().toLocalDate();
            LocalDate localEndDate = analysis.getReleasedDate().toLocalDate();
            // Calculate time difference in hours
            Long hoursDiff = Duration.between(localStartDate.atStartOfDay(), localEndDate.atStartOfDay()).toHours();
            hours.add(hoursDiff);
        });

        long sum = 0;
        if (!hours.isEmpty()) {
            for (Long h : hours) {
                sum += h;
            }
        }
        return hours.isEmpty() ? 0.0 : (double) sum / hours.size();
    }

    private List<Analysis> analysesWithDelayedTurnAroundTime() {
        List<Analysis> analyses = analysisService.getAnalysesCompletedOnByStatusId(DateUtil.getNowAsSqlDate(),
                iStatusService.getStatusID(AnalysisStatus.Finalized));

        List<Analysis> delayedAnalyses = new ArrayList<>();
        analyses.forEach(analysis -> {
            // Convert java.sql.Date to java.time.LocalDate
            LocalDate localStartDate = analysis.getStartedDate().toLocalDate();
            LocalDate localEndDate = analysis.getReleasedDate().toLocalDate();
            // Calculate time difference in hours
            Long hoursDiff = Duration.between(localStartDate.atStartOfDay(), localEndDate.atStartOfDay()).toHours();
            if (hoursDiff > 96) {
                delayedAnalyses.add(analysis);
            }
        });
        return delayedAnalyses;
    }

    private List<Analysis> unprintedResults() {
        List<Analysis> analyses = analysisService.getAnalysesCompletedOnByStatusId(DateUtil.getNowAsSqlDate(),
                iStatusService.getStatusID(AnalysisStatus.Finalized));

        List<Analysis> unprintedAnalyses = new ArrayList<>();
        if (analyses == null) {
            return unprintedAnalyses;
        }
        analyses.forEach(a -> {
            if (!analysisService.patientReportHasBeenDone(a)) {
                unprintedAnalyses.add(a);
            }
        });
        return unprintedAnalyses;
    }

    private List<OrderDisplayBean> convertAnalysesToOrderBean(List<Analysis> analyses) {
        List<OrderDisplayBean> orderBeanList = new ArrayList<>();
        if (analyses != null) {
            analyses.forEach(analysis -> {
                if (analysis != null) {
                    OrderDisplayBean orderBean = new OrderDisplayBean();
                    orderBean.setId(analysis.getId());
                    Sample sample = analysis.getSampleItem() != null ? analysis.getSampleItem().getSample() : null;
                    if (sample != null) {
                        orderBean.setPriority(sample.getPriority() != null ? sample.getPriority().toString() : "");
                        orderBean.setLabNumber(sample.getAccessionNumber() != null ? sample.getAccessionNumber() : "");
                        Patient patient = sampleHumanService.getPatientForSample(sample);
                        if (patient != null && patient.getNationalId() != null) {
                            orderBean.setPatientId(patient.getNationalId());
                        }
                    }
                    orderBean.setOrderDate(analysis.getStartedDateForDisplay());
                    orderBean.setTestName(analysis.getTest() != null ? analysis.getTest().getLocalizedName() : "");
                    orderBean
                            .setTestSection(analysis.getTestSection() != null ? analysis.getTestSection().getId() : "");
                    orderBeanList.add(orderBean);
                }
            });
        }

        return orderBeanList;
    }

    private List<OrderDisplayBean> convertAnalysesToUserOrdersBean(List<Analysis> analyses) {
        List<OrderDisplayBean> userOrders = new ArrayList<>();
        Map<String, List<Analysis>> userOrdersMap = new HashMap<>();
        analyses.forEach(analysis -> {
            String systemUserId = analysis.getSampleItem().getSample().getSysUserId();
            if (userOrdersMap.containsKey(systemUserId)) {
                userOrdersMap.get(systemUserId).add(analysis);
            } else {
                List<Analysis> userAnalyses = new ArrayList<>();
                userAnalyses.add(analysis);
                userOrdersMap.put(systemUserId, userAnalyses);
            }
        });

        userOrdersMap.forEach((userId, analysisList) -> {
            OrderDisplayBean userOrderBean = new OrderDisplayBean();
            SystemUser user = systemUserService.get(userId);
            if (user != null) {
                userOrderBean.setId(userId);
                userOrderBean.setUserFirstName(user.getFirstName());
                userOrderBean.setUserLastName(user.getLastName());
                userOrderBean.setCountOfOrdersEntered(countDistinctAccessions(analysisList));
                userOrders.add(userOrderBean);
            }
        });
        return userOrders;
    }

    private int countDistinctAccessions(List<Analysis> analyses) {
        if (analyses == null || analyses.isEmpty()) {
            return 0;
        }
        Set<String> accessionNumbers = new HashSet<>();
        for (Analysis analysis : analyses) {
            if (analysis == null || analysis.getSampleItem() == null || analysis.getSampleItem().getSample() == null) {
                continue;
            }
            String accessionNumber = analysis.getSampleItem().getSample().getAccessionNumber();
            if (StringUtils.isNotBlank(accessionNumber)) {
                accessionNumbers.add(accessionNumber);
            }
        }
        return accessionNumbers.size();
    }

    private List<OrderDisplayBean> getUserOrderBeans(List<Analysis> analyses, String userId) {
        Map<String, List<Analysis>> userOrdersMap = new HashMap<>();
        analyses.forEach(analysis -> {
            String systemUserId = analysis.getSampleItem().getSample().getSysUserId();
            if (userOrdersMap.containsKey(systemUserId)) {
                userOrdersMap.get(systemUserId).add(analysis);
            } else {
                List<Analysis> userAnalyses = new ArrayList<>();
                userAnalyses.add(analysis);
                userOrdersMap.put(systemUserId, userAnalyses);
            }
        });

        if (userOrdersMap.get(userId) != null) {
            return convertAnalysesToOrderBean(userOrdersMap.get(userId));
        }
        return new ArrayList<>();
    }

    private List<OrderDisplayBean> convertElectronicToOrderBean(List<ElectronicOrder> eOrders) {
        List<OrderDisplayBean> orderBeanList = new ArrayList<>();
        eOrders.forEach(eOrder -> {
            OrderDisplayBean orderBean = new OrderDisplayBean();
            orderBean.setId(eOrder.getId());
            orderBean.setPriority(eOrder.getPriority().toString());
            orderBean.setOrderDate(DateUtil.convertTimestampToStringDate(eOrder.getOrderTimestamp()));
            Sample sample = sampleService.getSampleByReferringId(eOrder.getExternalId());
            if (sample != null) {
                orderBean.setLabNumber(sample.getAccessionNumber());
            }

            Test test = null;
            try {
                IGenericClient fhirClient = fhirUtil.getFhirClient(fhirConfig.getLocalFhirStorePath());
                ServiceRequest serviceRequest = fhirClient.read().resource(ServiceRequest.class)
                        .withId(eOrder.getExternalId()).execute();
                for (Coding coding : serviceRequest.getCode().getCoding()) {
                    if (coding.hasSystem()) {
                        if (coding.getSystem().equalsIgnoreCase("http://loinc.org")) {
                            List<Test> tests = testService.getActiveTestsByLoinc(coding.getCode());
                            if (tests.size() != 0) {
                                test = tests.get(0);
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {

            }
            if (test != null) {
                orderBean.setTestName(test.getLocalizedTestName().getLocalizedValue());
            }

            orderBean.setPatientId(eOrder.getPatient().getNationalId());
            orderBeanList.add(orderBean);
        });

        return orderBeanList;
    }

    @GetMapping(value = "home-dashboard/metrics", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public DashBoardMetrics getDasBoardTiles(HttpServletRequest request) {

        DashBoardMetrics metrics = new DashBoardMetrics();
        boolean canViewReception = userHasSampleReceptionApprovalRole(request);
        java.sql.Timestamp startTimestamp = DateUtil
                .convertStringDateStringTimeToTimestamp(DateUtil.getCurrentDateAsText(), "00:00:00.0");
        java.sql.Timestamp endTimestamp = DateUtil
                .convertStringDateStringTimeToTimestamp(DateUtil.getCurrentDateAsText(), "23:59:59");
        DashBoardTile.TileType.stream().forEach(type -> {
            List<Integer> statusIdList;
            Set<Integer> statusIdSet;
            switch (type) {
            case ORDERS_IN_PROGRESS:
                metrics.setOrdersInProgress(dashboardQueueMapper
                        .countTestUnitsForAnalyses(retrieveAnalysesByStatus(AnalysisStatus.NotStarted)));
                break;
            case ORDERS_READY_FOR_VALIDATION:
                metrics.setOrdersReadyForValidation(dashboardQueueMapper
                        .countTestUnitsForAnalyses(retrieveAnalysesByStatus(AnalysisStatus.TechnicalAcceptance)));
                break;
            case ORDERS_COMPLETED_TODAY:
                metrics.setOrdersCompletedToday(
                        dashboardQueueMapper.countTestUnitsForAnalyses(analysisService.getAnalysesCompletedOnByStatusId(
                                DateUtil.getNowAsSqlDate(), iStatusService.getStatusID(AnalysisStatus.Finalized))));
                break;
            case ORDERS_PATIALLY_COMPLETED_TODAY:
                statusIdSet = new HashSet<>();
                statusIdSet.add(Integer.parseInt(iStatusService.getStatusID(AnalysisStatus.SampleRejected)));
                statusIdSet.add(Integer.parseInt(iStatusService.getStatusID(AnalysisStatus.Finalized)));
                metrics.setPatiallyCompletedToday(dashboardQueueMapper.countTestUnitsForAnalyses(analysisService
                        .getAnalysisStartedOnExcludedByStatusId(DateUtil.getNowAsSqlDate(), statusIdSet)));
                break;

            case ORDERS_ENTERED_BY_USER_TODAY:
                statusIdSet = new HashSet<>();
                statusIdSet.add(Integer.parseInt(iStatusService.getStatusID(AnalysisStatus.SampleRejected)));
                metrics.setOrderEnterdByUserToday(countDistinctAccessions(analysisService
                        .getAnalysisStartedOnExcludedByStatusId(DateUtil.getNowAsSqlDate(), statusIdSet)));
                break;
            case ORDERS_REJECTED_TODAY:
                metrics.setOrdersRejectedToday(dashboardQueueMapper.countTestUnitsForAnalyses(analysisService
                        .getAnalysisStartedOnRangeByStatusId(DateUtil.getNowAsSqlDate(), DateUtil.getNowAsSqlDate(),
                                iStatusService.getStatusID(AnalysisStatus.SampleRejected))));
                break;
            case UN_PRINTED_RESULTS:
                metrics.setUnPritendResults(dashboardQueueMapper.countTestUnitsForAnalyses(unprintedResults()));
                break;
            case INCOMING_ORDERS:
                List<Integer> estausIds = new ArrayList<>();
                estausIds.add(Integer.parseInt(iStatusService.getStatusID(ExternalOrderStatus.Entered)));
                estausIds.add(Integer.parseInt(iStatusService.getStatusID(ExternalOrderStatus.NonConforming)));
                metrics.setIncomigOrders(electronicOrderService.getCountOfElectronicOrdersByStatusList(estausIds));
                break;
            case PENDING_RECEPTION:
                if (canViewReception) {
                    metrics.setPendingReception(dashboardQueueMapper
                            .countTestUnitsForAnalyses(retrieveAnalysesByStatus(AnalysisStatus.PendingReception)));
                } else {
                    metrics.setPendingReception(0);
                }
                break;
            case AVERAGE_TURN_AROUND_TIME:
                metrics.setAverageTurnAroudTime(calculateAverageReceptionToValidationTime());
                break;
            case DELAYED_TURN_AROUND:
                metrics.setDelayedTurnAround(
                        dashboardQueueMapper.countTestUnitsForAnalyses(analysesWithDelayedTurnAroundTime()));
                break;
            default:
                break;
            }
        });

        return metrics;
    }

    /**
     * Get the list of orders to be displayed on the dashboard. It will returna a
     * list of orders based on the type of the list in paginated manner.
     */
    @GetMapping(value = "home-dashboard/{listType}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public PatientDashBoardForm getDashBoardDisplayList(HttpServletRequest request,
            @PathVariable DashBoardTile.TileType listType, @RequestParam(required = false) String systemUserId)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {

        PatientDashBoardForm response = new PatientDashBoardForm();
        PatientDashBoardPaging paging = new PatientDashBoardPaging();
        List<OrderDisplayBean> orderDisplayBeans = new ArrayList<>();

        String requestedPage = request.getParameter("page");
        if (GenericValidator.isBlankOrNull(requestedPage) && !DashboardQueueMapper.usesQueueView(listType)) {
            orderDisplayBeans = retreiveOrders(listType, systemUserId, request);

            // All the orders retreived are fed into paging to return the first page of the
            // list.
            paging.setDatabaseResults(request, response, orderDisplayBeans);
        } else if (!GenericValidator.isBlankOrNull(requestedPage) && !DashboardQueueMapper.usesQueueView(listType)) {
            int requestedPageNumber = Integer.parseInt(requestedPage);

            // Sets the requested page in the response.
            paging.page(request, response, requestedPageNumber);
        }
        populateQueueResponse(listType, systemUserId, request, response);

        return response;
    }

    private void populateQueueResponse(DashBoardTile.TileType listType, String systemUserId, HttpServletRequest request,
            PatientDashBoardForm response) {
        if (!DashboardQueueMapper.usesQueueView(listType)) {
            return;
        }
        List<Analysis> analyses = retrieveAnalyses(listType, systemUserId, request);
        List<DashboardQueueItemDTO> queueItems = dashboardQueueMapper.groupAnalysesByAccession(analyses);
        queueItems = dashboardQueueMapper.filterQueueItems(queueItems, request.getParameter("patientQuery"),
                request.getParameter("labNumber"));
        int page = parseQueuePage(request.getParameter("page"));
        int pageSize = parseQueuePageSize(request.getParameter("pageSize"));
        response.setQueue(dashboardQueueMapper.buildQueueResponse(queueItems, page, pageSize));
    }

    private int parseQueuePage(String pageValue) {
        if (GenericValidator.isBlankOrNull(pageValue)) {
            return DashboardQueueMapper.DEFAULT_PAGE;
        }
        try {
            return Integer.parseInt(pageValue);
        } catch (NumberFormatException e) {
            return DashboardQueueMapper.DEFAULT_PAGE;
        }
    }

    private int parseQueuePageSize(String pageSizeValue) {
        if (GenericValidator.isBlankOrNull(pageSizeValue)) {
            return DashboardQueueMapper.DEFAULT_PAGE_SIZE;
        }
        try {
            return Integer.parseInt(pageSizeValue);
        } catch (NumberFormatException e) {
            return DashboardQueueMapper.DEFAULT_PAGE_SIZE;
        }
    }

    /**
     * Returns the list of orders based on the type of the list provided by the
     * getdashBoardDisplayList method.
     */
    private List<OrderDisplayBean> retreiveOrders(DashBoardTile.TileType listType, String systemUserId,
            HttpServletRequest request) {
        switch (listType) {
        case ORDERS_IN_PROGRESS:
        case ORDERS_READY_FOR_VALIDATION:
        case ORDERS_COMPLETED_TODAY:
        case ORDERS_PATIALLY_COMPLETED_TODAY:
        case ORDERS_REJECTED_TODAY:
        case UN_PRINTED_RESULTS:
        case PENDING_RECEPTION:
        case DELAYED_TURN_AROUND:
        case ORDERS_FOR_USER:
            return convertAnalysesToOrderBean(retrieveAnalyses(listType, systemUserId, request));
        case ORDERS_ENTERED_BY_USER_TODAY:
            Set<Integer> statusIdSet = new HashSet<>();
            statusIdSet.add(Integer.parseInt(iStatusService.getStatusID(AnalysisStatus.SampleRejected)));
            return convertAnalysesToUserOrdersBean(
                    analysisService.getAnalysisStartedOnExcludedByStatusId(DateUtil.getNowAsSqlDate(), statusIdSet));
        case INCOMING_ORDERS:
            List<Integer> estausIds = new ArrayList<>();
            estausIds.add(Integer.parseInt(iStatusService.getStatusID(ExternalOrderStatus.Entered)));
            estausIds.add(Integer.parseInt(iStatusService.getStatusID(ExternalOrderStatus.NonConforming)));
            List<ElectronicOrder> eOrders = electronicOrderService.getAllElectronicOrdersByStatusList(estausIds,
                    ElectronicOrder.SortOrder.STATUS_ID);
            return convertElectronicToOrderBean(eOrders);
        case AVERAGE_TURN_AROUND_TIME:
        default:
            return new ArrayList<>();
        }
    }

    private List<Analysis> retrieveAnalyses(DashBoardTile.TileType listType, String systemUserId,
            HttpServletRequest request) {
        Set<Integer> statusIdSet;
        switch (listType) {
        case ORDERS_IN_PROGRESS:
            return retrieveAnalysesByStatus(AnalysisStatus.NotStarted);
        case ORDERS_READY_FOR_VALIDATION:
            return retrieveAnalysesByStatus(AnalysisStatus.TechnicalAcceptance);
        case ORDERS_COMPLETED_TODAY:
            return analysisService.getAnalysesCompletedOnByStatusId(DateUtil.getNowAsSqlDate(),
                    iStatusService.getStatusID(AnalysisStatus.Finalized));
        case ORDERS_PATIALLY_COMPLETED_TODAY:
            statusIdSet = new HashSet<>();
            statusIdSet.add(Integer.parseInt(iStatusService.getStatusID(AnalysisStatus.SampleRejected)));
            statusIdSet.add(Integer.parseInt(iStatusService.getStatusID(AnalysisStatus.Finalized)));
            return analysisService.getAnalysisStartedOnExcludedByStatusId(DateUtil.getNowAsSqlDate(), statusIdSet);
        case ORDERS_REJECTED_TODAY:
            return analysisService.getAnalysisStartedOnRangeByStatusId(DateUtil.getNowAsSqlDate(),
                    DateUtil.getNowAsSqlDate(), iStatusService.getStatusID(AnalysisStatus.SampleRejected));
        case UN_PRINTED_RESULTS:
            return unprintedResults();
        case PENDING_RECEPTION:
            if (!userHasSampleReceptionApprovalRole(request)) {
                return new ArrayList<>();
            }
            return retrieveAnalysesByStatus(AnalysisStatus.PendingReception);
        case DELAYED_TURN_AROUND:
            return analysesWithDelayedTurnAroundTime();
        case ORDERS_FOR_USER:
            if (StringUtils.isNotBlank(systemUserId)) {
                statusIdSet = new HashSet<>();
                statusIdSet.add(Integer.parseInt(iStatusService.getStatusID(AnalysisStatus.SampleRejected)));
                List<Analysis> analyses = analysisService
                        .getAnalysisStartedOnExcludedByStatusId(DateUtil.getNowAsSqlDate(), statusIdSet);
                return filterAnalysesForUser(analyses, systemUserId);
            }
            return new ArrayList<>();
        default:
            return new ArrayList<>();
        }
    }

    private List<Analysis> filterAnalysesForUser(List<Analysis> analyses, String userId) {
        List<Analysis> userAnalyses = new ArrayList<>();
        if (analyses == null) {
            return userAnalyses;
        }
        for (Analysis analysis : analyses) {
            if (analysis.getSampleItem() != null && analysis.getSampleItem().getSample() != null
                    && userId.equals(analysis.getSampleItem().getSample().getSysUserId())) {
                userAnalyses.add(analysis);
            }
        }
        return userAnalyses;
    }

    private List<Analysis> retrieveAnalysesByStatus(AnalysisStatus status) {
        return receptionDAO.findAnalysesByStatusWithSampleFilters(iStatusService.getStatusID(status), null, null, null);
    }

    private boolean userHasSampleReceptionApprovalRole(HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return false;
        }
        return userRoleService.userInRole(sysUserId, Constants.ROLE_SAMPLE_RECEPTION_APPROVAL);
    }

    @GetMapping(value = "home-dashboard/turn-around-time-metrics", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public AverageTimeDisplayBean getDasBoardAverageTurnAroundTime() {
        AverageTimeDisplayBean timeBean = new AverageTimeDisplayBean();
        timeBean.setReceptionToResult(calculateAverageReceptionToResultTime());
        timeBean.setReceptionToValidation(calculateAverageReceptionToValidationTime());
        timeBean.setResultToValidation(calculateAverageResultToValidationTime());
        return timeBean;
    }
}
