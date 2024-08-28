/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
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
import static com.ericsson.oss.services.cmnbicrud.ejb.common.FdnUtility.extractTypeFromFdn;

import com.ericsson.oss.itpf.sdk.security.accesscontrol.annotation.Authorize;
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudDeleteRequest;
import com.ericsson.oss.presentation.cmnbirest.api.NbiResponse;
import com.ericsson.oss.services.cm.cmshared.dto.CmObject;
import com.ericsson.oss.services.cm.cmshared.dto.CmOutputSpecification;
import com.ericsson.oss.services.cm.cmshared.dto.CmResponse;
import com.ericsson.oss.services.cm.cmshared.dto.search.CmSearchCriteria;
import com.ericsson.oss.services.cmnbicrud.ejb.cal.CompactAuditLoggerCreator;
import com.ericsson.oss.services.cmnbicrud.ejb.common.CmRestNbiError;
import com.ericsson.oss.services.cmnbicrud.ejb.common.ExceptionFactory;
import com.ericsson.oss.services.cmnbicrud.ejb.common.ResponseType;
import com.ericsson.oss.services.cmnbicrud.ejb.common.ScopeType;
import com.ericsson.oss.services.cmnbicrud.ejb.dao.CmReaderDao;
import com.ericsson.oss.services.cmnbicrud.ejb.dao.CmWriterDao;
import com.ericsson.oss.services.cmnbicrud.ejb.delete.CmDeleteResponse;
import com.ericsson.oss.services.cmnbicrud.ejb.exceptions.CmRestNbiValidationException;
import com.ericsson.oss.services.cmnbicrud.ejb.delete.CmDeleteRequest;
import com.ericsson.oss.services.cmnbicrud.ejb.get.CmGetRequest;
import com.ericsson.oss.services.cmnbicrud.ejb.log.ResponseHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

public class CmNbiCrudDeleteExecutor {

    @Inject
    ResponseHelper responseHelper;

    @Inject
    ExceptionFactory exceptionFactory;

    @Inject
    CmNbiCheckInputParameters cmNbiCheckInputParameters;

    @Inject
    CmGetRequestParser cmGetRequestParser;

    @Inject
    CmReaderDao cmReaderDao;

    @Inject
    CmWriterDao cmWriterDao;

    @Inject
    CompactAuditLoggerCreator compactAuditLoggerCreator;

    private static final Logger logger = LoggerFactory.getLogger(CmNbiCrudDeleteExecutor.class);

    private static final List<CmObject> EMPTY_CMOBJECTS = new ArrayList<>();
    private static final Map<String, Set<String>> NO_ATTRIBUTES = new HashMap<>();

    @Authorize(resource = RBAC_RESOURCE_CM_NBI_CRUD, action = RBAC_ACTION_DELETE)
    public NbiResponse executeOperation(final NbiCrudDeleteRequest nbiCrudDeleteRequest) {
        CmDeleteResponse cmDeleteResponse = executor(nbiCrudDeleteRequest);
        return createNbiResponse(cmDeleteResponse);
    }

    private NbiResponse createNbiResponse(CmDeleteResponse cmDeleteResponse) {
        NbiResponse nbiResponse = null;
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setDateFormat(new SimpleDateFormat(SIMPLE_DATE_FORMAT_STRING));
        if (cmDeleteResponse.getResponseType() == ResponseType.SUCCESS) {
            //successful response
            logger.debug("CmNbiCrudDeleteExecutor:: response is SUCCESS");
            final JsonNode jsonNode = objectMapper.valueToTree(createListOfUri(cmDeleteResponse.getCmObjects()));
            nbiResponse = new NbiResponse(cmDeleteResponse.getHttpCode(), jsonNode.toString());
        } else {
            //failed response
            logger.debug("CmNbiCrudDeleteExecutor:: response is FAIL with httpCode={}", cmDeleteResponse.getHttpCode());
            final JsonNode jsonNode = objectMapper.valueToTree(cmDeleteResponse.getErrorResponseType());
            nbiResponse = new NbiResponse(cmDeleteResponse.getHttpCode(), jsonNode.toString());
        }
        return nbiResponse;
    }

   /*
    *                 DELETE COMMAND REQUEST
    *
    * */
    private CmDeleteResponse executor(final NbiCrudDeleteRequest nbiCrudDeleteRequest) {
        CmDeleteResponse cmDeleteResponse = null;
        Collection<CmObject>  objectsToBeDeleted = null;
        Collection<CmObject> deletedCmObjects = null;

        try {
            // Syntax validation
            CmDeleteRequest cmDeleteRequest = cmNbiCheckInputParameters.generateCmDeleteRequest(nbiCrudDeleteRequest);

            // semantic validation
            validateCmDeleteRequest(cmDeleteRequest);

            // Preliminary Checks
            cmWriterDao.checkIfValidateFdnAgainstTbac(cmDeleteRequest.getFdn());

            // create list of objects to be deleted
            objectsToBeDeleted = getObjectsToBeDeleted(cmDeleteRequest);

            // delete objects from DPS and returns deleted objects
            deletedCmObjects = nbiDeleteCmObjects(objectsToBeDeleted);

            if (cmDeleteRequest.getNoQueryParameters()) { // it implies onlyOneResourceDeleted
                // no matter if we return jsonContent = '[]'. Inside rest when http code = 204 we don't fill entity (see CmNbiCrudResource)
                compactAuditLoggerCreator.setCompactAuditLogSummaryForDelete(deletedCmObjects);
                cmDeleteResponse = new CmDeleteResponse(ResponseType.SUCCESS, null, EMPTY_CMOBJECTS, DELETE_SUCCESS_HTTP_CODE_NO_QUERY_PARAMETERS);
            } else {
                compactAuditLoggerCreator.setCompactAuditLogSummaryForDelete(deletedCmObjects);
                return new CmDeleteResponse(ResponseType.SUCCESS, null, deletedCmObjects, DELETE_SUCCESS_HTTP_CODE);
            }
        } catch (final CmRestNbiValidationException e) {
            logger.debug("CmNbiCrudDeleteExecutor::executor:: nbiCrudDeleteRequest={}:: catch CmRestNbiValidationException e.getClass={}, e.getMessage={} ", nbiCrudDeleteRequest, e.getClass(), e.getMessage());
            cmDeleteResponse = responseHelper.handleValidationExceptionCmDeleteResponse(e);
        } catch (Exception e) {
            logger.debug("CmNbiCrudDeleteExecutor::executor:: nbiCrudDeleteRequest={}:: catch Exception e.getClass={}, e.getMessage={} ", nbiCrudDeleteRequest, e.getClass(), e.getMessage());
            cmDeleteResponse = responseHelper.handleExceptionCmDeleteResponse(e);
        }

        return cmDeleteResponse;
    }

    /*
    *                 DELETE COMMAND REQUEST - FROM PATCH
    *
    * */
    public CmDeleteResponse executor(final CmDeleteRequest cmDeleteRequest) {
        CmDeleteResponse cmDeleteResponse;
        Collection<CmObject> deletedCmObjects;

        try {
            // delete objects from DPS and returns deleted objects
            deletedCmObjects = nbiDeleteCmObjects(cmDeleteRequest.getFdn());

            // create response
            cmDeleteResponse = new CmDeleteResponse(ResponseType.SUCCESS, null, deletedCmObjects, DELETE_SUCCESS_HTTP_CODE);

        } catch (final CmRestNbiValidationException e) {
            logger.debug("CmNbiCrudDeleteExecutor::executor:: cmDeleteRequest={}:: catch CmRestNbiValidationException e.getClass={}, e.getMessage={} ", cmDeleteRequest, e.getClass(), e.getMessage());
            cmDeleteResponse = responseHelper.handleValidationExceptionCmDeleteResponse(e);
        } catch (Exception e) {
            logger.debug("CmNbiCrudDeleteExecutor::executor:: cmDeleteRequest={}:: catch Exception e.getClass={}, e.getMessage={} ", cmDeleteRequest, e.getClass(), e.getMessage());
            cmDeleteResponse = responseHelper.handleExceptionCmDeleteResponse(e);
        }

        return cmDeleteResponse;
    }

    public void validateCmDeleteRequest(final CmDeleteRequest cmDeleteRequest) {

        // Preliminary Checks
        cmReaderDao.checkIfDatabaseIsAvailable();
        cmReaderDao.checkIfExistsFdn(cmDeleteRequest.getFdn());

        final ScopeType scopeType = cmDeleteRequest.getScopeType();
        final String filter = cmDeleteRequest.getFilter();

        checkIfScopeTypeAndFilterAreValidForDelete(scopeType, filter);

        if ((isXpathASubNetwork(cmDeleteRequest.getFdn())) && (scopeType != ScopeType.BASE_ONLY)) {
            throw exceptionFactory.createValidationException(CmRestNbiError.INVALID_SCOPE_TYPE_TO_DELETE_A_SUBNETWORK, scopeType);
        }

        validateScopeTypeForDeleteOperation(scopeType, cmDeleteRequest.getFdn());
    }

    /*
     * P R I V A T E - M E T H O D S
     */
     
    private void validateScopeTypeForDeleteOperation(final ScopeType scopeType, final String fdn) {

        CmResponse cmResponse = null;


        if (scopeType == ScopeType.BASE_ONLY) {
            // Delete is allowed only if it has no children
            cmResponse = cmReaderDao.getAllHierarchyWithBaseSubTree(fdn, NO_ATTRIBUTES, 1);

            if (isSuccessful(cmResponse) && hasChildren(cmResponse)) {
                throw exceptionFactory.createValidationException(CmRestNbiError.FDN_HAS_CHIDREN, scopeType, fdn);
            }
        }
    }

    private boolean isSuccessful(CmResponse cmResponse) {
        return cmResponse.getErrorCode() == 0;
    }

    private boolean hasChildren(CmResponse cmResponse) {
        return cmResponse.getCmObjects().size() > 1;
    }

    private void checkIfScopeTypeAndFilterAreValidForDelete(final ScopeType scopeType, final String filter) {
        if (scopeType == ScopeType.BASE_SUBTREE) {
            throw exceptionFactory.createValidationException(CmRestNbiError.INVALID_SCOPE_TYPE_FOR_DELETE, scopeType);
        }
        if ( (filter != null) && (scopeType == ScopeType.BASE_NTH_LEVEL || scopeType == ScopeType.BASE_ONLY)) {
            throw exceptionFactory.createValidationException(CmRestNbiError.FILTER_NOT_ALLOWED_WITH_SCOPE_TYPE_IN_DELETE, scopeType);
        }
    }

    private Collection<CmObject> getObjectsToBeDeleted(final CmDeleteRequest cmDeleteRequest) {
        CmResponse cmResponse;
        //  if      BASE_ALL and filter not null ... search objects using filter as criteria
        //  else if BASE_NTH_LEVEL - filter is not allowed in this case - get objects in hierarchy using scopeLevel
        //  else    - BASE_ONLY  is the only possible case not excluded by previous validation checks - object is FDN
        if ((cmDeleteRequest.getScopeType() == ScopeType.BASE_ALL) && (cmDeleteRequest.getFilter() != null)) {
            CmGetRequest cmGetRequest = new CmGetRequest(cmDeleteRequest.getFdn(), ScopeType.BASE_ALL, 0, NO_ATTRIBUTES, cmDeleteRequest.getFilter());
            CmSearchCriteria cmSearchCriteria = cmGetRequestParser.createCmSearchCriteria(cmGetRequest);
            List<CmOutputSpecification> cmOutputSpecifications = cmGetRequestParser.createCmOutputSpecifications(cmSearchCriteria, cmGetRequest);
            logger.debug("CmNbiCrudDeleteExecutor::executor:: cmDeleteRequest={}:: BASE_ALL+filter call createCmSearchCriteria cmSearchCriteria={}, cmOutputSpecifications={}", cmDeleteRequest, cmSearchCriteria, cmOutputSpecifications);
            cmResponse = cmReaderDao.search(cmSearchCriteria, cmOutputSpecifications);
            cmGetRequestParser.fiilterCmResponseIfNecessary(cmGetRequest, cmResponse);
            return cmResponse.getCmObjects();
        } else if ((cmDeleteRequest.getScopeType() == ScopeType.BASE_NTH_LEVEL) && (cmDeleteRequest.getFilter() == null)) {
            cmResponse = cmReaderDao.getAllHierarchyWithBaseNthLevel(cmDeleteRequest.getFdn(), NO_ATTRIBUTES, cmDeleteRequest.getScopeLevel());
            if (isSuccessful(cmResponse)) {
                return (cmResponse.getCmObjects());
            } else {
                throw exceptionFactory.createValidationException(CmRestNbiError.SERVER_ERROR, cmResponse.getStatusMessage());
            }
        } else {
            ArrayList<CmObject> objectList = new ArrayList<>();
            objectList.add(CmObject.fromFdn(cmDeleteRequest.getFdn()));
            return objectList;
        }
    }

    private Collection<CmObject>  nbiDeleteCmObjects(final Collection<CmObject>  objectsToBeDeleted ) {
        CmResponse cmResponse = null;
        ArrayList<CmObject> deletedObjects = new ArrayList<>();
        for (CmObject cmObject : objectsToBeDeleted) {
            // Save list of all children to be added to response
            cmResponse = cmReaderDao.getAllHierarchyWithBaseAll(cmObject.getFdn(), NO_ATTRIBUTES);
            if (isSuccessful(cmResponse)) {
                deletedObjects.addAll(cmResponse.getCmObjects());
            } else {
                throw exceptionFactory.createValidationException(CmRestNbiError.SERVER_ERROR, cmResponse.getStatusMessage());
            }
        }
        cmWriterDao.deleteObjectsForNbi(objectsToBeDeleted);
        return deletedObjects;
    }

    private Collection<CmObject> nbiDeleteCmObjects(final String fdn) {
        CmResponse cmResponse = null;
        ArrayList<CmObject> deletedObjects;
        //Save list of all children to be added to response
        cmResponse = cmReaderDao.getAllHierarchyWithBaseAll(fdn, NO_ATTRIBUTES);
        if (isSuccessful(cmResponse)) {
            deletedObjects = new ArrayList<>(cmResponse.getCmObjects());
        } else {
            throw exceptionFactory.createValidationException(CmRestNbiError.SERVER_ERROR, cmResponse.getStatusMessage());
        }
        cmWriterDao.deleteObjectsForNbiPatch(fdn);
        return deletedObjects;
    }

    private String getUri(final String fdn) {
        if (fdn != null) {
            return fdn.replaceAll(",","/");
        }
        return fdn;
    }

    private List<String> createListOfUri(Collection<CmObject> cmObjects) {
        List<String> listOfFdns = new ArrayList<>();
        Iterator<CmObject> it = cmObjects.iterator();
        while (it.hasNext()) {
            listOfFdns.add(getUri(it.next().getFdn()));
        }
        return listOfFdns;
    }

    private boolean isXpathASubNetwork(final String fdn) {
        return ("SubNetwork".equalsIgnoreCase(extractTypeFromFdn(fdn)));
    }
}
