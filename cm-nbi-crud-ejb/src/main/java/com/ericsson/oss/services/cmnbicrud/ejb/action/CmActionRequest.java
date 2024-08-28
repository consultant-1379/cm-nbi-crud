/*
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 */
package com.ericsson.oss.services.cmnbicrud.ejb.action;

import com.ericsson.oss.services.cmnbicrud.ejb.common.WriteRequest;

import java.util.Map;

public class CmActionRequest implements WriteRequest {
    private static final long serialVersionUID = 1L;

    private final String fdn;
    private final String actionName;

    @SuppressWarnings("squid:S1948")
    private Map<String, Object> attributes = null;

    public CmActionRequest(String fdn, String actionName, Map<String, Object> attributes) {
        this.fdn = fdn;
        this.actionName = actionName;
        this.attributes = attributes;
    }

    public String getFdn() {
        return fdn;
    }

    public String getActionName() {
        return actionName;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }


    @Override
    public String toString() {
        return "CmActionRequest{" +
                " fdn='" + fdn + '\'' +
                " actionName='" + actionName + '\'' +
                ", attributes=" + attributes +
                '}';
    }
}
