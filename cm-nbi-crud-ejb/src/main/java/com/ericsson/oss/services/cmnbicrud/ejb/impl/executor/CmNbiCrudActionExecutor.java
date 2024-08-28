/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.cmnbicrud.ejb.impl.executor;

import com.ericsson.oss.itpf.sdk.security.accesscontrol.annotation.Authorize;
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudActionRequest;
import com.ericsson.oss.presentation.cmnbirest.api.NbiResponse;
import com.ericsson.oss.services.cm.cmshared.dto.ActionSpecification;
import com.ericsson.oss.services.cm.cmshared.dto.CmObject;
import com.ericsson.oss.services.cm.cmshared.dto.CmResponse;
import com.ericsson.oss.services.cmnbicrud.ejb.cal.CompactAuditLoggerCreator;
import com.ericsson.oss.services.cmnbicrud.ejb.common.CmContextService;
import com.ericsson.oss.services.cmnbicrud.ejb.common.ResponseType;

import com.ericsson.oss.services.cmnbicrud.ejb.dao.CmReaderDao;
import com.ericsson.oss.services.cmnbicrud.ejb.dao.CmWriterDao;
import com.ericsson.oss.services.cmnbicrud.ejb.exceptions.CmRestNbiValidationException;
import com.ericsson.oss.services.cmnbicrud.ejb.log.ResponseHelper;
import com.ericsson.oss.services.cmnbicrud.ejb.action.CmActionRequest;
import com.ericsson.oss.services.cmnbicrud.ejb.action.CmActionResponse;

import com.ericsson.oss.services.cmnbicrud.spi.output.ResourceRepresentationType;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;

import static com.ericsson.oss.services.cmnbicrud.ejb.common.CmRestNbiConstants.*;

public class CmNbiCrudActionExecutor {

    private static final Logger logger = LoggerFactory.getLogger(CmNbiCrudActionExecutor.class);

    @Inject
    ResponseHelper responseHelper;

    @Inject
    CmNbiCheckInputParameters cmNbiCheckInputParameters;

    @Inject
    AttributeCreator attributeCreator;

    @Inject
    CmWriterDao cmWriterDao;

    @Inject
    CmReaderDao cmReaderDao;

    @Inject
    CompactAuditLoggerCreator compactAuditLoggerCreator;

    @Inject
    private CmContextService cmContextService;

    @Authorize(resource = RBAC_RESOURCE_CM_NBI_CRUD, action = RBAC_ACTION_UPDATE)
    public NbiResponse executeOperation(final NbiCrudActionRequest nbiCrudActionRequest) {
        CmActionResponse response = executor(nbiCrudActionRequest);
        return createNbiResponse(response);
    }

    @SuppressWarnings({"squid:S2259"})
    private NbiResponse createNbiResponse(CmActionResponse response) {
        NbiResponse nbiResponse;
        final ObjectMapper objectMapper = new ObjectMapper();

        if (!response.isErrored()) {
            int httpCode = ACTION_SUCCESS_HTTP_CODE_NO_CONTENT;

            //successful response
            logger.debug("CmNbiCrudPatchExecutor:: response is SUCCESS");
            String nbiActionResponse = null;
            Map<String, Object> attributes = response.getAttributes();
            if (!attributes.isEmpty()) {
                Object actionResponseObject = attributes.get("RETURN VALUE");
                if (actionResponseObject != null){
                        final JsonNode jsonNode = objectMapper.valueToTree(actionResponseObject);
                        nbiActionResponse = jsonNode.toString();
                    nbiActionResponse = "{ \"output\": " + nbiActionResponse + "}";
                    httpCode = ACTION_SUCCESS_HTTP_CODE;
                }
            }
            nbiResponse = new NbiResponse(httpCode, nbiActionResponse);
        } else {
            //failed response
            logger.debug("CmNbiCrudPatchActionExecutor:: response is FAIL with httpCode={}", response.getHttpCode());
            final JsonNode jsonNode = objectMapper.valueToTree(response.getErrorResponseType());
            nbiResponse = new NbiResponse(response.getHttpCode(), jsonNode.toString());
        }
        return nbiResponse;
    }

    private CmActionResponse executor(final NbiCrudActionRequest nbiCrudActionRequest)  {

        CmActionResponse cmActionResponse;

        try {
            CmActionRequest cmActionRequest = cmNbiCheckInputParameters.generateCmActionRequest(nbiCrudActionRequest);
            logger.debug("CmNbiCrudActionExecutor::cmActionRequest={}", cmActionRequest);
            String fdn = cmActionRequest.getFdn();

            // Preliminary Checks
            cmReaderDao.checkIfDatabaseIsAvailable();
            cmReaderDao.checkIfExistsFdn(fdn);
            cmWriterDao.checkIfValidateFdnAgainstTbac(fdn);

            obscureAndSetBody(cmActionRequest, nbiCrudActionRequest.getBody());

            ActionSpecification actionSpecification = new ActionSpecification(cmActionRequest.getActionName(), attributeCreator.createAttributeContainer(cmActionRequest.getAttributes()));
            CmResponse cmResponse = cmWriterDao.performActionOnFdn(fdn, actionSpecification);
            cmActionResponse = getCmActionResponse(cmResponse);

        }  catch (final JsonMappingException | JsonParseException e) {
            logger.debug("CmNbiCrudActionExecutor::executor:: nbiCrudActionRequest={}:: catch Json Exception e.getClass={}, e.getMessage={} ", nbiCrudActionRequest.getBody(), e.getClass(), e.getMessage());
            cmActionResponse = responseHelper.handleJsonExceptionCmActionResponse(e);
        } catch (final CmRestNbiValidationException e) {
            logger.debug("CmNbiCrudActionExecutor::executor:: nbiCrudActionRequest={}:: catch CmRestNbiValidationException e.getClass={}, e.getMessage={} ", nbiCrudActionRequest, e.getClass(), e.getMessage());
            cmActionResponse = responseHelper.handleValidationExceptionCmActionResponse(e);
        } catch (Exception e) {
            logger.debug("CmNbiCrudActionExecutor::executor:: nbiCrudActionRequest={}:: catch Exception e.getClass={}, e.getMessage={} ", nbiCrudActionRequest, e.getClass(), e.getMessage());
            cmActionResponse = responseHelper.handleExceptionCmActionResponse(e);
        }

        return cmActionResponse;
    }

    /*
     * P R I V A T E - M E T H O D S
    */
    public CmActionResponse getCmActionResponse(final CmResponse cmResponse) {
        if (cmResponse != null && isSuccessful(cmResponse)) {
                if (hasCmObjects(cmResponse)) {
                    compactAuditLoggerCreator.setCompactAuditLogSummaryForAction(cmResponse.getCmObjects().iterator().next());
                    return new CmActionResponse(ResponseType.SUCCESS, null, extractAttributes(cmResponse), ACTION_SUCCESS_HTTP_CODE);
                } else {
                    return responseHelper.createNotFoundCmActionResponse();
                }
        }
        return responseHelper.createErroredCmActionResponse(cmResponse);
    }

    private boolean isSuccessful(CmResponse cmResponse) {
        return cmResponse.getErrorCode() == 0;
    }

    private boolean hasCmObjects(CmResponse cmResponse) {
        return !cmResponse.getCmObjects().isEmpty();
    }

    private ResourceRepresentationType extractAttributes(final CmResponse cmResponse) {
        CmObject cmObject = cmResponse.getCmObjects().iterator().next();
        ResourceRepresentationType resource = new ResourceRepresentationType();
        resource.setAttributes(cmObject.getAttributes());
        return resource;
    }

    private void obscureAndSetBody(CmActionRequest cmActionRequest, final String body) {
        try {
            Set<String> sensitiveActionParameterNames = cmReaderDao.getSensitiveActionParameterNamesGivenFdn(cmActionRequest.getFdn(), cmActionRequest.getActionName());
            if (sensitiveActionParameterNames != null) {
                String obscuredBody = compactAuditLoggerCreator.obscureBodyForCmActionRequest(body, sensitiveActionParameterNames);
                cmContextService.setBody(obscuredBody);
            }
        } catch (Exception e) {
            logger.info("obscureAndSetBody::  Exception message={}", e.getMessage());
        }
    }
}
