/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2015
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.cmnbicrud.ejb.common;

import javax.transaction.Status;

@SuppressWarnings({"PMD.UseUtilityClass","PMD.ClassNamingConventions"})
public class FdnUtility {

    private static final String RDN_SEPARATOR = ",";
    public static final String LOCALLY_SCOPED_TYPE_SEPARATOR = "$$";

    private FdnUtility() {
        throw new IllegalStateException("Fdn Utility class");
    }

    public static String extractTypeFromFdn(final String fdn) {
        final int lastIndexOfRdnSeparator = fdn.lastIndexOf(RDN_SEPARATOR);
        final int lastIndexOfNameSeparator = fdn.lastIndexOf('=');
        return fdn.substring(lastIndexOfRdnSeparator + 1, lastIndexOfNameSeparator);
    }

    public static String extractNameFromFdn(final String fdn) {
        if (fdn == null) {
            return null;
        }
        final int lastIndexOfNameSeparator = fdn.lastIndexOf('=');
        return fdn.substring(lastIndexOfNameSeparator + 1);
    }

    public static String getParentFdn(final String fdn) {
        final String noParent = null;
        if (fdn == null || "".equals(fdn)) {
            return noParent;
        }
        final int endIndex = fdn.lastIndexOf(',');
        if (endIndex < 0) {
            return noParent;
        }
        return fdn.substring(0, endIndex);
    }

    public static String fdnToBeRemoved(String fdn) {
        StringBuilder fdnToBeRemoved = new StringBuilder();

        if (fdn == null) {
            return fdnToBeRemoved.toString();
        }

        final String[] rdns = fdn.split(RDN_SEPARATOR);
        if (rdns.length <= 1) {
            return fdnToBeRemoved.toString();
        }

        for (int i=0 ; i<=rdns.length-2 ; i++) {
            if (fdnToBeRemoved.length() == 0) {
                fdnToBeRemoved.append(rdns[i]);
            } else {
                fdnToBeRemoved.append(RDN_SEPARATOR + rdns[i]);
            }
        }

        fdnToBeRemoved.append(RDN_SEPARATOR);
        return fdnToBeRemoved.toString();
    }

    public static String purgedFdn(final String fdn,final String fdnToBeRemoved) {
        String purgedFdn = fdn;
        if (fdn.startsWith(fdnToBeRemoved)) {
            purgedFdn = fdn.substring(fdnToBeRemoved.length());
        }
        return purgedFdn;
    }

    public static boolean isRootMo(final String fdn) {
        return fdn == null;
    }

    public static String getTxStatus(final int status) {
        switch (status) {
            case Status.STATUS_ACTIVE:
                return "STATUS_ACTIVE";

            case Status.STATUS_MARKED_ROLLBACK:
                return "STATUS_MARKED_ROLLBACK";

            case Status.STATUS_PREPARED:
                return "STATUS_PREPARED";

            case Status.STATUS_COMMITTED:
                return "STATUS_COMMITTED";

            case Status.STATUS_ROLLEDBACK:
                return "STATUS_ROLLEDBACK";

            case Status.STATUS_UNKNOWN:
                return "STATUS_UNKNOWN";

            case Status.STATUS_NO_TRANSACTION:
                return "STATUS_NO_TRANSACTION";

            case Status.STATUS_PREPARING:
                return "STATUS_PREPARING";

            case Status.STATUS_COMMITTING:
                return "STATUS_COMMITTING";

            case Status.STATUS_ROLLING_BACK:
                return "STATUS_ROLLING_BACK";

            default:
                return "STATUS_UNKNOWN2";
        }
    }

    public static int getFdnDepth(final String fdn) {
        if (fdn == null || "".equals(fdn)) {
            return 0;
        }
        final String[] rdns = fdn.split(RDN_SEPARATOR);
        return rdns.length;
    }

    public static boolean containsLocallyScopeTypeSeparator(final String type) {
        return type.contains(LOCALLY_SCOPED_TYPE_SEPARATOR);
    }

    public static String getUnscopedType(final String type) {
        return type.contains(LOCALLY_SCOPED_TYPE_SEPARATOR) ? type.substring(type.lastIndexOf(LOCALLY_SCOPED_TYPE_SEPARATOR)
                + LOCALLY_SCOPED_TYPE_SEPARATOR.length()) : type;
    }
}
