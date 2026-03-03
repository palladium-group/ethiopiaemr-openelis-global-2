/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */

package org.openelisglobal.analyzer.service;

import jakarta.annotation.PostConstruct;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.PluginMenuService;
import org.openelisglobal.common.services.PluginMenuService.KnownMenu;
import org.openelisglobal.menu.valueholder.Menu;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

/**
 * Registers a "From Analyzer" submenu entry for each analyzer that does not already have one
 * (e.g. plugin-registered). Each entry links to the Analyzer Results page with that analyzer's type,
 * so users can see results sent to that analyzer.
 */
@Service
@DependsOn("pluginLoader")
public class AnalyzerMenuRegistrationService {

    private static final String ANALYZER_RESULTS_ACTION_PREFIX = "/AnalyzerResults?type=";
    private static final String MENU_ELEMENT_ID_PREFIX = "menu_analyzer_";
    private static final int PRESENTATION_ORDER = 100; // after plugin analyzer items

    private final AnalyzerService analyzerService;

    public AnalyzerMenuRegistrationService(AnalyzerService analyzerService) {
        this.analyzerService = analyzerService;
    }

    @PostConstruct
    public void registerAnalyzerMenus() {
        PluginMenuService pluginMenuService = PluginMenuService.getInstance();
        if (pluginMenuService == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "registerAnalyzerMenus",
                    "PluginMenuService not ready; skipping analyzer menu registration.");
            return;
        }

        Menu parent = pluginMenuService.getKnownMenu(KnownMenu.ANALYZER, "menu_results");
        if (parent == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "registerAnalyzerMenus",
                    "From Analyzer parent menu not found; skipping analyzer menu registration.");
            return;
        }

        List<Analyzer> analyzers = analyzerService.getAll();
        int added = 0;
        for (Analyzer analyzer : analyzers) {
            String actionURL = buildActionURL(analyzer.getName());
            if (actionURL == null) {
                continue;
            }
            Menu probe = new Menu();
            probe.setActionURL(actionURL);
            if (pluginMenuService.hasMenu(probe)) {
                continue;
            }
            Menu menu = new Menu();
            menu.setParent(parent);
            menu.setPresentationOrder(PRESENTATION_ORDER);
            menu.setElementId(MENU_ELEMENT_ID_PREFIX + analyzer.getId());
            menu.setActionURL(actionURL);
            menu.setDisplayKey(analyzer.getName());
            menu.setDisplayLabel(analyzer.getName());
            menu.setOpenInNewWindow(false);
            pluginMenuService.addMenu(menu);
            added++;
        }
        if (added > 0) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "registerAnalyzerMenus",
                    "Registered " + added + " analyzer result menu(s) under From Analyzer.");
        }
    }

    private static String buildActionURL(String analyzerName) {
        if (analyzerName == null || analyzerName.isEmpty()) {
            return null;
        }
        try {
            return ANALYZER_RESULTS_ACTION_PREFIX
                    + URLEncoder.encode(analyzerName, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            LogEvent.logError(e);
            return ANALYZER_RESULTS_ACTION_PREFIX + analyzerName.replace(" ", "%20");
        }
    }
}
