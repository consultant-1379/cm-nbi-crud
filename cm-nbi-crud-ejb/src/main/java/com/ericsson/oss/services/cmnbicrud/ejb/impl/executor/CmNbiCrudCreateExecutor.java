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
package com.ericsson.oss.services.cmnbicrud.ejb.impl.executor;


import static com.ericsson.oss.services.cmnbicrud.ejb.common.CmRestNbiConstants.*;

import com.ericsson.oss.itpf.sdk.security.accesscontrol.annotation.Authorize;
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudPostRequest;
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudPutRequest;
import com.ericsson.oss.presentation.cmnbirest.api.NbiResponse;
import com.ericsson.oss.services.cm.cmshared.dto.CmObject;
import com.ericsson.oss.services.cm.cmshared.dto.CmObjectSpecification;
import com.ericsson.oss.services.cm.cmshared.dto.AttributeSpecification;
import com.ericsson.oss.services.cm.cmshared.dto.AttributeSpecificationContainer;
import com.ericsson.oss.services.cm.cmshared.dto.CmResponse;

import com.ericsson.oss.services.cm.cmshared.dto.search.CmMatchCondition;
import com.ericsson.oss.services.cmnbicrud.ejb.cal.CompactAuditLoggerCreator;
import com.ericsson.oss.services.cmnbicrud.ejb.common.CmContextService;
import com.ericsson.oss.services.cmnbicrud.ejb.common.ExceptionFactory;
import com.ericsson.oss.services.cmnbicrud.ejb.common.ResponseType;
import com.ericsson.oss.services.cmnbicrud.ejb.create.CmCreateRequest;
import com.ericsson.oss.services.cmnbicrud.ejb.create.CmCreateResponse;
import com.ericsson.oss.services.cmnbicrud.ejb.dao.CmReaderDao;
import com.ericsson.oss.services.cmnbicrud.ejb.dao.CmWriterDao;
import com.ericsson.oss.services.cmnbicrud.ejb.exceptions.CmRestNbiValidationException;
import com.ericsson.oss.services.cmnbicrud.ejb.log.ResponseHelper;
import com.ericsson.oss.services.cmnbicrud.spi.output.MoObjects;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Set;

public class CmNbiCrudCreateExecutor {

    private static final Logger logger = LoggerFactory.getLogger(CmNbiCrudCreateExecutor.class);

    @Inject
    ResponseHelper responseHelper;

    @Inject
    ExceptionFactory exceptionFactory;

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

    private static final String MO_IDENTIFIER = "BSMOIDENTIFIER";

    /*
    *                 POST COMMAND REQUEST
    *
    * */
    @Authorize(resource = RBAC_RESOURCE_CM_NBI_CRUD, action = RBAC_ACTION_CREATE)
    public NbiResponse executeOperation(final NbiCrudPostRequest nbiCrudPostRequest) {
        CmCreateResponse cmCreateResponse = executor(nbiCrudPostRequest);
        return createNbiResponse(cmCreateResponse);
    }

    private CmCreateResponse executor(final NbiCrudPostRequest nbiCrudPostRequest) {
        CmCreateResponse cmCreateResponse = null;

        try {
            CmCreateRequest cmCreateRequest = cmNbiCheckInputParameters.generateCmCreateRequest(nbiCrudPostRequest);

            // Preliminary Checks
            cmReaderDao.checkIfDatabaseIsAvailable();
            cmReaderDao.checkIfExistsFdn(cmCreateRequest.getParentFdn());
            cmWriterDao.checkIfValidateFdnAgainstTbac(cmCreateRequest.getParentFdn());

            obscureAndSetBody(cmCreateRequest, nbiCrudPostRequest.getBody());

            cmCreateResponse = executeCmCreateRequest(cmCreateRequest);
        }  catch (final JsonMappingException | JsonParseException e) {
            logger.debug("CmNbiCrudPutExecutor::executor:: nbiCrudCreateRequest={}:: catch Json Exception e.getClass={}, e.getMessage={} ", nbiCrudPostRequest.getBody(), e.getClass(), e.getMessage());
            cmCreateResponse = responseHelper.handleJsonExceptionCmCreateResponse(e);
        } catch (final CmRestNbiValidationException e) {
            logger.debug("CmNbiCrudCreateExecutor::executor:: nbiCrudPostRequest={}:: catch CmRestNbiValidationException e.getClass={}, e.getMessage={} ", nbiCrudPostRequest, e.getClass(), e.getMessage());
            cmCreateResponse = responseHelper.handleValidationExceptionCmCreateResponse(e);
        } catch (Exception e) {
            logger.debug("CmNbiCrudCreateExecutor::executor:: nbiCrudPostRequest={}:: catch Exception e.getClass={}, e.getMessage={} ", nbiCrudPostRequest, e.getClass(), e.getMessage());
            cmCreateResponse = responseHelper.handleExceptionCmCreateResponse(e);
        }

        return cmCreateResponse;
    }

    /*
    *                 PUT (CREATE) COMMAND REQUEST
    *
    * */
    @Authorize(resource = RBAC_RESOURCE_CM_NBI_CRUD, action = RBAC_ACTION_CREATE)
    public NbiResponse executeOperation(final NbiCrudPutRequest nbiCrudPutRequest) {
        CmCreateResponse cmCreateResponse = executor(nbiCrudPutRequest);
        return createNbiResponse(cmCreateResponse);
    }

    private CmCreateResponse executor(final NbiCrudPutRequest nbiCrudPutRequest) {
        CmCreateResponse cmCreateResponse = null;

        try {
            CmCreateRequest cmCreateRequest = cmNbiCheckInputParameters.generateCmCreateRequest(nbiCrudPutRequest);

            // Preliminary Checks
            cmReaderDao.checkIfDatabaseIsAvailable();
            cmReaderDao.checkIfExistsFdn(cmCreateRequest.getParentFdn());
            cmWriterDao.checkIfValidateFdnAgainstTbac(cmCreateRequest.getParentFdn());

            obscureAndSetBody(cmCreateRequest, nbiCrudPutRequest.getBody());

            cmCreateResponse = executeCmCreateRequest(cmCreateRequest);
        }  catch (final JsonMappingException | JsonParseException e) {
            logger.debug("CmNbiCrudPutExecutor::executor:: nbiCrudCreateRequest={}:: catch Json Exception e.getClass={}, e.getMessage={} ", nbiCrudPutRequest.getBody(), e.getClass(), e.getMessage());
            cmCreateResponse = responseHelper.handleJsonExceptionCmCreateResponse(e);
        } catch (final CmRestNbiValidationException e) {
            logger.debug("CmNbiCrudCreateExecutor::executor:: nbiCrudPostRequest={}:: catch CmRestNbiValidationException e.getClass={}, e.getMessage={} ", nbiCrudPutRequest, e.getClass(), e.getMessage());
            cmCreateResponse = responseHelper.handleValidationExceptionCmCreateResponse(e);
        } catch (Exception e) {
            logger.debug("CmNbiCrudCreateExecutor::executor:: nbiCrudPostRequest={}:: catch Exception e.getClass={}, e.getMessage={} ", nbiCrudPutRequest, e.getClass(), e.getMessage());
            cmCreateResponse = responseHelper.handleExceptionCmCreateResponse(e);
        }

        return cmCreateResponse;
    }

    /*
    *                 POST (CREATE) COMMAND REQUEST - FROM PATCH
    *
    * */
    public CmCreateResponse executor(final CmCreateRequest cmCreateRequest) {
        CmCreateResponse cmCreateResponse = null;
        try {
            cmCreateResponse = executeCmCreateRequestForPatch(cmCreateRequest);
        } catch (final CmRestNbiValidationException e) {
            logger.debug("CmNbiCrudCreateExecutor::executor:: cmCreateRequest={}:: catch CmRestNbiValidationException e.getClass={}, e.getMessage={} ", cmCreateRequest, e.getClass(), e.getMessage());
            cmCreateResponse = responseHelper.handleValidationExceptionCmCreateResponse(e);
        } catch (Exception e) {
            logger.debug("CmNbiCrudCreateExecutor::executor:: cmCreateRequest={}:: catch Exception e.getClass={}, e.getMessage={} ", cmCreateRequest, e.getClass(), e.getMessage());
            cmCreateResponse = responseHelper.handleExceptionCmCreateResponse(e);
        }

        return cmCreateResponse;
    }

    /*
    *            Common Code
    * */

    private CmCreateResponse executeCmCreateRequest(CmCreateRequest cmCreateRequest) {
        CmCreateResponse cmCreateResponse;
        CmResponse cmResponse = executeCallCreateObject(cmCreateRequest);
        cmCreateResponse = formatMos(cmResponse);
        return cmCreateResponse;
    }

    private CmCreateResponse executeCmCreateRequestForPatch(CmCreateRequest cmCreateRequest) {
        CmCreateResponse cmCreateResponse;
        CmResponse cmResponse = executeCallCreateObject(cmCreateRequest);
        cmCreateResponse = formatCmObject(cmResponse);
        return cmCreateResponse;
    }

    private CmCreateResponse formatCmObject(final CmResponse cmResponse) {
        if (isSuccessful(cmResponse)) {
            if (hasCmObjects(cmResponse)) {
                return new CmCreateResponse(ResponseType.SUCCESS, null, cmResponse.getCmObjects(), POST_AND_PUT_CREATE_SUCCESS_HTTP_CODE);
            } else {
                return responseHelper.createNotFoundCmCreateResponse();
            }
        } else {
            return responseHelper.createErroredCmCreateResponse(cmResponse);
        }
    }

    private CmResponse executeCallCreateObject(CmCreateRequest cmCreateRequest) {
        CmObjectSpecification cmObjectSpecification = new CmObjectSpecification();
        cmObjectSpecification.setType(cmCreateRequest.getChildType());
        cmObjectSpecification.setName(cmCreateRequest.getId());
        cmObjectSpecification.setAttributeSpecificationContainer(attributeCreator.createAttributeContainer(cmCreateRequest.getAttributes()));
        addIdToContainer(cmCreateRequest.getId(), cmObjectSpecification.getAttributeSpecificationContainer());
        return cmWriterDao.createObjectForNbi(cmCreateRequest.getParentFdn(), cmObjectSpecification);
    }

    private NbiResponse createNbiResponse(CmCreateResponse cmCreateResponse) {
        NbiResponse nbiResponse = null;
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setDateFormat(new SimpleDateFormat(SIMPLE_DATE_FORMAT_STRING));
        if (cmCreateResponse.getResponseType() == ResponseType.SUCCESS) {
            //successful response
            logger.debug("CmNbiCrudGetExecutor:: response is SUCCESS");
            final JsonNode jsonNode = objectMapper.valueToTree(cmCreateResponse.getMoObjects());
            nbiResponse = new NbiResponse(cmCreateResponse.getHttpCode(), jsonNode.toString());

        } else {
            //failed response
            logger.debug("CmNbiCrudGetExecutor:: response is FAIL with httpCode={}", cmCreateResponse.getHttpCode());
            final JsonNode jsonNode = objectMapper.valueToTree(cmCreateResponse.getErrorResponseType());
            nbiResponse = new NbiResponse(cmCreateResponse.getHttpCode(), jsonNode.toString());
        }
        return nbiResponse;
    }

    /*
     * P R I V A T E - M E T H O D S
    */

    /*
    *  BASE_ONLY methods
    */

    private void addIdToContainer(final String id, final AttributeSpecificationContainer attributeSpecificationContainer) {
            AttributeSpecification attributeSpecification = new AttributeSpecification();
            attributeSpecification.setName(MO_IDENTIFIER);
            attributeSpecification.setCmMatchCondition(CmMatchCondition.NO_MATCH_REQUIRED);
            attributeSpecification.setValue(id);
            attributeSpecificationContainer.addExtendedAttributeSpecification(attributeSpecification);
    }

    private CmCreateResponse formatMos(final CmResponse cmResponse) {
        if (isSuccessful(cmResponse)) {
            if (hasCmObjects(cmResponse)) {
                compactAuditLoggerCreator.setCompactAuditLogSummaryForCreate(getCmObject(cmResponse));
                return new CmCreateResponse(ResponseType.SUCCESS, null, extractMoObject(cmResponse), POST_AND_PUT_CREATE_SUCCESS_HTTP_CODE);
            } else {
                return responseHelper.createNotFoundCmCreateResponse();
            }
        } else {
            return responseHelper.createErroredCmCreateResponse(cmResponse);
        }
    }

    private boolean isSuccessful(CmResponse cmResponse) {
        return cmResponse.getErrorCode() == 0;
    }

    private boolean hasCmObjects(CmResponse cmResponse) {
        return !cmResponse.getCmObjects().isEmpty();
    }

    private MoObjects extractMoObject(final CmResponse cmResponse) {
        CmObject cmObject = cmResponse.getCmObjects().iterator().next();
        return Node.convertInMoObjects(cmObject);
    }

    private CmObject getCmObject(final CmResponse cmResponse) {
        return cmResponse.getCmObjects().iterator().next();
    }

    private void obscureAndSetBody(CmCreateRequest cmCreateRequest, final String body) {
        try {
            Set<String> sensitiveAttributesNames = cmReaderDao.getSensitiveAttributeNamesGivenParentFdn(cmCreateRequest.getParentFdn(), cmCreateRequest.getChildType());
            if (sensitiveAttributesNames != null) {
                String obscuredBody = compactAuditLoggerCreator.obscureBodyForCmPutCreateRequest(body, sensitiveAttributesNames);
                cmContextService.setBody(obscuredBody);
            }
        } catch (Exception e) {
            logger.info("obscureAndSetBody::  Exception message={}", e.getMessage());
        }
    }

}
