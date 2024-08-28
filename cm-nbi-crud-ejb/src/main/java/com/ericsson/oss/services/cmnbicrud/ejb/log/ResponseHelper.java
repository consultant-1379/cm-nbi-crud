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
package com.ericsson.oss.services.cmnbicrud.ejb.log;

import com.ericsson.oss.itpf.sdk.security.accesscontrol.SecurityViolationException;
import com.ericsson.oss.presentation.cmnbirest.api.NbiRequest;
import com.ericsson.oss.presentation.cmnbirest.api.NbiResponse;
import com.ericsson.oss.services.cm.cmshared.dto.CmObject;
import com.ericsson.oss.services.cm.cmshared.dto.CmResponse;
import com.ericsson.oss.services.cmnbicrud.ejb.common.CmRestNbiError;
import com.ericsson.oss.services.cmnbicrud.ejb.common.CmRestNbiErrorHandler;
import com.ericsson.oss.services.cmnbicrud.ejb.create.CmCreateResponse;
import com.ericsson.oss.services.cmnbicrud.ejb.delete.CmDeleteResponse;
import com.ericsson.oss.services.cmnbicrud.ejb.exceptions.CmRestNbiValidationException;
import com.ericsson.oss.services.cmnbicrud.ejb.get.CmGetRequest;
import com.ericsson.oss.services.cmnbicrud.ejb.get.CmGetResponse;
import com.ericsson.oss.services.cmnbicrud.ejb.common.ResponseType;
import com.ericsson.oss.services.cmnbicrud.ejb.action.CmActionResponse;
import com.ericsson.oss.services.cmnbicrud.ejb.patch.ErroredWriteResponse;
import com.ericsson.oss.services.cmnbicrud.ejb.put.CmPutResponse;
import com.ericsson.oss.services.cmnbicrud.spi.output.ErrorResponseType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;

public class ResponseHelper {

    @Inject
    CmRestNbiErrorHandler errorHandler;

    @Inject
    private ExceptionHelper exceptionHelper;

    private static final String EXCEPTION_MESSAGE = "Nbi Rest Execution Error ";
    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseHelper.class);


    /***************************************************************
     Get Section

     ******************************************************************/
    public CmGetResponse handleException(final Exception e) {
       final String errorMessage = createErrorMessageFromException(e);
        ErrorResponseType errorResponseType = new ErrorResponseType(errorMessage);
       return new CmGetResponse(ResponseType.FAIL, errorResponseType, null, CmRestNbiError.UNEXPECTED_ERROR.getHttpcode());
    }

    public CmGetResponse handleValidationException(final CmRestNbiValidationException cmValidationException) {
        final String errorMessage = cmValidationException.getMessage();
        ErrorResponseType errorResponseType = new ErrorResponseType(errorMessage);
        return new CmGetResponse(ResponseType.FAIL, errorResponseType, null, cmValidationException.getHttpCode());
    }

    public CmGetResponse createErroredCmGetResponse(CmResponse cmResponse) {
        final String errorMessage = cmResponse.getStatusMessage();
        ErrorResponseType errorResponseType = new ErrorResponseType(errorMessage);
        return new CmGetResponse(ResponseType.FAIL, errorResponseType, null, CmRestNbiError.SERVER_ERROR.getHttpcode());
    }

    public CmGetResponse createUnimplementedUseCaseCmGetResponse(CmGetRequest cmGetRequest) {
        final String errorMessage = errorHandler.createErrorMessage(CmRestNbiError.UNIMPLEMENTED_USE_CASE, cmGetRequest.getDescritionForUnimplementedUseCase());
        ErrorResponseType errorResponseType = new ErrorResponseType(errorMessage);
        return new CmGetResponse(ResponseType.FAIL, errorResponseType, null, CmRestNbiError.UNIMPLEMENTED_USE_CASE.getHttpcode());
    }


    /***************************************************************
            Put Section

    ******************************************************************/

    public CmPutResponse handleExceptionCmPutResponse(final Exception e) {
        final String errorMessage = createErrorMessageFromException(e);
        ErrorResponseType errorResponseType = new ErrorResponseType(errorMessage);
        return new CmPutResponse(ResponseType.FAIL, errorResponseType, (Collection<CmObject>)null, CmRestNbiError.UNEXPECTED_ERROR.getHttpcode());
    }

    public CmPutResponse handleValidationExceptionCmPutResponse(final CmRestNbiValidationException cmValidationException) {
        final String errorMessage = cmValidationException.getMessage();
        ErrorResponseType errorResponseType = new ErrorResponseType(errorMessage);
        return new CmPutResponse(ResponseType.FAIL, errorResponseType, (Collection<CmObject>)null, cmValidationException.getHttpCode());
    }

    public CmPutResponse handleJsonExceptionCmPutResponse(final Exception cmJsonException) {
        final String errorMessage = CmRestNbiError.INVALID_JSON.getMessage() + cmJsonException.getMessage();
        ErrorResponseType errorResponseType = new ErrorResponseType(errorMessage);
        return new CmPutResponse(ResponseType.FAIL, errorResponseType, (Collection<CmObject>)null, CmRestNbiError.INVALID_JSON.getHttpcode());
    }

    public CmPutResponse createNotFoundCmPutResponse() {
        final String errorMessage = errorHandler.createErrorMessage(CmRestNbiError.NOT_FOUND_MOS);
        ErrorResponseType errorResponseType = new ErrorResponseType(errorMessage);
        return new CmPutResponse(ResponseType.FAIL, errorResponseType, (Collection<CmObject>)null, CmRestNbiError.NOT_FOUND_MOS.getHttpcode());
    }

    public CmPutResponse createErroredCmPutResponse(CmResponse cmResponse) {
        final String errorMessage = cmResponse.getStatusMessage();
        ErrorResponseType errorResponseType = new ErrorResponseType(errorMessage);
        return new CmPutResponse(ResponseType.FAIL, errorResponseType, (Collection<CmObject>)null, CmRestNbiError.SERVER_ERROR.getHttpcode());
    }

    /***************************************************************
     Create Section

     ******************************************************************/

    public CmCreateResponse handleExceptionCmCreateResponse(final Exception e) {
        final String errorMessage = createErrorMessageFromException(e);
        ErrorResponseType errorResponseType = new ErrorResponseType(errorMessage);
        return new CmCreateResponse(ResponseType.FAIL, errorResponseType, (Collection<CmObject>)null, CmRestNbiError.UNEXPECTED_ERROR.getHttpcode());
    }

    public CmCreateResponse handleValidationExceptionCmCreateResponse(final CmRestNbiValidationException cmValidationException) {
        final String errorMessage = cmValidationException.getMessage();
        ErrorResponseType errorResponseType = new ErrorResponseType(errorMessage);
        return new CmCreateResponse(ResponseType.FAIL, errorResponseType, (Collection<CmObject>)null, cmValidationException.getHttpCode());
    }

    public CmCreateResponse createNotFoundCmCreateResponse() {
        final String errorMessage = errorHandler.createErrorMessage(CmRestNbiError.NOT_FOUND_MOS);
        ErrorResponseType errorResponseType = new ErrorResponseType(errorMessage);
        return new CmCreateResponse(ResponseType.FAIL, errorResponseType, (Collection<CmObject>)null, CmRestNbiError.NOT_FOUND_MOS.getHttpcode());
    }

    public CmCreateResponse createErroredCmCreateResponse(CmResponse cmResponse) {
        final String errorMessage = cmResponse.getStatusMessage();
        ErrorResponseType errorResponseType = new ErrorResponseType(errorMessage);
        return new CmCreateResponse(ResponseType.FAIL, errorResponseType, (Collection<CmObject>)null, CmRestNbiError.SERVER_ERROR.getHttpcode());
    }

    public CmCreateResponse handleJsonExceptionCmCreateResponse(final Exception cmJsonException) {
        final String errorMessage = CmRestNbiError.INVALID_JSON.getMessage() + cmJsonException.getMessage();
        ErrorResponseType errorResponseType = new ErrorResponseType(errorMessage);
        return new CmCreateResponse(ResponseType.FAIL, errorResponseType, (Collection<CmObject>)null, CmRestNbiError.INVALID_JSON.getHttpcode());
    }

    /***************************************************************
     Delete Section

     ******************************************************************/
    public CmDeleteResponse handleExceptionCmDeleteResponse(final Exception e) {
        final String errorMessage = createErrorMessageFromException(e);
        ErrorResponseType errorResponseType = new ErrorResponseType(errorMessage);
        return new CmDeleteResponse(ResponseType.FAIL, errorResponseType, ((Collection<CmObject>)null), CmRestNbiError.UNEXPECTED_ERROR.getHttpcode());
    }

    public CmDeleteResponse handleValidationExceptionCmDeleteResponse(final CmRestNbiValidationException cmValidationException) {
        final String errorMessage = cmValidationException.getMessage();
        ErrorResponseType errorResponseType = new ErrorResponseType(errorMessage);
        return new CmDeleteResponse(ResponseType.FAIL, errorResponseType, ((Collection<CmObject>)null), cmValidationException.getHttpCode());
    }

    /***************************************************************
     Patch Section

     ******************************************************************/
    public ErroredWriteResponse handleExceptionCmPatchResponse(final Exception e) {
        final String errorMessage = createErrorMessageFromException(e);
        ErrorResponseType errorResponseType = new ErrorResponseType(errorMessage);
        return new ErroredWriteResponse(errorResponseType, CmRestNbiError.UNEXPECTED_ERROR.getHttpcode());
    }

    public ErroredWriteResponse handleValidationExceptionCmPatchResponse(final CmRestNbiValidationException cmValidationException) {
        final String errorMessage = cmValidationException.getMessage();
        ErrorResponseType errorResponseType = new ErrorResponseType(errorMessage);
        return new ErroredWriteResponse(errorResponseType, cmValidationException.getHttpCode());
    }

    public ErroredWriteResponse handleJsonExceptionCmPatchResponse(final Exception cmJsonException) {
        final String errorMessage = CmRestNbiError.INVALID_JSON.getMessage() + cmJsonException.getMessage();
        ErrorResponseType errorResponseType = new ErrorResponseType(errorMessage);
        return new ErroredWriteResponse(errorResponseType, CmRestNbiError.INVALID_JSON.getHttpcode());
    }

    /*
        PATCH (ACTION)
     */
    public CmActionResponse createNotFoundCmActionResponse() {
        final String errorMessage = errorHandler.createErrorMessage(CmRestNbiError.NOT_FOUND_MOS);
        ErrorResponseType errorResponseType = new ErrorResponseType(errorMessage);
        return new CmActionResponse(ResponseType.FAIL, errorResponseType, null, CmRestNbiError.NOT_FOUND_MOS.getHttpcode());
    }

    public CmActionResponse createErroredCmActionResponse(CmResponse cmResponse) {
        final String errorMessage = cmResponse==null ? "null cmresponse" : cmResponse.getStatusMessage();
        ErrorResponseType errorResponseType = new ErrorResponseType(errorMessage);
        return new CmActionResponse(ResponseType.FAIL, errorResponseType,null, CmRestNbiError.SERVER_ERROR.getHttpcode());
    }

    public CmActionResponse handleExceptionCmActionResponse(final Exception e) {
        final String errorMessage = createErrorMessageFromException(e);
        ErrorResponseType errorResponseType = new ErrorResponseType(errorMessage);
        return new CmActionResponse(ResponseType.FAIL, errorResponseType, CmRestNbiError.UNEXPECTED_ERROR.getHttpcode());
    }

    public CmActionResponse handleValidationExceptionCmActionResponse(final CmRestNbiValidationException cmValidationException) {
        final String errorMessage = cmValidationException.getMessage();
        ErrorResponseType errorResponseType = new ErrorResponseType(errorMessage);
        return new CmActionResponse(ResponseType.FAIL, errorResponseType, cmValidationException.getHttpCode());
    }

    public CmActionResponse handleJsonExceptionCmActionResponse(final Exception cmJsonException) {
        final String errorMessage = CmRestNbiError.INVALID_JSON.getMessage() + cmJsonException.getMessage();
        ErrorResponseType errorResponseType = new ErrorResponseType(errorMessage);
        return new CmActionResponse(ResponseType.FAIL, errorResponseType, CmRestNbiError.INVALID_JSON.getHttpcode());
    }

    /*
    *              COMMON
    * */
    public NbiResponse createUnimplementedUseCaseNbiResponse(final NbiRequest nbiRequest) {
        final String errorMessage = errorHandler.createErrorMessage(CmRestNbiError.UNIMPLEMENTED_USE_CASE, nbiRequest.toString());
        ErrorResponseType errorResponseType = new ErrorResponseType(errorMessage);
        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode jsonNode = objectMapper.valueToTree(errorResponseType);
        return new NbiResponse(CmRestNbiError.UNIMPLEMENTED_USE_CASE.getHttpcode(), jsonNode.toString());
    }

    public NbiResponse createSecurityViolationNbiResponse(final NbiRequest nbiRequest) {
        final String errorMessage = errorHandler.createErrorMessage(CmRestNbiError.ACCESS_DENIED, nbiRequest.toString());
        ErrorResponseType errorResponseType = new ErrorResponseType(errorMessage);
        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode jsonNode = objectMapper.valueToTree(errorResponseType);
        return new NbiResponse(CmRestNbiError.ACCESS_DENIED.getHttpcode(), jsonNode.toString());
    }

    public boolean isSecurityViolationException(final Exception e) {
        return (e.getCause() instanceof SecurityViolationException);
    }
    /*
     * P R I V A T E - M E T H O D S
     */
    private String createErrorMessageFromException(final Exception e) {
        printStackTraceIfEnable(e);
        final Throwable unpackedException = exceptionHelper.getRootCauseAndRewrap(e);
        return EXCEPTION_MESSAGE + unpackedException.getMessage();
    }

    private void printStackTraceIfEnable( final Throwable throwable) {
        if (LOGGER.isDebugEnabled()) {
            printStackInfo(throwable);
        }
    }

    private void printStackInfo(Throwable throwable) {
        final StringWriter stringWriter = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        final String stackTraceResult = stringWriter.toString();
        LOGGER.error("CmbiRestNbi::ResponseHelper Exception StackTrace  : {}",stackTraceResult);
    }
}
