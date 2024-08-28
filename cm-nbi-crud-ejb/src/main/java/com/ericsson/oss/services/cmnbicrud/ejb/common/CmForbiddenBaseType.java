/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.cmnbicrud.ejb.common;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Created by enmadmin on 12/6/21.
 */
public enum CmForbiddenBaseType {
    SUBNETWORK("subnetwork"),
    ROOT(null);

    private String type;

    private static Set<String> forbiddenType = new HashSet<>();
    static {
        for(final CmForbiddenBaseType entry : CmForbiddenBaseType.values()) {
            forbiddenType.add(entry.type);
        }
    }

    CmForbiddenBaseType(final String  type) {
        this.type  = type;
    }

    public static boolean isForbiddenType(final String type) {
        if (type == null) {
            return true;
        }
        return forbiddenType.contains(type.toLowerCase(Locale.ENGLISH));
    }
}
