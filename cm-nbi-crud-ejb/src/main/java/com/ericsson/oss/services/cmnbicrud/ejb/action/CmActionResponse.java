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

import com.ericsson.oss.services.cmnbicrud.ejb.common.ResponseType;
import com.ericsson.oss.services.cmnbicrud.spi.output.ErrorResponseType;
import com.ericsson.oss.services.cmnbicrud.spi.output.ResourceRepresentationType;

import java.io.Serializable;
import java.util.Map;

public class CmActionResponse implements Serializable {
 private static final long serialVersionUID = 1L;

 private ResponseType responseType = null;
 private int httpCode;
 private ErrorResponseType errorResponseType;
 private ResourceRepresentationType rrTypeForAttributes = null;  // ResourceRepresentationType used here for Serialization

    @SuppressWarnings("squid:S2384")
    public CmActionResponse(ResponseType responseType, ErrorResponseType errorResponseType, ResourceRepresentationType attributes, int httpCode) {
        this.responseType = responseType;
        this.errorResponseType = errorResponseType;
        this.rrTypeForAttributes = attributes;
        this.httpCode = httpCode;
    }

    public CmActionResponse(ResponseType responseType, ErrorResponseType errorResponseType, int httpCode) {
        this.responseType = responseType;
        this.errorResponseType = errorResponseType;
        this.httpCode = httpCode;
    }

    public ResponseType getResponseType() {
        return responseType;
    }

    public ErrorResponseType getErrorResponseType() {
        return errorResponseType;
    }

    @SuppressWarnings("squid:S2384")
    public Map<String, Object> getAttributes() {
        return rrTypeForAttributes == null ? null : rrTypeForAttributes.getAttributes();
    }

    public int getHttpCode() {
        return httpCode;
    }

    public boolean isErrored() {
        return responseType == ResponseType.FAIL;
    }

    @Override
    public String toString() {
        return "CmActionResponse{" +
                " httpCode=" + httpCode +
                ", responseType=" + responseType +
                ", errorResponseType=" + errorResponseType +
                ", attributes=" + getAttributes() +
                '}';
    }
}
