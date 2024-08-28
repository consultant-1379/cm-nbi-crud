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

import com.ericsson.oss.services.cmnbicrud.ejb.common.WriteRequest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CmPatchRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String commonParent;
    private String longestSon;
    private List<WriteRequest> requests;

    public CmPatchRequest(String commonParent, String longestSon, List<WriteRequest> requests) {
        this.commonParent = commonParent;
        this.longestSon = longestSon;
        this.requests = new ArrayList<>(requests);
    }

    public String getCommonParent() {
        return commonParent;
    }
    public String getLongestSon() {
        return longestSon;
    }

    public List<WriteRequest> getRequests() {
        return new ArrayList<>(requests);
    }


    @Override
    public String toString() {
        return "CmPatchRequest{" +
                "commonParent='" + commonParent + '\'' +
                "longestSon='" + longestSon + '\'' +
                ", requests=" + requests +
                '}';
    }

}
