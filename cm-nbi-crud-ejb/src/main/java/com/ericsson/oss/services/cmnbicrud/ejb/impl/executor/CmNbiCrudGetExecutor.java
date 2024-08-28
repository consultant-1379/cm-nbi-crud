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
import static com.ericsson.oss.services.cmnbicrud.ejb.common.FdnUtility.fdnToBeRemoved;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.security.accesscontrol.annotation.Authorize;
import com.ericsson.oss.services.cm.cmshared.dto.search.CmSearchCriteria;
import com.ericsson.oss.services.cmnbicrud.ejb.cal.CompactAuditLoggerCreator;
import com.ericsson.oss.services.cmnbicrud.ejb.common.ExceptionFactory;
import com.ericsson.oss.services.cmnbicrud.ejb.dao.CmReaderDao;
import com.ericsson.oss.services.cmnbicrud.ejb.exceptions.CmRestNbiValidationException;
import com.ericsson.oss.services.cmnbicrud.ejb.log.ResponseHelper;
import com.ericsson.oss.services.cmnbicrud.ejb.get.CmGetRequest;
import com.ericsson.oss.services.cmnbicrud.ejb.get.CmGetResponse;
import com.ericsson.oss.services.cmnbicrud.ejb.common.ResponseType;
import com.ericsson.oss.services.cmnbicrud.ejb.common.ScopeType;
import com.ericsson.oss.services.cmnbicrud.spi.output.MoObjects;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.cm.cmshared.dto.*;

import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudGetRequest;
import com.ericsson.oss.presentation.cmnbirest.api.NbiResponse;

public class CmNbiCrudGetExecutor {

    private static final Logger logger = LoggerFactory.getLogger(CmNbiCrudGetExecutor.class);

    @Inject
    ResponseHelper responseHelper;

    @Inject
    ExceptionFactory exceptionFactory;

    @Inject
    CmNbiCheckInputParameters cmNbiCheckInputParameters;

    @Inject
    CmReaderDao cmReaderDao;

    @Inject
    private CmGetRequestParser cmGetRequestParser;

    @Inject
    CompactAuditLoggerCreator compactAuditLoggerCreator;

    @Authorize(resource = RBAC_RESOURCE_CM_NBI_CRUD, action = RBAC_ACTION_READ)
    public NbiResponse executeOperation(final NbiCrudGetRequest nbiCrudGetRequest) {
        CmGetResponse cmGetResponse = executor(nbiCrudGetRequest);
        return createNbiResponse(cmGetResponse);
    }

    private NbiResponse createNbiResponse(CmGetResponse cmGetResponse) {
        NbiResponse nbiResponse = null;
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setDateFormat(new SimpleDateFormat(SIMPLE_DATE_FORMAT_STRING));
        if (cmGetResponse.getResponseType() == ResponseType.SUCCESS) {
            //successful response
            logger.debug("CmNbiCrudGetExecutor:: response is SUCCESS");
            final JsonNode jsonNode = objectMapper.valueToTree(cmGetResponse.getMoObjects());
            nbiResponse = new NbiResponse(cmGetResponse.getHttpCode(), jsonNode.toString());

        } else {
            //failed response
            logger.debug("CmNbiCrudGetExecutor:: response is FAIL with httpCode={}", cmGetResponse.getHttpCode());
            final JsonNode jsonNode = objectMapper.valueToTree(cmGetResponse.getErrorResponseType());
            nbiResponse = new NbiResponse(cmGetResponse.getHttpCode(), jsonNode.toString());
        }
        return nbiResponse;
    }


    private CmGetResponse executor(final NbiCrudGetRequest nbiCrudGetRequest) {
        CmGetResponse cmGetResponse = null;

        try {
            CmGetRequest cmGetRequest = cmNbiCheckInputParameters.generateCmGetRequest(nbiCrudGetRequest);
            cmNbiCheckInputParameters.validateCmGetRequest(cmGetRequest);

            // Preliminary Checks
            cmReaderDao.checkIfDatabaseIsAvailable();
            cmReaderDao.checkIfExistsFdn(cmGetRequest.getFdn());

            if (cmGetRequest.getScopeType() == ScopeType.BASE_ONLY) {

                //get by fdn
                logger.debug("CmNbiCrudGetExecutor::executor:: nbiCrudGetRequest={}:: call getMoByFdnForCmRestNbi", nbiCrudGetRequest);
                CmResponse cmResponse = cmReaderDao.getMoByFdnForCmRestNbi(cmGetRequest.getFdn(),cmGetRequest.getAttributes());
                cmGetResponse = formatSingleMo(cmResponse);
            } else if ((cmGetRequest.getScopeType() == ScopeType.BASE_ALL || cmGetRequest.getScopeType() == ScopeType.BASE_SUBTREE || cmGetRequest.getScopeType() == ScopeType.BASE_NTH_LEVEL) && !cmGetRequest.hasFilter()) {

                // get with all hierarchy
                logger.debug("CmNbiCrudGetExecutor::executor:: nbiCrudGetRequest={}:: call getAllHierarchy", nbiCrudGetRequest);
                CmResponse cmResponse = getAllHierarchy(cmGetRequest);

                // we want to remove unnecessary hierarchy so we start from requested fdn
                // So if we ask for MeContext=ERBS002,ManagedElement=1,ENodeBFunction=1  we will remove MeContext=ERBS002,ManagedElement=1, from all fdns
                final String fdnToBeRemoveved = fdnToBeRemoved(cmGetRequest.getFdn());
                cmGetResponse = formatAllMos(cmResponse, fdnToBeRemoveved);
            } else if ((cmGetRequest.getScopeType() == ScopeType.BASE_ALL || cmGetRequest.getScopeType() == ScopeType.BASE_SUBTREE || cmGetRequest.getScopeType() == ScopeType.BASE_NTH_LEVEL) && cmGetRequest.hasFilter()) {

                // get with all hierarchy
                CmSearchCriteria cmSearchCriteria = cmGetRequestParser.createCmSearchCriteria(cmGetRequest);
                List<CmOutputSpecification> cmOutputSpecifications = cmGetRequestParser.createCmOutputSpecifications(cmSearchCriteria, cmGetRequest);

                logger.debug("CmNbiCrudGetExecutor::executor:: nbiCrudGetRequest={}:: BASE_ALL+filter call createCmSearchCriteria cmSearchCriteria={}, cmOutputSpecifications={}", nbiCrudGetRequest, cmSearchCriteria, cmOutputSpecifications);
                CmResponse cmResponse = cmReaderDao.search(cmSearchCriteria, cmOutputSpecifications);

                cmGetRequestParser.fiilterCmResponseIfNecessary(cmGetRequest, cmResponse);

                // we want to remove unnecessary hierarchy so we start from requested fdn
                // So if we ask for MeContext=ERBS002,ManagedElement=1,ENodeBFunction=1  we will remove MeContext=ERBS002,ManagedElement=1, from all fdns
                final String fdnToBeRemoveved = fdnToBeRemoved(cmGetRequest.getFdn());
                cmGetResponse = formatAllMos(cmResponse, fdnToBeRemoveved);
            } else {
                cmGetResponse = responseHelper.createUnimplementedUseCaseCmGetResponse(cmGetRequest);
            }
        }catch (final CmRestNbiValidationException e) {
            logger.debug("CmNbiCrudGetExecutor::executor:: nbiCrudGetRequest={}:: catch CmRestNbiValidationException e.getClass={}, e.getMessage={} ", nbiCrudGetRequest, e.getClass(), e.getMessage());
            cmGetResponse = responseHelper.handleValidationException(e);
        } catch (Exception e) {
            logger.debug("CmNbiCrudGetExecutor::executor:: nbiCrudGetRequest={}:: catch Exception e.getClass={}, e.getMessage={} ", nbiCrudGetRequest, e.getClass(), e.getMessage());
            cmGetResponse = responseHelper.handleException(e);
        }

        return cmGetResponse;
    }

    /*
     * P R I V A T E - M E T H O D S
    */

    /*
    *  BASE_ONLY methods
    */
    private CmGetResponse formatSingleMo(final CmResponse cmResponse) {
        if (isSuccessful(cmResponse)) {
            if (hasCmObjects(cmResponse)) {
                compactAuditLoggerCreator.setCompactAuditLogSummaryForGet(getCmObjectsNum(cmResponse));
                return new CmGetResponse(ResponseType.SUCCESS, null, extractMoObject(cmResponse), GET_SUCCESS_HTTP_CODE);
            } else {
                compactAuditLoggerCreator.setCompactAuditLogSummaryForGet(0);
                return new CmGetResponse(ResponseType.SUCCESS, null, Node.convertInEmptyMoObjects(), GET_SUCCESS_HTTP_CODE);
            }
        } else {
            return responseHelper.createErroredCmGetResponse(cmResponse);
        }
    }

    private boolean hasCmObjects(CmResponse cmResponse) {
        return !cmResponse.getCmObjects().isEmpty();
    }

    private MoObjects extractMoObject(final CmResponse cmResponse) {
        CmObject cmObject = cmResponse.getCmObjects().iterator().next();
        return Node.convertInMoObjects(cmObject);
    }

    /*
    *  BASE_ALL,... methods
    */

    private CmResponse getAllHierarchy(CmGetRequest cmGetRequest) {
        CmResponse cmResponse = null;
        if (cmGetRequest.getScopeType() == ScopeType.BASE_ALL) {
            cmResponse = cmReaderDao.getAllHierarchyWithBaseAll(cmGetRequest.getFdn(), cmGetRequest.getAttributes());
        }
        else if (cmGetRequest.getScopeType() == ScopeType.BASE_SUBTREE) {
            cmResponse = cmReaderDao.getAllHierarchyWithBaseSubTree(cmGetRequest.getFdn(), cmGetRequest.getAttributes(), cmGetRequest.getScopeLevel());
        }
        else if (cmGetRequest.getScopeType() == ScopeType.BASE_NTH_LEVEL) {
            cmResponse = cmReaderDao.getAllHierarchyWithBaseNthLevel(cmGetRequest.getFdn(), cmGetRequest.getAttributes(), cmGetRequest.getScopeLevel());
        }
        return cmResponse;
    }

    private CmGetResponse formatAllMos(final CmResponse cmResponse, final String fdnToBeRemoved) {
        if (isSuccessful(cmResponse)) {
            if (hasCmObjects(cmResponse)) {
                compactAuditLoggerCreator.setCompactAuditLogSummaryForGet(getCmObjectsNum(cmResponse));
                return formatAllMos(cmResponse.getCmObjects(), fdnToBeRemoved);
            } else {
                compactAuditLoggerCreator.setCompactAuditLogSummaryForGet(0);
                return new CmGetResponse(ResponseType.SUCCESS, null, Node.convertInEmptyMoObjects(), GET_SUCCESS_HTTP_CODE);
            }
        } else {
            return responseHelper.createErroredCmGetResponse(cmResponse);
        }
    }

    private boolean isSuccessful(CmResponse cmResponse) {
        return cmResponse.getErrorCode() == 0;
    }

    private CmGetResponse formatAllMos(final Collection<CmObject> cmObjectPlusAllDescendants, final String fdnToBeRemoved) {
        Node node = Node.createNodeHierarchy(cmObjectPlusAllDescendants, fdnToBeRemoved);
        MoObjects rootMoObject = Node.convertInMoObjects(node);
        return new CmGetResponse(ResponseType.SUCCESS, null, rootMoObject, GET_SUCCESS_HTTP_CODE);
    }

    private int getCmObjectsNum(final CmResponse cmResponse) {
        return cmResponse.getCmObjects().size();
    }
}
