/*
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 */
package com.ericsson.oss.services.cmnbicrud.ejb.patch;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class MapWithOnlyOneComplexMember implements Serializable {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("squid:S1948")
    private Map<String, Object> map = new HashMap<>();

    public MapWithOnlyOneComplexMember(final String member, final Object value) {
        map.put(member, value);
    }

    public Map<String, Object> getMap() {
        return map;
    }
}
