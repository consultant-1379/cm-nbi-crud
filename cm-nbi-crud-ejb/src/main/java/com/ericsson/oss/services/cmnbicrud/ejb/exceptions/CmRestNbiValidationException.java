/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.cmnbicrud.ejb.exceptions;

import javax.ejb.ApplicationException;

@ApplicationException(rollback = false)
public class CmRestNbiValidationException extends RuntimeException {

    private static final long serialVersionUID = 5616557036361539638L;

    private final int httpCode;

    public CmRestNbiValidationException(final String message,final int httpCode) {
        super(message);
        this.httpCode = httpCode;
    }

    /**
     * @return the httpCode
     */
    public int getHttpCode() { return httpCode; }

}
