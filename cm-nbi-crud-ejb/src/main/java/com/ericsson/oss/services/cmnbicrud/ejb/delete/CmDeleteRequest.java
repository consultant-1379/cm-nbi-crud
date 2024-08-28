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
package com.ericsson.oss.services.cmnbicrud.ejb.delete;

import com.ericsson.oss.services.cmnbicrud.ejb.common.WriteRequest;
import com.ericsson.oss.services.cmnbicrud.ejb.common.ScopeType;

public class CmDeleteRequest implements WriteRequest {
    private static final long serialVersionUID = 1L;

    private String fdn;
    private ScopeType scopeType;
    private int scopeLevel;
    private String filter;
    private boolean noQueryParameters;

    public CmDeleteRequest(String fdn,
                           ScopeType scopeType,
                           int scopeLevel,
                           String filter,
                           boolean noQueryParameters) {
        this.fdn = fdn;
        this.scopeType = scopeType;
        this.scopeLevel = scopeLevel;
        this.filter = filter;
        this.noQueryParameters = noQueryParameters;
    }

    public String getFdn() {
        return fdn;
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

    public boolean getNoQueryParameters() {
        return noQueryParameters;
    }

    @Override
    public String toString() {
        return "CmDeleteRequest{" +
                "fdn='" + fdn + '\'' +
                ", scopeType=" + scopeType +
                ", scopeLevel=" + scopeLevel +
                ", filter='" + filter + '\'' +
                ", noQueryParameters='" + noQueryParameters + '\'' +
                '}';
    }
}
