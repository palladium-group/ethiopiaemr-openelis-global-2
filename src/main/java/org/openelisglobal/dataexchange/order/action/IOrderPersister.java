/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.dataexchange.order.action;

import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.patient.valueholder.Patient;

public interface IOrderPersister {

    void persist(MessagePatient patient, ElectronicOrder eOrder);

    /**
     * Resolves (matching by GUID/external id) or creates the OpenELIS patient for
     * the given order patient, without creating an electronic order. Used by the
     * program-order import path so a program case (e.g. Pathology) can be created
     * directly from an imported FHIR order.
     *
     * @param patient the interpreted order patient
     * @return the resolved or newly created OpenELIS patient
     */
    Patient persistPatientData(MessagePatient patient);

    String getServiceUserId();

    void cancelOrder(String referringOrderNumber);
}
