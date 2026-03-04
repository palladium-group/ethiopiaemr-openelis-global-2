/*
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.services.PluginMenuService;
import org.openelisglobal.common.services.PluginMenuService.KnownMenu;
import org.openelisglobal.menu.valueholder.Menu;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for AnalyzerMenuRegistrationService. Tests menu registration for
 * non-plugin analyzers.
 */
@RunWith(MockitoJUnitRunner.class)
public class AnalyzerMenuRegistrationServiceTest {

    @Mock
    private AnalyzerService analyzerService;

    @InjectMocks
    private AnalyzerMenuRegistrationService service;

    private PluginMenuService originalPluginMenuService;
    private PluginMenuService mockPluginMenuService;

    @Before
    public void setUp() throws Exception {
        originalPluginMenuService = (PluginMenuService) ReflectionTestUtils.getField(PluginMenuService.class,
                "INSTANCE");
        mockPluginMenuService = mock(PluginMenuService.class);
        ReflectionTestUtils.setField(PluginMenuService.class, "INSTANCE", mockPluginMenuService);
    }

    @After
    public void tearDown() throws Exception {
        ReflectionTestUtils.setField(PluginMenuService.class, "INSTANCE", originalPluginMenuService);
    }

    @Test
    public void testRegisterAnalyzerMenus_WhenPluginMenuServiceNull_DoesNotThrow() throws Exception {
        ReflectionTestUtils.setField(PluginMenuService.class, "INSTANCE", null);

        service.registerAnalyzerMenus();
    }

    @Test
    public void testRegisterAnalyzerMenus_WhenParentMenuNull_DoesNotThrow() throws Exception {
        when(mockPluginMenuService.getKnownMenu(eq(KnownMenu.ANALYZER), eq("menu_results"))).thenReturn(null);

        service.registerAnalyzerMenus();
    }

    @Test
    public void testRegisterAnalyzerMenus_WhenNoAnalyzers_DoesNotAddMenus() throws Exception {
        Menu parent = new Menu();
        parent.setElementId("menu_results_analyzer");
        when(mockPluginMenuService.getKnownMenu(eq(KnownMenu.ANALYZER), eq("menu_results"))).thenReturn(parent);
        when(analyzerService.getAll()).thenReturn(Collections.emptyList());

        service.registerAnalyzerMenus();

        verify(mockPluginMenuService).getKnownMenu(KnownMenu.ANALYZER, "menu_results");
    }

    @Test
    public void testRegisterAnalyzerMenus_WhenAnalyzerHasNoName_SkipsAnalyzer() throws Exception {
        Menu parent = new Menu();
        parent.setElementId("menu_results_analyzer");
        org.openelisglobal.analyzer.valueholder.Analyzer analyzer = new org.openelisglobal.analyzer.valueholder.Analyzer();
        analyzer.setId("1");
        analyzer.setName(null);

        when(mockPluginMenuService.getKnownMenu(eq(KnownMenu.ANALYZER), eq("menu_results"))).thenReturn(parent);
        when(analyzerService.getAll()).thenReturn(Arrays.asList(analyzer));

        service.registerAnalyzerMenus();

        verify(mockPluginMenuService).getKnownMenu(KnownMenu.ANALYZER, "menu_results");
    }

    @Test
    public void testRegisterAnalyzerMenus_WhenAnalyzerNotInPlugin_AddsMenu() throws Exception {
        Menu parent = new Menu();
        parent.setElementId("menu_results_analyzer");
        org.openelisglobal.analyzer.valueholder.Analyzer analyzer = new org.openelisglobal.analyzer.valueholder.Analyzer();
        analyzer.setId("10");
        analyzer.setName("JSON Sample Analyzer");

        when(mockPluginMenuService.getKnownMenu(eq(KnownMenu.ANALYZER), eq("menu_results"))).thenReturn(parent);
        when(analyzerService.getAll()).thenReturn(Arrays.asList(analyzer));
        when(mockPluginMenuService.hasMenu(any(Menu.class))).thenReturn(false);

        service.registerAnalyzerMenus();

        ArgumentCaptor<Menu> menuCaptor = ArgumentCaptor.forClass(Menu.class);
        verify(mockPluginMenuService).addMenu(menuCaptor.capture());

        Menu added = menuCaptor.getValue();
        assertEquals("menu_analyzer_10", added.getElementId());
        assertEquals("/AnalyzerResults?type=JSON+Sample+Analyzer", added.getActionURL());
        assertEquals("JSON Sample Analyzer", added.getDisplayKey());
        assertEquals("JSON Sample Analyzer", added.getDisplayLabel());
        assertEquals(parent, added.getParent());
    }

    @Test
    public void testRegisterAnalyzerMenus_WhenAnalyzerAlreadyInPlugin_SkipsAdding() throws Exception {
        Menu parent = new Menu();
        parent.setElementId("menu_results_analyzer");
        org.openelisglobal.analyzer.valueholder.Analyzer analyzer = new org.openelisglobal.analyzer.valueholder.Analyzer();
        analyzer.setId("1");
        analyzer.setName("SysmexXTAnalyzer");

        when(mockPluginMenuService.getKnownMenu(eq(KnownMenu.ANALYZER), eq("menu_results"))).thenReturn(parent);
        when(analyzerService.getAll()).thenReturn(Arrays.asList(analyzer));
        when(mockPluginMenuService.hasMenu(any(Menu.class))).thenReturn(true);

        service.registerAnalyzerMenus();

        verify(mockPluginMenuService).getKnownMenu(KnownMenu.ANALYZER, "menu_results");
    }
}
