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
package com.ericsson.oss.services.cmnbicrud.ejb.put;

import com.ericsson.oss.services.cmnbicrud.ejb.common.WriteRequest;

import java.util.Map;

public class CmPutRequest implements WriteRequest {
    private static final long serialVersionUID = 1L;

    private final String fdn;
    private final String id;
    private final String type;

    @SuppressWarnings("squid:S1948")
    private Map<String, Object> attributes = null;

    public CmPutRequest(String fdn, String id, String type, Map<String, Object> attributes) {
        this.fdn = fdn;
        this.id = id;
        this.type = type;
        this.attributes = attributes;
    }

    public String getFdn() {
        return fdn;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public String getDescriptionForUnimplementedUseCase() {
        return "attributes="+attributes;
    }

    @Override
    public String toString() {
        return "CmPutRequest{" +
                "fdn='" + fdn + '\'' +
                "id='" + id + '\'' +
                "type='" + type + '\'' +
                ", attributes=" + attributes +
                '}';
    }
}
