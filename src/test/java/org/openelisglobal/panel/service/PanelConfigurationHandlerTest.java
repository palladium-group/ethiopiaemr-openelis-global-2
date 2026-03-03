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
 * Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.panel.service;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class PanelConfigurationHandlerTest {

    private PanelConfigurationHandler handler = new PanelConfigurationHandler();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testGetDomainName() {
        assertEquals("panels", handler.getDomainName());
    }

    @Test
    public void testGetFileExtension() {
        assertEquals("csv", handler.getFileExtension());
    }

    @Test
    public void testGetLoadOrder() {
        assertEquals(150, handler.getLoadOrder());
    }

    @Test
    public void testProcessConfiguration_EmptyFile_ThrowsException() throws Exception {
        String csv = "";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Panel configuration file test.csv is empty");

        handler.processConfiguration(inputStream, "test.csv");
    }

    @Test
    public void testProcessConfiguration_MissingPanelNameColumn_ThrowsException() throws Exception {
        String csv = "sampleType,description\nWhole Blood,CBC panel\n";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Panel configuration file test.csv must have a 'panelName' column");

        handler.processConfiguration(inputStream, "test.csv");
    }
}
