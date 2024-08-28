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

import com.ericsson.oss.services.cmnbicrud.ejb.common.ResponseType;
import com.ericsson.oss.services.cmnbicrud.spi.output.ErrorResponseType;
import com.ericsson.oss.services.cmnbicrud.spi.output.MoObjects;

import java.io.Serializable;

public class CmGetResponse implements Serializable {
 private static final long serialVersionUID = 1L;

 private ResponseType responseType;
 private int httpCode;
 private ErrorResponseType errorResponseType;
 private MoObjects moObjects;


    public CmGetResponse(ResponseType responseType, ErrorResponseType errorResponseType, MoObjects moObjects, int httpCode) {
        this.responseType = responseType;
        this.errorResponseType = errorResponseType;
        this.moObjects = moObjects;
        this.httpCode = httpCode;
    }

    public ResponseType getResponseType() {
        return responseType;
    }

    public ErrorResponseType getErrorResponseType() {
        return errorResponseType;
    }

    public MoObjects getMoObjects() {
        return moObjects;
    }

    public int getHttpCode() {
        return httpCode;
    }

    @Override
    public String toString() {
        return "CmGetResponse{" +
                "httpCode=" + httpCode +
                "responseType=" + responseType +
                ", errorResponseType=" + errorResponseType +
                ", moObjects=" + moObjects +
                '}';
    }
}
