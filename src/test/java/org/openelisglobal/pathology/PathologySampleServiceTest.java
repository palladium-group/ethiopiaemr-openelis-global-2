package org.openelisglobal.pathology;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.dbunit.DatabaseUnitException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.valueholder.pathology.PathologyBlock;
import org.openelisglobal.program.valueholder.pathology.PathologyRead;
import org.openelisglobal.program.valueholder.pathology.PathologyRequest;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.program.valueholder.pathology.PathologySlide;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class PathologySampleServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    PathologySampleService pathologySampleService;

    @Autowired
    SystemUserService systemUserService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/pathology-sample.xml");
    }

    @Test
    public void getAll_shouldReturnAllExistingPathologySamples() {
        Assert.assertEquals(Integer.parseInt("3"), pathologySampleService.getAll().size());
    }

    @Test
    public void getWithStatus_shouldReturnPathologySampleWhenGivenStatus() {
        List<PathologySample> pathologySamples = pathologySampleService
                .getWithStatus(List.of(PathologySample.PathologyStatus.GROSSING));

        Assert.assertNotNull(pathologySamples);
        Assert.assertEquals(PathologySample.PathologyStatus.GROSSING, pathologySamples.get(0).getStatus());
        Assert.assertEquals("John", pathologySamples.get(0).getTechnician().getFirstName());
    }

    @Test
    public void searchWithStatusAndTerm_shouldSearchGivenStatusAndSearchTerm() {
        List<PathologySample> pathologySamples = pathologySampleService
                .searchWithStatusAndTerm(List.of(PathologySample.PathologyStatus.GROSSING), "");

        Assert.assertNotNull(pathologySamples);
        Assert.assertEquals(PathologySample.PathologyStatus.GROSSING, pathologySamples.get(0).getStatus());
        Assert.assertEquals("John", pathologySamples.get(0).getTechnician().getFirstName());
    }

    @Test
    public void assignTechnician_shouldAssignTechnicianToPathologySample() throws SQLException, DatabaseUnitException {
        SystemUser curUser = systemUserService.getUserById("1004");
        pathologySampleService.assignTechnician(2, curUser, "");

        PathologySample pathologySample = pathologySampleService.get(1);

        Assert.assertEquals(Integer.valueOf(1), pathologySample.getId());
        Assert.assertEquals(PathologySample.PathologyStatus.GROSSING, pathologySample.getStatus());
        Assert.assertEquals("John", pathologySample.getTechnician().getFirstName());
        Assert.assertEquals(Integer.parseInt("1"), pathologySample.getBlocks().size());
    }

    @Test
    public void assignPathologist_shouldAssignPathologistToPathologySample() {
        SystemUser curUser = systemUserService.getUserById("1004");
        pathologySampleService.assignPathologist(2, curUser, "");

        PathologySample pathologySample = pathologySampleService.get(1);

        Assert.assertEquals(Integer.valueOf(1), pathologySample.getId());
        Assert.assertEquals(PathologySample.PathologyStatus.GROSSING, pathologySample.getStatus());
        Assert.assertEquals("John", pathologySample.getTechnician().getFirstName());
    }

    @Test
    public void getCountWithStatus_shouldCountPathologySampleWithStatus() {
        Long pathologyStatuses = pathologySampleService
                .getCountWithStatus(Collections.singletonList(PathologySample.PathologyStatus.GROSSING));

        Assert.assertEquals(Long.valueOf(2), pathologyStatuses);
    }

    @Test
    public void getCountWithStatusBetweenDates_shouldCountPathologySampleWithStatusBetweenDates() {
        Long expectedCount = 2L;
        Long countWithStatusBetweenDates = pathologySampleService.getCountWithStatusBetweenDates(
                Collections.singletonList(PathologySample.PathologyStatus.GROSSING),
                Timestamp.valueOf("2024-06-10 12:00:00.0"), Timestamp.valueOf("2024-07-10 12:00:00.0"));

        Assert.assertEquals(expectedCount, countWithStatusBetweenDates);
    }

    @Test
    public void updateWithFormValues_shouldUpdatePathologySampleWithFormValues() {
        PathologySampleForm pathologySampleForm = new PathologySampleForm();
        pathologySampleForm.setSystemUserId("2");

        PathologyBlock block1 = new PathologyBlock();
        block1.setBlockNumber(12);
        block1.setLocation("Lab 2");

        PathologyBlock block2 = new PathologyBlock();
        block2.setBlockNumber(13);
        block2.setLocation("Lab 3");

        List<PathologyBlock> pathologyBlocks = Arrays.asList(block1, block2);

        pathologySampleForm.setBlocks(pathologyBlocks);
        pathologySampleForm.setSlides(Collections.singletonList(new PathologySampleForm.PathologySlideForm()));
        pathologySampleForm.setReports(Collections.singletonList(new PathologySampleForm.PathologyReportForm()));
        pathologySampleService.updateWithFormValues(2, pathologySampleForm);

        Assert.assertEquals(Integer.parseInt("2"), pathologySampleForm.getBlocks().size());
        Assert.assertEquals("2", pathologySampleForm.getSystemUserId());
    }

    @Test
    @Transactional
    public void requestSpecialStains_shouldOpenAdditionalRequestWithStainTechniqueAndFinalizedRound() {
        pathologySampleService.requestSpecialStains(3, "micro round 1", "conclusion round 1", Collections.emptyList(),
                List.of("PAS"), "1002");

        PathologySample sample = pathologySampleService.get(3);
        Assert.assertEquals(PathologySample.PathologyStatus.ADDITIONAL_REQUEST, sample.getStatus());
        Assert.assertTrue("stain recorded as a technique",
                sample.getTechniques().stream().anyMatch(t -> "PAS".equals(t.getValue())));
        Assert.assertTrue("an OPENED special-stain request exists", sample.getRequests().stream()
                .anyMatch(r -> r.getStatus() == PathologyRequest.RequestStatus.OPENED && "PAS".equals(r.getValue())));

        Assert.assertEquals(1, sample.getReads().size());
        PathologyRead round1 = sample.getReads().get(0);
        Assert.assertEquals(Integer.valueOf(1), round1.getRoundNumber());
        Assert.assertTrue("round 1 finalized", round1.getFinalized());
        Assert.assertEquals("micro round 1", round1.getMicroscopyExam());
    }

    @Test
    @Transactional
    public void startSpecialStain_shouldMoveAdditionalRequestToSlicing() {
        pathologySampleService.requestSpecialStains(3, "m", "c", Collections.emptyList(), List.of("PAS"), "1002");
        pathologySampleService.startSpecialStain(3, "1001");

        Assert.assertEquals(PathologySample.PathologyStatus.SLICING, pathologySampleService.get(3).getStatus());
    }

    @Test
    @Transactional
    public void cutSlide_shouldCreateStainedControlSlideForSpecialStainRound() {
        pathologySampleService.requestSpecialStains(3, "m", "c", Collections.emptyList(), List.of("PAS"), "1002");
        pathologySampleService.startSpecialStain(3, "1001");
        pathologySampleService.cutSlide(3, 103, "PAS", PathologySlide.SlideRole.CONTROL_POS, "1001");

        PathologySlide control = pathologySampleService.get(3).getSlides().stream()
                .filter(s -> s.getSlideRole() == PathologySlide.SlideRole.CONTROL_POS).findFirst().orElseThrow();
        Assert.assertEquals("PAS", control.getStainType());
        Assert.assertEquals(Integer.valueOf(103), control.getPathologyBlockId());
    }

    @Test
    @Transactional
    public void specialStainReLoop_shouldReturnToReadyCompleteRequestAndAppendRound() {
        // Round 1 read, request a PAS special stain.
        pathologySampleService.requestSpecialStains(3, "micro round 1", "conclusion round 1", Collections.emptyList(),
                List.of("PAS"), "1002");
        // Tech picks it up and cuts a new PAS section from the existing block 103.
        pathologySampleService.startSpecialStain(3, "1001");
        pathologySampleService.cutSlide(3, 103, "PAS", PathologySlide.SlideRole.PATIENT, "1001");

        PathologySlide newSlide = pathologySampleService.get(3).getSlides().stream()
                .filter(s -> s.getConfirmedAt() == null).findFirst().orElseThrow();

        pathologySampleService.confirmSlide(3, newSlide.getId(), "1001");
        Assert.assertEquals(PathologySample.PathologyStatus.STAINING, pathologySampleService.get(3).getStatus());

        pathologySampleService.markSlideStained(3, newSlide.getId(), "1001");
        PathologySample afterStain = pathologySampleService.get(3);
        Assert.assertEquals(PathologySample.PathologyStatus.READY_PATHOLOGIST, afterStain.getStatus());
        Assert.assertTrue("open request completed once staining finished", afterStain.getRequests().stream()
                .allMatch(r -> r.getStatus() == PathologyRequest.RequestStatus.COMPLETED));
        Assert.assertTrue("new slide carries its stain type",
                afterStain.getSlides().stream().anyMatch(s -> "PAS".equals(s.getStainType())));

        // Second read opens round 2 and preserves the finalized round 1.
        pathologySampleService.saveReadDraft(3, "micro round 2", "conclusion round 2", Collections.emptyList(), "1002");
        PathologySample afterRead2 = pathologySampleService.get(3);
        Assert.assertEquals(2, afterRead2.getReads().size());
        Assert.assertTrue("round 1 preserved and finalized", afterRead2.getReads().stream()
                .anyMatch(r -> Integer.valueOf(1).equals(r.getRoundNumber()) && Boolean.TRUE.equals(r.getFinalized())));
        Assert.assertTrue("round 2 open", afterRead2.getReads().stream().anyMatch(
                r -> Integer.valueOf(2).equals(r.getRoundNumber()) && !Boolean.TRUE.equals(r.getFinalized())));
    }

}
