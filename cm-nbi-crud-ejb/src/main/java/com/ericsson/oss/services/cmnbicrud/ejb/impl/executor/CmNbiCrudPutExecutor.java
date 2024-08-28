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
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudPutRequest;
import com.ericsson.oss.presentation.cmnbirest.api.NbiResponse;
import com.ericsson.oss.services.cm.cmshared.dto.*;
import com.ericsson.oss.services.cmnbicrud.ejb.cal.CompactAuditLoggerCreator;
import com.ericsson.oss.services.cmnbicrud.ejb.common.CmContextService;
import com.ericsson.oss.services.cmnbicrud.ejb.common.ExceptionFactory;
import com.ericsson.oss.services.cmnbicrud.ejb.common.ResponseType;
import com.ericsson.oss.services.cmnbicrud.ejb.dao.CmReaderDao;
import com.ericsson.oss.services.cmnbicrud.ejb.dao.CmWriterDao;
import com.ericsson.oss.services.cmnbicrud.ejb.exceptions.CmRestNbiValidationException;
import com.ericsson.oss.services.cmnbicrud.ejb.log.ResponseHelper;
import com.ericsson.oss.services.cmnbicrud.ejb.put.CmPutRequest;
import com.ericsson.oss.services.cmnbicrud.ejb.put.CmPutResponse;
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

public class CmNbiCrudPutExecutor {

    private static final Logger logger = LoggerFactory.getLogger(CmNbiCrudPutExecutor.class);

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

    @Authorize(resource = RBAC_RESOURCE_CM_NBI_CRUD, action = RBAC_ACTION_UPDATE)
    public NbiResponse executeOperation(final NbiCrudPutRequest nbiCrudPutRequest) {
        CmPutResponse cmPutResponse = executor(nbiCrudPutRequest);
        return createNbiResponse(cmPutResponse);
    }

    public boolean isAnUpdateOperation( final NbiCrudPutRequest nbiCrudPutRequest) {
        try {
            final String fdn = cmNbiCheckInputParameters.getFdnFromXpath(nbiCrudPutRequest.getXpath());
            return cmReaderDao.checkIfExistsFdnWithoutException(fdn);
        } catch (final CmRestNbiValidationException e ) {
            logger.debug("isAnUpdateOperation exception = {}",e.getMessage());
            return false;
        }
    }

    private NbiResponse createNbiResponse(CmPutResponse cmPutResponse) {
        NbiResponse nbiResponse = null;
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setDateFormat(new SimpleDateFormat(SIMPLE_DATE_FORMAT_STRING));
        if (cmPutResponse.getResponseType() == ResponseType.SUCCESS) {
            //successful response
            logger.debug("CmNbiCrudPutExecutor:: response is SUCCESS");
            final JsonNode jsonNode = objectMapper.valueToTree(cmPutResponse.getMoObjects());
            nbiResponse = new NbiResponse(cmPutResponse.getHttpCode(), jsonNode.toString());
        } else {
            //failed response
            logger.debug("CmNbiCrudPutExecutor:: response is FAIL with httpCode={}", cmPutResponse.getHttpCode());
            final JsonNode jsonNode = objectMapper.valueToTree(cmPutResponse.getErrorResponseType());
            nbiResponse = new NbiResponse(cmPutResponse.getHttpCode(), jsonNode.toString());
        }
        return nbiResponse;
    }

    /*
    *                 PUT COMMAND REQUEST
    *
    * */
    private CmPutResponse executor(final NbiCrudPutRequest nbiCrudPutRequest) {
        CmPutResponse cmPutResponse = null;

        try {
            CmPutRequest cmPutRequest = cmNbiCheckInputParameters.generateCmPutRequest(nbiCrudPutRequest);

            // Preliminary Checks
            cmReaderDao.checkIfDatabaseIsAvailable();
            cmReaderDao.checkIfExistsFdn(cmPutRequest.getFdn());
            cmWriterDao.checkIfValidateFdnAgainstTbac(cmPutRequest.getFdn());

            obscureAndSetBody(cmPutRequest, nbiCrudPutRequest);

            CmObjectSpecification cmObjectSpecification = new CmObjectSpecification();
            cmObjectSpecification.setType(cmPutRequest.getType());
            cmObjectSpecification.setName(cmPutRequest.getId());
            cmObjectSpecification.setAttributeSpecificationContainer(attributeCreator.createAttributeContainer(cmPutRequest.getAttributes()));

            CmResponse cmResponse = cmWriterDao.setManagedObjectForNbiPUT(cmPutRequest.getFdn(), cmObjectSpecification);

            cmPutResponse = getCmPutResponse(cmResponse);
        }  catch (final JsonMappingException | JsonParseException e) {
            logger.debug("CmNbiCrudPutExecutor::executor:: nbiCrudPutRequest={}:: catch Json Exception e.getClass={}, e.getMessage={} ", nbiCrudPutRequest.getBody(), e.getClass(), e.getMessage());
            cmPutResponse = responseHelper.handleJsonExceptionCmPutResponse(e);
        } catch (final CmRestNbiValidationException e) {
            logger.debug("CmNbiCrudPutExecutor::executor:: nbiCrudPutRequest={}:: catch CmRestNbiValidationException e.getClass={}, e.getMessage={} ", nbiCrudPutRequest, e.getClass(), e.getMessage());
            cmPutResponse = responseHelper.handleValidationExceptionCmPutResponse(e);
        } catch (Exception e) {
            logger.debug("CmNbiCrudPutExecutor::executor:: nbiCrudPutRequest={}:: catch Exception e.getClass={}, e.getMessage={} ", nbiCrudPutRequest, e.getClass(), e.getMessage());
            cmPutResponse = responseHelper.handleExceptionCmPutResponse(e);
        }

        return cmPutResponse;
    }

    /*
    *                 PUT COMMAND REQUEST - FROM PATCH
    *
    * */
    public CmPutResponse executor(final CmPutRequest cmPutRequest) {
        CmPutResponse cmPutResponse = null;

        try {
            CmObjectSpecification cmObjectSpecification = new CmObjectSpecification();
            cmObjectSpecification.setType(cmPutRequest.getType());
            cmObjectSpecification.setName(cmPutRequest.getId());
            cmObjectSpecification.setAttributeSpecificationContainer(attributeCreator.createAttributeContainer(cmPutRequest.getAttributes()));

            CmResponse cmResponse = cmWriterDao.setManagedObjectForNbiPATCH(cmPutRequest.getFdn(), cmObjectSpecification);
            cmPutResponse = formatCmObject(cmResponse);
        } catch (final CmRestNbiValidationException e) {
            logger.debug("CmNbiCrudPutExecutor::executor:: CmPutRequest={}:: catch CmRestNbiValidationException e.getClass={}, e.getMessage={} ", cmPutRequest, e.getClass(), e.getMessage());
            cmPutResponse = responseHelper.handleValidationExceptionCmPutResponse(e);
        } catch (Exception e) {
            logger.debug("CmNbiCrudPutExecutor::executor:: CmPutRequest={}:: catch Exception e.getClass={}, e.getMessage={} ", cmPutRequest, e.getClass(), e.getMessage());
            cmPutResponse = responseHelper.handleExceptionCmPutResponse(e);
        }

        return cmPutResponse;
    }

    /*
     * P R I V A T E - M E T H O D S
    */

    private CmPutResponse getCmPutResponse(final CmResponse cmResponse) {
        CmPutResponse cmPutResponse;
        if( attributesHasNotBeenModified(cmResponse) ) {
            // no matter if we return jsonContent = '{}'. Inside rest when http code = 204 we don't fill entity (see CmNbiCrudResource)
            cmPutResponse = new CmPutResponse(ResponseType.SUCCESS, null, Node.convertInEmptyMoObjects(), PUT_MODIFY_SUCCESS_HTTP_CODE_NO_CONTENT);
        } else {
            cmPutResponse = formatMos(cmResponse);
        }
        return cmPutResponse;
    }

    private CmPutResponse formatCmObject(final CmResponse cmResponse) {
        if (isSuccessful(cmResponse)) {
            if (hasCmObjects(cmResponse)) {
                return new CmPutResponse(ResponseType.SUCCESS, null, cmResponse.getCmObjects(), PUT_MODIFY_SUCCESS_HTTP_CODE);
            } else {
                return responseHelper.createNotFoundCmPutResponse();
            }
        } else {
            return responseHelper.createErroredCmPutResponse(cmResponse);
        }
    }

    private boolean attributesHasNotBeenModified(final CmResponse cmResponse) {
        return hasCmObjects(cmResponse) && cmResponse.getCmObjects().iterator().next().getAttributes().isEmpty();
    }

    private CmPutResponse formatMos(final CmResponse cmResponse) {
        if (isSuccessful(cmResponse)) {
            if (hasCmObjects(cmResponse)) {
                compactAuditLoggerCreator.setCompactAuditLogSummaryForPutModify(getCmObject(cmResponse));
                return new CmPutResponse(ResponseType.SUCCESS, null, extractMoObject(cmResponse), PUT_MODIFY_SUCCESS_HTTP_CODE);
            } else {
                return responseHelper.createNotFoundCmPutResponse();
            }
        } else {
            return responseHelper.createErroredCmPutResponse(cmResponse);
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

    private void obscureAndSetBody(CmPutRequest cmPutRequest, NbiCrudPutRequest nbiCrudPutRequest) {
        try {
            Set<String> sensitiveAttributesNames = cmReaderDao.getSensitiveAttributeNamesGivenFdn(cmPutRequest.getFdn());
            if (sensitiveAttributesNames != null) {
                String obscuredBody = compactAuditLoggerCreator.obscureBodyForCmPutCreateRequest(nbiCrudPutRequest.getBody(), sensitiveAttributesNames);
                cmContextService.setBody(obscuredBody);
            }
        } catch (Exception e) {
            logger.info("obscureAndSetBody::  Exception message={}", e.getMessage());
        }
    }

}
