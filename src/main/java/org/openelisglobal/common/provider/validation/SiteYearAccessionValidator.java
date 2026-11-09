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
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.common.provider.validation;

import java.util.Calendar;
import java.util.GregorianCalendar;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.program.valueholder.ServiceCategory;

public class SiteYearAccessionValidator extends BaseSiteYearAccessionValidator implements IAccessionNumberGenerator {

    private static final int MAX_UNUSED_NUMBER_ATTEMPTS = 100;

    @Override
    public int getMaxAccessionLength() {
        return getSiteEndIndex() + 15;
    }

    @Override
    public int getMinAccessionLength() {
        // return getSiteEndIndex() + 7;
        return getSiteEndIndex() + 15;
    }

    @Override
    protected int getIncrementStartIndex() {
        return getSiteEndIndex() + 2;
    }

    @Override
    protected int getSiteEndIndex() {
        return getPrefix().length();
    }

    @Override
    protected int getYearEndIndex() {
        return getSiteEndIndex() + 2;
    }

    @Override
    protected int getYearStartIndex() {
        return getSiteEndIndex();
    }

    @Override
    public int getInvarientLength() {
        return getSiteEndIndex();
    }

    @Override
    public int getChangeableLength() {
        return getMaxAccessionLength() - getInvarientLength();
    }

    @Override
    public String getPrefix() {
        return ConfigurationProperties.getInstance()
                .getPropertyValue(ConfigurationProperties.Property.ACCESSION_NUMBER_PREFIX);
    }

    @Override
    public String incrementAccessionNumber() throws IllegalArgumentException {
        String year = DateUtil.getTwoDigitYear();
        long nextNum = accessionService.getNextNumberIncrement(this.getPrefix() + year, AccessionFormat.SITEYEARNUM);
        String incrementAsString;
        incrementAsString = String.format("%013d", nextNum);
        return getPrefix() + year + incrementAsString;
    }

    @Override
    public String incrementAccessionNumberNoReserve() throws IllegalArgumentException {
        String year = DateUtil.getTwoDigitYear();
        long nextNum = accessionService.getNextNumberNoIncrement(this.getPrefix() + year, AccessionFormat.SITEYEARNUM);
        String incrementAsString;
        incrementAsString = String.format("%013d", nextNum);
        return getPrefix() + year + incrementAsString;
    }

    /**
     * Reserves the next number of a service category's own sequence:
     * {@code <site prefix><category code><yy><number>}, padded to the same total
     * length as general accession numbers (e.g. DEV01PATH26000000001). Each
     * category and year has its own counter row, so it never collides with the
     * general sequence and restarts every year. Like the general sequence, numbers
     * already on a sample (typed in manually, restored data) are skipped.
     */
    public String incrementServiceCategoryAccessionNumber(ServiceCategory category) {
        String prefixAndYear = getPrefix() + category.getCode() + DateUtil.getTwoDigitYear();
        int digits = getMaxAccessionLength() - prefixAndYear.length();
        for (int attempt = 0; attempt < MAX_UNUSED_NUMBER_ATTEMPTS; attempt++) {
            long nextNum = accessionService.getNextNumberIncrement(prefixAndYear, AccessionFormat.SITEYEARNUM);
            String increment = String.format("%0" + digits + "d", nextNum);
            if (increment.length() > digits) {
                throw new IllegalStateException("accession sequence exhausted for " + prefixAndYear);
            }
            String accessionNumber = prefixAndYear + increment;
            if (!accessionNumberIsUsed(accessionNumber, null)) {
                return accessionNumber;
            }
        }
        throw new IllegalStateException("no unused accession number found for " + prefixAndYear + " after "
                + MAX_UNUSED_NUMBER_ATTEMPTS + " attempts");
    }

    /**
     * Accepts service-category numbers
     * ({@code <site prefix><PATH|CYTO><yy><digits>}) alongside the general format,
     * so they work everywhere an accession number is entered or searched.
     */
    @Override
    public ValidationResults validFormat(String accessionNumber, boolean checkDate) {
        ServiceCategory category = serviceCategoryOf(accessionNumber);
        if (category == null || !Boolean
                .valueOf(ConfigurationProperties.getInstance().getPropertyValue(Property.ACCESSION_NUMBER_VALIDATE))) {
            return super.validFormat(accessionNumber, checkDate);
        }
        if (accessionNumber.length() != getMaxAccessionLength()) {
            return ValidationResults.LENGTH_FAIL;
        }
        int yearStart = getSiteEndIndex() + category.getCode().length();
        int yearEnd = yearStart + 2;
        try {
            int year = Integer.parseInt(accessionNumber.substring(yearStart, yearEnd));
            if (checkDate && (new GregorianCalendar().get(Calendar.YEAR) - 2000) != year) {
                return ValidationResults.YEAR_FAIL;
            }
        } catch (NumberFormatException e) {
            return ValidationResults.YEAR_FAIL;
        }
        for (char c : accessionNumber.substring(yearEnd).toCharArray()) {
            if (!Character.isDigit(c)) {
                return ValidationResults.FORMAT_FAIL;
            }
        }
        return ValidationResults.SUCCESS;
    }

    private ServiceCategory serviceCategoryOf(String accessionNumber) {
        String prefix = getPrefix();
        if (accessionNumber == null || prefix == null || !accessionNumber.startsWith(prefix)) {
            return null;
        }
        for (ServiceCategory category : ServiceCategory.values()) {
            if (accessionNumber.startsWith(prefix + category.getCode())) {
                return category;
            }
        }
        return null;
    }
}
