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
package com.ericsson.oss.services.cmnbicrud.spi.output;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown=true)
public class ErrorResponseType implements Serializable {
    private static final long serialVersionUID = 1L;

    private Error error;

    public ErrorResponseType(final String errorInfo) {
        this.error = new Error(errorInfo);
       }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ErrorResponseType that = (ErrorResponseType) o;

        return error.equals(that.error);
    }

    @Override
    public int hashCode() {
        return error.hashCode();
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "ErrorResponseType{" +
                "error=" + error +
                '}';
    }

    public class Error implements Serializable {
        private static final long serialVersionUID = 2L;

        private String errorInfo;

        private Error(final String errorInfo) {
            this.errorInfo = errorInfo;
        }

        public String getErrorInfo() {
            return errorInfo;
        }

        public void setErrorInfo(String errorInfo) {
            this.errorInfo = errorInfo;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Error err = (Error) o;

            return errorInfo.equals(err.errorInfo);
        }

        @Override
        public int hashCode() {
            return errorInfo.hashCode();
        }

        @Override
        public String toString() {
            return "Error{" +
                    "errorInfo='" + errorInfo + '\'' +
                    '}';
        }
    }

}
