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

import com.ericsson.oss.services.cm.cmshared.dto.CmObject;
import com.ericsson.oss.services.cmnbicrud.ejb.common.ResponseType;
import com.ericsson.oss.services.cmnbicrud.ejb.common.WriteResponse;
import com.ericsson.oss.services.cmnbicrud.spi.output.ErrorResponseType;
import com.ericsson.oss.services.cmnbicrud.spi.output.MoObjects;

import java.util.Collection;

public class CmCreateResponse implements WriteResponse {
 private static final long serialVersionUID = 1L;

 private ResponseType responseType;
 private int httpCode;
 private ErrorResponseType errorResponseType;
 private MoObjects moObjects;
 private Collection<CmObject> cmObjects;


    public CmCreateResponse(ResponseType responseType, ErrorResponseType errorResponseType, MoObjects moObjects, int httpCode) {
        this.responseType = responseType;
        this.errorResponseType = errorResponseType;
        this.moObjects = moObjects;
        this.httpCode = httpCode;
    }

    @SuppressWarnings("squid:S2384")
    public CmCreateResponse(ResponseType responseType, ErrorResponseType errorResponseType, Collection<CmObject> cmObjects, int httpCode) {
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
        return moObjects;
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
        return "CmCreateResponse{" +
                " httpCode=" + httpCode +
                ", responseType=" + responseType +
                ", errorResponseType=" + errorResponseType +
                ", moObjects=" + moObjects +
                ", cmObjects=" + cmObjects +
                '}';
    }
}
