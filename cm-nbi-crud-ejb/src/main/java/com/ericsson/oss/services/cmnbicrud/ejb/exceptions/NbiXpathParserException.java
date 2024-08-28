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
package com.ericsson.oss.services.cmnbicrud.ejb.exceptions;

/**
 * Created by enmadmin on 9/7/21.
 */
public class NbiXpathParserException extends RuntimeException {

    public NbiXpathParserException(String message) {
        super(message);
    }
}
