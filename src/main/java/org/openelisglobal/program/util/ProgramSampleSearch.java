package org.openelisglobal.program.util;

import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.sample.valueholder.Sample;

/**
 * Shared reception/dashboard search matching: null-safe, case-insensitive contains
 * on accession and patient first/last/full name.
 */
public final class ProgramSampleSearch {

    private ProgramSampleSearch() {
    }

    public static boolean matchesPatientOrAccession(Sample sample, Patient patient, String searchTerm) {
        if (StringUtils.isBlank(searchTerm)) {
            return true;
        }
        String term = searchTerm.trim().toLowerCase();
        if (sample != null && StringUtils.containsIgnoreCase(sample.getAccessionNumber(), term)) {
            return true;
        }
        return matchesPatientName(patient, term);
    }

    private static boolean matchesPatientName(Patient patient, String termLower) {
        if (patient == null || patient.getPerson() == null) {
            return false;
        }
        Person person = patient.getPerson();
        String first = StringUtils.defaultString(person.getFirstName()).toLowerCase();
        String last = StringUtils.defaultString(person.getLastName()).toLowerCase();
        String full = (first + " " + last).trim();
        return first.contains(termLower) || last.contains(termLower) || full.contains(termLower);
    }
}
