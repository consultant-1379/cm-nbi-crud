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
package com.ericsson.oss.services.cmnbicrud.ejb.common;



import com.ericsson.oss.services.cmnbicrud.ejb.exceptions.CmRestNbiValidationException;

import javax.inject.Inject;

public class ExceptionFactory {

    @Inject
    private CmRestNbiErrorHandler errorHandler;

    public CmRestNbiValidationException createValidationException(final CmRestNbiError error, final Object... additionalInfo) {
        final String errorMessage = errorHandler.createErrorMessage(error, additionalInfo);
        return new CmRestNbiValidationException(errorMessage, error.getHttpcode());
    }

}

