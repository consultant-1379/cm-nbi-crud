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
package com.ericsson.oss.services.cmnbicrud.ejb.create;

import com.ericsson.oss.services.cmnbicrud.ejb.common.WriteRequest;

import java.util.Map;

public class CmCreateRequest implements WriteRequest {
    private static final long serialVersionUID = 1L;

    private String parentFdn;
    private String childType;
    private String id;

    @SuppressWarnings("squid:S1948")
    private Map<String, Object> attributes = null;

    public CmCreateRequest(final String parentFdn, final String id, final String childType, Map<String, Object> attributes) {
        this.parentFdn = parentFdn;
        this.attributes = attributes;
        this.id = id;
        this.childType = childType;
    }

    public String getParentFdn() {
        return parentFdn;
    }

    public String getChildType() {
        return childType;
    }

    public String getId() {
        return id;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        return "CmCreateRequest{" +
                "parentFdn='" + parentFdn + '\'' +
                "id='" + id + '\'' +
                "childType='" + childType + '\'' +
                ", attributes=" + attributes +
                '}';
    }
}
