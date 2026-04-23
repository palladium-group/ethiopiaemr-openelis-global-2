package org.openelisglobal.sample.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class SamplePatientEntryRestControllerTest {

    private SamplePatientEntryRestController controller;

    @Mock
    private OrganizationService organizationService;

    @Before
    public void setUp() {
        controller = new SamplePatientEntryRestController();
        ReflectionTestUtils.setField(controller, "organizationService", organizationService);
    }

    @Test
    public void normalizeHealthRegionAndDistrictIdsIfNeeded_ShouldResolveNamesToIds() {
        PatientManagementInfo patientInfo = new PatientManagementInfo();
        patientInfo.setHealthRegion("Oromia Region");
        patientInfo.setHealthDistrict("West Shewa Zone");

        Organization region = mock(Organization.class);
        when(region.getId()).thenReturn("100");

        Organization district = mock(Organization.class);
        when(district.getId()).thenReturn("200");
        when(district.getOrganizationName()).thenReturn("West Shewa Zone");

        when(organizationService.getOrganizationByName(org.mockito.ArgumentMatchers.any(Organization.class),
                org.mockito.ArgumentMatchers.eq(true))).thenReturn(region);
        when(organizationService.getOrganizationsByParentId("100")).thenReturn(List.of(district));

        ReflectionTestUtils.invokeMethod(controller, "normalizeHealthRegionAndDistrictIdsIfNeeded", patientInfo);

        assertEquals("100", patientInfo.getHealthRegion());
        assertEquals("200", patientInfo.getHealthDistrict());
    }

    @Test
    public void normalizeHealthRegionAndDistrictIdsIfNeeded_ShouldSkipWhenAlreadyIds() {
        PatientManagementInfo patientInfo = new PatientManagementInfo();
        patientInfo.setHealthRegion("100");
        patientInfo.setHealthDistrict("200");

        ReflectionTestUtils.invokeMethod(controller, "normalizeHealthRegionAndDistrictIdsIfNeeded", patientInfo);

        assertEquals("100", patientInfo.getHealthRegion());
        assertEquals("200", patientInfo.getHealthDistrict());
        verify(organizationService, never()).getOrganizationByName(org.mockito.ArgumentMatchers.any(Organization.class),
                org.mockito.ArgumentMatchers.eq(true));
        verify(organizationService, never()).getOrganizationsByParentId(org.mockito.ArgumentMatchers.anyString());
    }
}
