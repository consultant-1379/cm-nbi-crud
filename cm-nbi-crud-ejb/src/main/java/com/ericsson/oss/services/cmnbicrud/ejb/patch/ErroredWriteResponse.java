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

import com.ericsson.oss.services.cm.cmshared.dto.CmObject;
import com.ericsson.oss.services.cmnbicrud.ejb.common.ResponseType;
import com.ericsson.oss.services.cmnbicrud.ejb.common.WriteResponse;
import com.ericsson.oss.services.cmnbicrud.spi.output.ErrorResponseType;
import com.ericsson.oss.services.cmnbicrud.spi.output.MoObjects;

import java.util.Collection;

public class ErroredWriteResponse implements WriteResponse {
 private static final long serialVersionUID = 1L;

 private int httpCode;
 private ErrorResponseType errorResponseType;


    public ErroredWriteResponse(ErrorResponseType errorResponseType, int httpCode) {
        this.errorResponseType = errorResponseType;
        this.httpCode = httpCode;
    }

    public ResponseType getResponseType() {
        return ResponseType.FAIL;
    }

    @Override
    public ErrorResponseType getErrorResponseType() {
        return errorResponseType;
    }

    @Override
    public MoObjects getMoObjects() {
        return null;
    }

    @Override
    @SuppressWarnings({"squid:S1168"})
    public Collection<CmObject> getCmObjects() {
        return null;
    }

    @Override
    public int getHttpCode() {
        return httpCode;
    }

    @Override
    public boolean isErrored() {
        return true;
    }

    @Override
    public String toString() {
        return "ErroredWriteResponse{" +
                "httpCode=" + httpCode +
                ", errorResponseType=" + errorResponseType +
                ", moObjects=" + null +
                '}';
    }
}
