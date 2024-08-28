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
package com.ericsson.oss.services.cmnbicrud.ejb.impl.executor;

import static com.ericsson.oss.services.cmnbicrud.ejb.common.CmRestNbiConstants.GET_SUCCESS_HTTP_CODES;
import static com.ericsson.oss.services.cmnbicrud.ejb.common.CmRestNbiConstants.POST_SUCCESS_HTTP_CODES;
import static com.ericsson.oss.services.cmnbicrud.ejb.common.CmRestNbiConstants.PUT_CREATE_SUCCESS_HTTP_CODES;
import static com.ericsson.oss.services.cmnbicrud.ejb.common.CmRestNbiConstants.PUT_MODIFY_SUCCESS_HTTP_CODES;
import static com.ericsson.oss.services.cmnbicrud.ejb.common.CmRestNbiConstants.DELETE_SUCCESS_HTTP_CODES;
import static com.ericsson.oss.services.cmnbicrud.ejb.common.CmRestNbiConstants.PATCH_SUCCESS_HTTP_CODES;
import static com.ericsson.oss.services.cmnbicrud.ejb.common.CmRestNbiConstants.ACTION_SUCCESS_HTTP_CODES;
import static com.ericsson.oss.services.cmnbicrud.ejb.common.CmRestNbiConstants.UNKNOWN_SUCCESS_HTTP_CODES;

import com.ericsson.oss.itpf.sdk.security.accesscontrol.SecurityViolationException;
import com.ericsson.oss.presentation.cmnbirest.api.*;
import com.ericsson.oss.services.cmnbicrud.ejb.cal.OperationSlogan;
import com.ericsson.oss.services.cmnbicrud.ejb.common.CmContextService;
import com.ericsson.oss.services.cmnbicrud.ejb.log.CmCommandLogger;
import com.ericsson.oss.services.cmnbicrud.ejb.log.ResponseHelper;
import com.ericsson.oss.services.cmnbicrud.ejb.message.CmNbiCrudSendResponse;
import com.ericsson.oss.services.cmnbicrud.ejb.instrumentation.InstrumentationBean;
import com.ericsson.oss.services.cmnbicrud.ejb.cal.CompactAuditLoggerCreator;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.Set;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class CmNbiCommandExecutor {
    @Inject
    private Logger logger;

    @Inject
    private CmNbiCrudGetExecutor cmNbiCrudGetExecutor;

    @Inject
    private CmNbiCrudPutExecutor cmNbiCrudPutExecutor;

    @Inject
    private CmNbiCrudCreateExecutor cmNbiCrudCreateExecutor;

    @Inject
    private CmNbiCrudDeleteExecutor cmNbiCrudDeleteExecutor;

    @Inject
    private CmNbiCrudPatchExecutor cmNbiCrudPatchExecutor;

    @Inject
    private CmNbiCrudActionExecutor cmNbiCrudActionExecutor;

    @Inject
    private CmNbiCrudSendResponse cmNbiCrudSendResponse;

    @Inject
    private CmCommandLogger commandLogger;

    @Inject
    private ResponseHelper responseHelper;

    @Inject
    private CmContextService cmContextService;

    @Inject
    private InstrumentationBean instrumentationBean;

    @Inject
    CompactAuditLoggerCreator compactAuditLoggerCreator;

    @SuppressWarnings({"squid:S3776"})
    public void processCommandRequest(final String requestId, final NbiRequest nbiRequest) {
        logger.debug("CmNbiCommandExecutor::processCommandRequest requestId={}, nbiRequest={}", requestId, nbiRequest);
        final long startTime = instrumentationBean.startInstrumentationMeasure();
        String requestType = InstrumentationBean.UNKNOWN;
        Set<Integer> successHttpCodes = UNKNOWN_SUCCESS_HTTP_CODES;
        NbiResponse nbiResponse = null;
        try {
            cmContextService.setUserContext(nbiRequest);
            cmContextService.setDetailedLogInformation(nbiRequest.getUserIpAddress(), nbiRequest.getSsoToken(), compactAuditLoggerCreator.getSloganFromRequest(nbiRequest), compactAuditLoggerCreator.getUriAndParamsFromRequest(nbiRequest), compactAuditLoggerCreator.getBodyBeforeStartingRequest(nbiRequest));
            if (nbiRequest instanceof NbiCrudGetRequest) {
                final String scopeType = ((NbiCrudGetRequest) nbiRequest).getScopeType();
                requestType = InstrumentationBean.GET_BASE_OTHER_ALL;
                if (scopeType == null || scopeType.equals("BASE_ONLY")) {
                    requestType = InstrumentationBean.GET_BASE_ONLY;
                }
                successHttpCodes = GET_SUCCESS_HTTP_CODES;
                commandLogger.logCommandRequestIfDebugEnabled(nbiRequest);
                logger.debug("CmNbiCommandExecutor received GET Crud operation {}", requestId);
                nbiResponse = cmNbiCrudGetExecutor.executeOperation((NbiCrudGetRequest)nbiRequest);
                cmNbiCrudSendResponse.sendMessage(requestId, nbiResponse);
            } else if (nbiRequest instanceof NbiCrudPutRequest) {

                if (cmNbiCrudPutExecutor.isAnUpdateOperation((NbiCrudPutRequest)nbiRequest)) {
                    requestType = InstrumentationBean.PUT_MODIFY;
                    successHttpCodes = PUT_MODIFY_SUCCESS_HTTP_CODES;
                    commandLogger.logCommandRequestIfDebugEnabled(nbiRequest);
                    logger.debug("CmNbiCommandExecutor received PUT Crud (MODIFY) operation {}", requestId);
                    nbiResponse = cmNbiCrudPutExecutor.executeOperation((NbiCrudPutRequest) nbiRequest);
                    cmNbiCrudSendResponse.sendMessage(requestId, nbiResponse);
                } else { // create operation
                    cmContextService.setOperationSlogan(OperationSlogan.EXECUTE_PUT_CREATE.getSlogan());
                    requestType = InstrumentationBean.PUT_CREATE;
                    successHttpCodes = PUT_CREATE_SUCCESS_HTTP_CODES;
                    commandLogger.logCommandRequestIfDebugEnabled(nbiRequest);
                    logger.debug("CmNbiCommandExecutor received PUT Crud (CREATE) operation {}", requestId);
                    nbiResponse = cmNbiCrudCreateExecutor.executeOperation((NbiCrudPutRequest) nbiRequest);
                    cmNbiCrudSendResponse.sendMessage(requestId, nbiResponse);
                }

            } else if (nbiRequest instanceof NbiCrudPostRequest) {
                requestType = InstrumentationBean.POST;
                successHttpCodes = POST_SUCCESS_HTTP_CODES;
                commandLogger.logCommandRequestIfDebugEnabled(nbiRequest);
                logger.debug("CmNbiCommandExecutor received POST Crud operation {}",requestId);
                nbiResponse = cmNbiCrudCreateExecutor.executeOperation( (NbiCrudPostRequest)nbiRequest);
                cmNbiCrudSendResponse.sendMessage(requestId, nbiResponse);
            } else if (nbiRequest instanceof NbiCrudDeleteRequest) {
                requestType = InstrumentationBean.DELETE;
                successHttpCodes = DELETE_SUCCESS_HTTP_CODES;
                commandLogger.logCommandRequestIfDebugEnabled(nbiRequest);
                logger.debug("CmNbiCommandExecutor received DELETE Crud operation {}", requestId);
                nbiResponse = cmNbiCrudDeleteExecutor.executeOperation( (NbiCrudDeleteRequest) nbiRequest);
                cmNbiCrudSendResponse.sendMessage(requestId, nbiResponse);
            } else if (nbiRequest instanceof NbiCrudPatchRequest) {
                successHttpCodes = PATCH_SUCCESS_HTTP_CODES;
                commandLogger.logCommandRequest(nbiRequest);
                if (((NbiCrudPatchRequest) nbiRequest).getPatchContentType() == PatchContentType.JSON_PATCH) {
                    requestType = InstrumentationBean.PATCH_JSON_PATCH;
                } else {
                    requestType = InstrumentationBean.PATCH_3GPP_JSON_PATCH;
                }
                logger.debug("CmNbiCommandExecutor received PATCH Crud operation {}",requestId);
                nbiResponse = cmNbiCrudPatchExecutor.executeOperation( (NbiCrudPatchRequest)nbiRequest);
                cmNbiCrudSendResponse.sendMessage(requestId, nbiResponse);
            } else if (nbiRequest instanceof NbiCrudActionRequest) {
                successHttpCodes = ACTION_SUCCESS_HTTP_CODES;
                commandLogger.logCommandRequestIfDebugEnabled(nbiRequest);
                logger.debug("CmNbiCommandExecutor received ACTION Crud operation {}", requestId);
                nbiResponse = cmNbiCrudActionExecutor.executeOperation( (NbiCrudActionRequest) nbiRequest);
                cmNbiCrudSendResponse.sendMessage(requestId, nbiResponse);
            } else {
                logger.debug("CmNbiCommandExecutor:: unsupported nbiRequest={}", nbiRequest);
                nbiResponse = responseHelper.createUnimplementedUseCaseNbiResponse(nbiRequest);
                cmNbiCrudSendResponse.sendMessage(requestId, nbiResponse);
            }
        } catch (final SecurityViolationException e) {
            logger.debug("CmNbiCommandExecutor:: security violation exception e.getClass={}, e.getMessage={} ", e.getClass(), e.getMessage());
            nbiResponse = responseHelper.createSecurityViolationNbiResponse(nbiRequest);
            cmNbiCrudSendResponse.sendMessage(requestId, nbiResponse);
        } finally {
            if (nbiResponse != null) {
                commandLogger.logCommandResponse(requestId, nbiResponse, successHttpCodes);
                commandLogger.logCompactAuditLog(nbiResponse, successHttpCodes, cmContextService);
            }
            instrumentationBean.stopInstrumentationMeasure(requestType, startTime);
            cmContextService.clearUserContext();
        }
    }


}
