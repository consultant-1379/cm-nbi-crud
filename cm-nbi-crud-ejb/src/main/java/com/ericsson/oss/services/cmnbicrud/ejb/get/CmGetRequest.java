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
package com.ericsson.oss.services.cmnbicrud.ejb.get;

import com.ericsson.oss.services.cmnbicrud.ejb.common.ScopeType;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public class CmGetRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String fdn;
    private ScopeType scopeType;
    private Map<String, Set<String>> attributes = null;
    private int scopeLevel;
    private String filter;

    public CmGetRequest(String fdn, ScopeType scopeType, int scopeLevel, Map<String, Set<String>> attributes, String filter) {
        this.fdn = fdn;
        this.attributes = attributes;
        this.scopeType = scopeType;
        this.scopeLevel = scopeLevel;
        this.filter = filter;
    }

    public String getFdn() {
        return fdn;
    }

    public Map<String, Set<String>> getAttributes() {
        return attributes;
    }

    public ScopeType getScopeType() {
        return scopeType;
    }

    public int getScopeLevel() {
        return scopeLevel;
    }

    public String getFilter() {
        return filter;
    }

    public boolean hasNullAttributes() {
        return attributes == null;
    }

    public boolean hasEmptyAttributes() {
        return ((attributes != null) && attributes.isEmpty() );
    }

    public boolean hasFilter() {
        return filter != null;
    }

    public String getDescritionForUnimplementedUseCase() {
        return "scopeType="+scopeType+ ", attributes="+attributes+ ", filter="+filter;
    }

    @Override
    public String toString() {
        return "CmGetRequest{" +
                "fdn='" + fdn + '\'' +
                ", scopeType=" + scopeType +
                ", attributes=" + attributes +
                ", scopeLevel=" + scopeLevel +
                ", filter='" + filter + '\'' +
                '}';
    }
}
