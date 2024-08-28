package com.ericsson.oss.services.cmnbicrud.ejb.common;

import com.ericsson.oss.itpf.sdk.context.ContextService;
import com.ericsson.oss.presentation.cmnbirest.api.NbiRequest;

import javax.inject.Inject;

/**
 * Interacts with the {@link ContextService} for setting and getting the UserId and RequestId.
 */
public class CmContextService {

    @Inject
    private ContextService contextService;

    public static final String COMMAND_REQUEST_CONTEXT_ID = "CM-CLI-Request-ID";
    public static final String USER_ID_CONTEXT_VALUE_NAME = "X-Tor-UserID";
    public static final String USER_IP_ADDRESS = "User-IP-Address";
    public static final String SSO_TOKEN = "SSO-Token";
    public static final String COMPACT_AUDIT_LOG_SUMMARY = "COMPACT_AUDIT_LOG_SUMMARY";

    //These entries define command content
    public static final String OPERATION_SLOGAN = "Operation-Slogan";
    public static final String URI_AND_PARAMS = "URI-AND-PARAMS";
    public static final String BODY = "Body";


    public void setUserContext(final NbiRequest nbiRequest) {
        contextService.setContextValue(USER_ID_CONTEXT_VALUE_NAME, nbiRequest.getUserId());
        contextService.setContextValue(COMMAND_REQUEST_CONTEXT_ID, nbiRequest.getRequestId());
    }

    public void setDetailedLogInformation(final String userIpAddress,final String ssoToken, final String operationSlogan, final String uriAndParams, String body) {
        contextService.setContextValue(USER_IP_ADDRESS, userIpAddress);
        contextService.setContextValue(SSO_TOKEN, ssoToken);
        contextService.setContextValue(OPERATION_SLOGAN, operationSlogan);
        contextService.setContextValue(URI_AND_PARAMS, uriAndParams);
        contextService.setContextValue(BODY, body);
    }

    public void clearUserContext() {
        contextService.setContextValue(USER_ID_CONTEXT_VALUE_NAME, null);
        contextService.setContextValue(COMMAND_REQUEST_CONTEXT_ID, null);
        contextService.setContextValue(USER_IP_ADDRESS, null);
        contextService.setContextValue(SSO_TOKEN, null);
        contextService.setContextValue(OPERATION_SLOGAN, null);
        contextService.setContextValue(URI_AND_PARAMS, null);
        contextService.setContextValue(BODY, null);
    }

    public String getUserId() {
        return contextService.getContextValue(USER_ID_CONTEXT_VALUE_NAME);
    }
    public String getUserIpAddress() {return contextService.getContextValue(USER_IP_ADDRESS);}
    public String getSsoToken() {
        return contextService.getContextValue(SSO_TOKEN);
    }
    public String getRequestId() {
        return contextService.getContextValue(COMMAND_REQUEST_CONTEXT_ID);
    }
    public String getOperationSlogan() {
        return contextService.getContextValue(OPERATION_SLOGAN);
    }
    public void setOperationSlogan(final String operationSlogan) {
        contextService.setContextValue(OPERATION_SLOGAN, operationSlogan);
    }

    //uri contain xpath and all path params
    public String getUriAndParams() {
        return contextService.getContextValue(URI_AND_PARAMS);
    }

    public void setBody(final String body) {
        contextService.setContextValue(BODY, body);
    }
    public String getBody() {
        return contextService.getContextValue(BODY);
    }

    public void setCompactAuditLogSummary(final String summary) {contextService.setContextValue(COMPACT_AUDIT_LOG_SUMMARY, summary);}
    public String getCompactAuditLogSummary() {
        return contextService.getContextValue(COMPACT_AUDIT_LOG_SUMMARY);
    }
}
