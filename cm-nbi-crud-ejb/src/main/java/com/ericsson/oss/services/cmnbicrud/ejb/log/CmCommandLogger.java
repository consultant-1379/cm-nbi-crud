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

import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.ErrorSeverity;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.presentation.cmnbirest.api.NbiRequest;
import com.ericsson.oss.presentation.cmnbirest.api.NbiResponse;
import com.ericsson.oss.services.cmnbicrud.ejb.common.CmContextService;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.Set;

/**
 * Logs commands using the {@link SystemRecorder}.
 */
public class CmCommandLogger {
    private static final String ERROR_INFO_TAG = "\\{\"errorInfo\":";
    private static final String ERROR_TAG = "\\{\"error\":";
    private static final String ERROR_DETAIL_TAG = "\\{\"errorDetail\":";



    @Inject
    private Logger logger;

    @Inject
    private SystemRecorder systemRecorder;

    public static final String CM_REST_NBI = "CM-RestNbi";
    private static final String CM_CLI_REQUEST_ID = "CM-CLI-Request-ID: ";
    private static final String NOT_AVAILABLE = "N/A";

    public void logCommandRequest(final NbiRequest nbiRequest) {
        systemRecorder.recordCommand("nbiRequest=" + nbiRequest.getRecordingInfo(), CommandPhase.STARTED, CM_REST_NBI, "", CM_CLI_REQUEST_ID + nbiRequest.getRequestId());
    }

    public void logCommandRequestIfDebugEnabled(final NbiRequest nbiRequest) {
        if (logger.isDebugEnabled()) {
                logCommandRequest(nbiRequest);
        }
    }

    public void logCommandResponse(final String requestId, final NbiResponse nbiResponse, final Set successHttpCodes) {
        if (!checkCAL()) {
            if (successHttpCodes.contains(nbiResponse.getHttpCode())) {
                systemRecorder.recordCommand("nbiRequest ...", CommandPhase.FINISHED_WITH_SUCCESS, CM_REST_NBI, "",
                        CM_CLI_REQUEST_ID + requestId + ". httpCode: " + nbiResponse.getHttpCode());
            } else {
                systemRecorder.recordError("nbiRequest ...", ErrorSeverity.ERROR, CM_REST_NBI, "",
                        CM_CLI_REQUEST_ID + requestId + ". httpCode: " + nbiResponse.getHttpCode());
            }
        }
    }

    public void logCompactAuditLog(final NbiResponse nbiResponse,
                                   final Set successHttpCodes,
                                   final CmContextService cmContextService
                                   ) {
        final String userId = cmContextService.getUserId();
        final String requestId = cmContextService.getRequestId();
        final String ipAddress = cmContextService.getUserIpAddress();
        final String ssoToken =  cmContextService.getSsoToken();
        final String commandDescription = cmContextService.getOperationSlogan() + " Resource: "+cmContextService.getUriAndParams()+ "  Body: " +  cmContextService.getBody();

        CommandPhase commandPhase = null;
        String json = null;
        if (successHttpCodes.contains(nbiResponse.getHttpCode())) {
            commandPhase = CommandPhase.FINISHED_WITH_SUCCESS;
            json = cmContextService.getCompactAuditLogSummary();
            if (json == null) {
                json = "{"+ NOT_AVAILABLE +"}";
            }
        } else {
            commandPhase = CommandPhase.FINISHED_WITH_ERROR;
            json = removeErrorInfoTag(nbiResponse.getJsonContent());
        }

        try {
            systemRecorder.recordCompactAudit(userId, commandDescription, commandPhase, CM_REST_NBI, "CM-NBI-Request-ID: " + requestId, ipAddress, ssoToken, json);
        } catch (Exception e) {
            logger.info("logCompactAuditLog:: exception in systemRecorder.recordCompactAudit e.getClass={}, e.getMessage={} for requestId={}, commandDescription={}, json.length={}", e.getClass(), e.getMessage(), requestId, commandDescription, getJsonLen(json));
        }
    }

    //same as in Service Framework
    private boolean checkCAL() {
        String propertyCheck = System.getProperty("com.ericsson.oss.itpf.sdk.recording.COMPACT_AUDIT_LOG", "false");
        return "true".equalsIgnoreCase(propertyCheck);
    }

    private String removeErrorInfoTag(String json) {
        if (json == null) {
            return json;
        }
        String s = json.replaceAll(ERROR_INFO_TAG, "");
        if (!json.equals(s) && s.endsWith("}")) {
            s = s.substring(0 , s.length() - 1);
        }

        return s.replaceAll(ERROR_TAG, ERROR_DETAIL_TAG);
    }

    private long getJsonLen(String json) {
        if (json == null) {
            return 0;
        }
        return json.length();
    }
}
