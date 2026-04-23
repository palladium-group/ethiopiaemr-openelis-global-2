package org.openelisglobal.dataexchange.order.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class DBOrderPersisterTest {

    private DBOrderPersister persister;

    @Mock
    private OrganizationService organizationService;

    @Before
    public void setUp() {
        persister = new DBOrderPersister();
        ReflectionTestUtils.setField(persister, "organizationService", organizationService);
    }

    @Test
    public void normalizeHealthRegionAndDistrictIdsIfNeeded_ShouldResolveNamesToIds() {
        MessagePatient orderPatient = new MessagePatient();
        orderPatient.setHealthRegion("Oromia Region");
        orderPatient.setHealthDistrict("West Shewa Zone");

        Organization region = new Organization();
        region.setId("100");
        region.setOrganizationName("Oromia Region");

        Organization district = new Organization();
        district.setId("200");
        district.setOrganizationName("West Shewa Zone");

        when(organizationService.getOrganizationByName(any(Organization.class), eq(true))).thenReturn(region);
        when(organizationService.getOrganizationsByParentId("100")).thenReturn(List.of(district));

        ReflectionTestUtils.invokeMethod(persister, "normalizeHealthRegionAndDistrictIdsIfNeeded", orderPatient);

        assertEquals("100", orderPatient.getHealthRegion());
        assertEquals("200", orderPatient.getHealthDistrict());
    }

    @Test
    public void normalizeHealthRegionAndDistrictIdsIfNeeded_ShouldSkipLookupWhenIds() {
        MessagePatient orderPatient = new MessagePatient();
        orderPatient.setHealthRegion("100");
        orderPatient.setHealthDistrict("200");

        ReflectionTestUtils.invokeMethod(persister, "normalizeHealthRegionAndDistrictIdsIfNeeded", orderPatient);

        assertNotNull(orderPatient.getHealthRegion());
        assertNotNull(orderPatient.getHealthDistrict());
        verify(organizationService, never()).getOrganizationByName(any(Organization.class), eq(true));
        verify(organizationService, never()).getOrganizationsByParentId(any(String.class));
    }
}
