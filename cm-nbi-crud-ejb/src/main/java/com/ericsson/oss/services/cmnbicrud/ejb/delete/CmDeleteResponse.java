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

import com.ericsson.oss.services.cm.cmshared.dto.CmObject;
import com.ericsson.oss.services.cmnbicrud.ejb.common.ResponseType;
import com.ericsson.oss.services.cmnbicrud.ejb.common.WriteResponse;
import com.ericsson.oss.services.cmnbicrud.spi.output.ErrorResponseType;
import com.ericsson.oss.services.cmnbicrud.spi.output.MoObjects;

import java.util.Collection;

public class CmDeleteResponse implements WriteResponse {
 private static final long serialVersionUID = 1L;

 private ResponseType responseType;
 private int httpCode;
 private ErrorResponseType errorResponseType;
 private Collection<CmObject> cmObjects;

    @SuppressWarnings("squid:S2384")
    public CmDeleteResponse(ResponseType responseType, ErrorResponseType errorResponseType, Collection<CmObject> cmObjects, int httpCode) {
        this.responseType = responseType;
        this.errorResponseType = errorResponseType;
        this.cmObjects = cmObjects;
        this.httpCode = httpCode;
    }


    public ResponseType getResponseType() {
        return responseType;
    }

    @Override
    public ErrorResponseType getErrorResponseType() {
        return errorResponseType;
    }

    @SuppressWarnings("squid:S2384")
    public Collection<CmObject> getCmObjects() {
        return cmObjects;
    }

    @Override
    public MoObjects getMoObjects() {
        return null;
    }

    @Override
    public int getHttpCode() {
        return httpCode;
    }

    @Override
    public boolean isErrored() {
        return responseType == ResponseType.FAIL;
    }

    @Override
    public String toString() {
        return "CmDeleteResponse{" +
                "responseType=" + responseType +
                ", httpCode=" + httpCode +
                ", errorResponseType=" + errorResponseType +
                ", cmObjects=" + cmObjects +
                '}';
    }
}
