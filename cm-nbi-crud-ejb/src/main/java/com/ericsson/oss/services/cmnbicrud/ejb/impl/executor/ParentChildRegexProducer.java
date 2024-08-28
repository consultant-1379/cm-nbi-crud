/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.cmnbicrud.ejb.impl.executor;

import java.util.regex.Pattern;

import static com.ericsson.oss.services.cmnbicrud.ejb.common.FdnUtility.getUnscopedType;
import static com.ericsson.oss.services.cmnbicrud.ejb.common.FdnUtility.containsLocallyScopeTypeSeparator;

@SuppressWarnings({"PMD.ClassNamingConventions","PMD.UseUtilityClass"})
public class ParentChildRegexProducer {
    private static final String CASE_INSENSTIVE_CONSTRUCT = "(?i)";
    private static final String PREFIX_TO_MATCH_ANY_NUMBER_RDNS_BEFORE = "([^,]*,|^)*?";
    private static final String OTHER_MOS_INBETWEEN = ",([^=]+=[^=]+,)*?";
    private static final String MATCH_MO_ID = "=[^,]+";
    private static final String RDN_SEPARATOR = ",";
    private static final String SLASH_SEPARATOR = "/";

    private ParentChildRegexProducer() {}

    @SuppressWarnings({"squid:S3776"})
    public static String createRegexForVerifyingFdnContainsAllSpecifiedParentsInCorrectOrder(String filterWithoutAttributes) {
        final StringBuilder parentRegexBuilder = new StringBuilder(CASE_INSENSTIVE_CONSTRUCT);
        boolean isFirstTime = true;
        boolean isDescendant = false;
        int countEmpties = 0;
        String[] mos = filterWithoutAttributes.split(SLASH_SEPARATOR);

        for (String mo:mos) {
            if (mo!=null && !mo.isEmpty()) {

                final boolean containsLocallyScopeTypeSeparator = containsLocallyScopeTypeSeparator(mo);
                final String unscopedType = getUnscopedType(mo);

                if (isFirstTime) {
                    isDescendant = countEmpties > 1;
                    isFirstTime = false;

                    if (isDescendant || containsLocallyScopeTypeSeparator) {
                        parentRegexBuilder.append(PREFIX_TO_MATCH_ANY_NUMBER_RDNS_BEFORE);
                    }

                    final String regexLiteralParentType = Pattern.quote(unscopedType);
                    parentRegexBuilder.append(regexLiteralParentType);
                    parentRegexBuilder.append(MATCH_MO_ID);
                } else {
                    isDescendant = countEmpties > 0;

                    if (isDescendant || containsLocallyScopeTypeSeparator) {
                        parentRegexBuilder.append(OTHER_MOS_INBETWEEN);
                        final String regexLiteralParentType = Pattern.quote(unscopedType);
                        parentRegexBuilder.append(regexLiteralParentType);
                        parentRegexBuilder.append(MATCH_MO_ID);
                    } else {
                        final String regexLiteralParentType = Pattern.quote(RDN_SEPARATOR + unscopedType);
                        parentRegexBuilder.append(regexLiteralParentType);
                        parentRegexBuilder.append(MATCH_MO_ID);
                    }

                }
                countEmpties = 0;

            } else {
                countEmpties++;
            }
        }

        return parentRegexBuilder.toString();
      }

      public static boolean isFdnValidAgainstRegexPattern(final String fdn, final String regexPattern) {
        return fdn.matches(regexPattern);
      }
}