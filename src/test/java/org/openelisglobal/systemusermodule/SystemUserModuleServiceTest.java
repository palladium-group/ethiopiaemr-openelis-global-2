package org.openelisglobal.systemusermodule;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.systemusermodule.service.SystemUserModuleService;
import org.openelisglobal.systemusermodule.valueholder.SystemUserModule;
import org.springframework.beans.factory.annotation.Autowired;

public class SystemUserModuleServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SystemUserModuleService systemUserModuleService;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/system-user-module.xml");
    }

    @Test
    public void getData_ShouldReturnDataForASystemUserModule() {
        SystemUserModule systemUserModule = systemUserModuleService.get("2");
        systemUserModuleService.getData(systemUserModule);
        assertNotNull(systemUserModule);
        assertEquals("Y", systemUserModule.getHasSelect());
    }

    @Test
    public void getAllPermissionModules_ShouldReturnAllSystemUserModules() {
        List<SystemUserModule> systemUserModules = systemUserModuleService.getAllPermissionModules();
        assertNotNull(systemUserModules);
        assertEquals(3, systemUserModules.size());
        assertEquals("Y", systemUserModules.get(0).getHasUpdate());
    }

    @Test
    public void getTotalPermissionModuleCount_ShouldReturnNumberOfSystemUserModules() {
        Integer systemUserModuleCount = systemUserModuleService.getTotalPermissionModuleCount();
        assertNotNull(systemUserModuleCount);
        assertEquals(3, (int) systemUserModuleCount);
    }

    @Test
    public void getPageOfPermissionModules_ShouldReturnAPageOfSystemUserModules() {
        int numberOfPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));

    }

}
