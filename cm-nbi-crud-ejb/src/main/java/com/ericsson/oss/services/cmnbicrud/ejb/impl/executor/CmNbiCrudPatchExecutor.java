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

import com.ericsson.oss.itpf.sdk.security.accesscontrol.SecurityViolationException;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.annotation.Authorize;
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudPatchRequest;
import com.ericsson.oss.presentation.cmnbirest.api.NbiResponse;
import com.ericsson.oss.services.cm.cmshared.dto.CmObject;
import com.ericsson.oss.services.cmnbicrud.ejb.cal.CompactAuditLoggerCreator;
import com.ericsson.oss.services.cmnbicrud.ejb.common.*;
import com.ericsson.oss.services.cmnbicrud.ejb.create.CmCreateResponse;
import com.ericsson.oss.services.cmnbicrud.ejb.dao.CmReaderDao;
import com.ericsson.oss.services.cmnbicrud.ejb.dao.CmWriterDao;
import com.ericsson.oss.services.cmnbicrud.ejb.delete.CmDeleteResponse;
import com.ericsson.oss.services.cmnbicrud.ejb.exceptions.CmRestNbiValidationException;
import com.ericsson.oss.services.cmnbicrud.ejb.log.ResponseHelper;
import com.ericsson.oss.services.cmnbicrud.ejb.patch.CmPatchRequest;
import com.ericsson.oss.services.cmnbicrud.ejb.put.CmPutResponse;
import com.ericsson.oss.services.cmnbicrud.spi.output.MoObjects;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;

import static com.ericsson.oss.services.cmnbicrud.ejb.common.CmRestNbiConstants.*;
import static com.ericsson.oss.services.cmnbicrud.ejb.common.FdnUtility.fdnToBeRemoved;

public class CmNbiCrudPatchExecutor {

    private static final Logger logger = LoggerFactory.getLogger(CmNbiCrudPatchExecutor.class);

    private String commonParent = "";

    @Inject
    ResponseHelper responseHelper;

    @Inject
    CmNbiCheckInputParameters cmNbiCheckInputParameters;

    @Inject
    CmNbiCrudPatchExecutorBean cmNbiCrudPatchExecutorBean;

    @Inject
    CmWriterDao cmWriterDao;

    @Inject
    CmReaderDao cmReaderDao;

    @Inject
    CompactAuditLoggerCreator compactAuditLoggerCreator;

    @Inject
    private CmContextService cmContextService;

    @Authorize(resource = RBAC_RESOURCE_CM_NBI_CRUD, action = RBAC_ACTION_DELETE)
    public NbiResponse executeOperation(final NbiCrudPatchRequest nbiCrudPatchRequest) {
        List<WriteResponse> responses = executor(nbiCrudPatchRequest);
        return createNbiResponse(responses);
    }

    @SuppressWarnings({"squid:S2259"})
    private NbiResponse createNbiResponse(List<WriteResponse> responses) {
        NbiResponse nbiResponse = null;
        final ObjectMapper objectMapper = new ObjectMapper();

        if (!containsErroredResponse(responses)) {
            //successful response
            logger.debug("CmNbiCrudPatchExecutor:: response is SUCCESS");
            compactAuditLoggerCreator.setCompactAuditLogSummaryForPatch(responses);
            final String fdnToBeRemoveved = fdnToBeRemoved(commonParent);
            MoObjects moObjects = formatAllMos(getAllCmObjects(responses), fdnToBeRemoveved);
            final JsonNode jsonNode = objectMapper.valueToTree(moObjects);
            nbiResponse = new NbiResponse(PATCH_SUCCESS_HTTP_CODE, jsonNode.toString());
        } else {
            WriteResponse writeResponse = getFirstErroredResponse(responses);
            //failed response
            logger.debug("CmNbiCrudPatchExecutor:: response is FAIL with httpCode={}", writeResponse.getHttpCode());
            final JsonNode jsonNode = objectMapper.valueToTree(writeResponse.getErrorResponseType());
            nbiResponse = new NbiResponse(writeResponse.getHttpCode(), jsonNode.toString());
        }
        return nbiResponse;
    }

    private  List<WriteResponse> executor(final NbiCrudPatchRequest nbiCrudPatchRequest)  {
        List<WriteResponse> responses = new ArrayList<>();
        try {
            logger.debug("CmNbiCrudPatchExecutor::nbiCrudPatchRequest={}", nbiCrudPatchRequest);
            CmPatchRequest cmPatchRequest = cmNbiCheckInputParameters.generateCmPatchRequest(nbiCrudPatchRequest);
            logger.debug("CmNbiCrudPatchExecutor::cmPatchRequest={}", cmPatchRequest);

            // Preliminary Checks
            cmReaderDao.checkIfDatabaseIsAvailable();

            logger.debug("CmNbiCrudPatchExecutor:: checkIfExistsFdn cmPatchRequest.getCommonParent()={}", cmPatchRequest.getCommonParent());
            cmReaderDao.checkIfExistsFdn(cmPatchRequest.getCommonParent());

            // here should be code for body obscuration (see sample inside snippet.txt) but, according to US criteria, we do not implement it (too complex for now) and only flatten body
            flattenBodyForCmPatchRequest(nbiCrudPatchRequest.getBody());

            logger.debug("CmNbiCrudPatchExecutor:: checkIfValidateFdnAgainstTbac cmPatchRequest.getLongestSon()={}", cmPatchRequest.getLongestSon());
            cmWriterDao.checkIfValidateFdnAgainstTbac(cmPatchRequest.getLongestSon());

            responses = cmNbiCrudPatchExecutorBean.executor(cmPatchRequest.getRequests());
            commonParent = cmPatchRequest.getCommonParent();
        }  catch (final JsonMappingException | JsonParseException e) {
            logger.debug("CmNbiCrudPatchExecutor::executor:: nbiCrudPatchRequest={}:: catch Json Exception e.getClass={}, e.getMessage={} ", nbiCrudPatchRequest.getBody(), e.getClass(), e.getMessage());
            responses.add(responseHelper.handleJsonExceptionCmPatchResponse(e));
        } catch (final CmRestNbiValidationException e) {
            logger.debug("CmNbiCrudPatchExecutor::executor:: nbiCrudPatchRequest={}:: catch CmRestNbiValidationException e.getClass={}, e.getMessage={} ", nbiCrudPatchRequest, e.getClass(), e.getMessage());
            responses.add(responseHelper.handleValidationExceptionCmPatchResponse(e));
        } catch (Exception e) {
            if (responseHelper.isSecurityViolationException(e)) {
                logger.debug("CmNbiCrudPatchExecutor::executor:: nbiCrudPatchRequest={}:: catch SecurityViolationException e.getCause().getMessage()={} ", nbiCrudPatchRequest, e.getCause().getMessage());
                throw new SecurityViolationException(e.getCause().getMessage());
            }
            responses.add(responseHelper.handleExceptionCmPatchResponse(e));
        }
        return responses;
    }

    /*
     * P R I V A T E - M E T H O D S
    */
    private boolean containsErroredResponse(List<WriteResponse> responses) {
        for (final WriteResponse response:responses) {
            if (response.isErrored()) {
                return true;
            }
        }
        return false;
    }

    private WriteResponse getFirstErroredResponse(List<WriteResponse> responses) {
        for (final WriteResponse response:responses) {
            if (response.isErrored()) {
                return response;
            }
        }
        return null;
    }

    @SuppressWarnings({"squid:S3776"})
    private List<CmObject> getAllCmObjects(List<WriteResponse> responses) {
        Map<String, CmObject> cmObjectMap = new HashMap<>();
        for (final WriteResponse response:responses) {
            if (response instanceof CmCreateResponse) {
                for (CmObject cmObject:response.getCmObjects()) {
                    cmObjectMap.put(cmObject.getFdn(), cmObject);
                }
            } else if (response instanceof CmPutResponse) {
                for (CmObject cmObject:response.getCmObjects()) {
                    if (cmObjectMap.containsKey(cmObject.getFdn())) {
                        fillAttributes(cmObjectMap.get(cmObject.getFdn()), cmObject);
                    } else {
                        cmObjectMap.put(cmObject.getFdn(), cmObject);
                    }
                }
            } else if (response instanceof CmDeleteResponse) {
                for (CmObject cmObject:response.getCmObjects()) {
                    if (cmObjectMap.containsKey(cmObject.getFdn())) {
                        cmObjectMap.remove(cmObject.getFdn());
                    }
                }
            }
        } //end for
        List<CmObject> listOfCmObjects = new ArrayList<>();

        listOfCmObjects.addAll(cmObjectMap.values());
        return listOfCmObjects;
    }

    private MoObjects formatAllMos(final Collection<CmObject> cmObjectPlusAllDescendants, final String fdnToBeRemoved) {
        Node node = Node.createNodeHierarchy(cmObjectPlusAllDescendants, fdnToBeRemoved);
        return Node.convertInMoObjects(node);
    }

    private void fillAttributes(CmObject oldCmObject, CmObject newCmObject) {
        for (Map.Entry<String,Object> entry: newCmObject.getAttributes().entrySet()) {
            String attribute = entry.getKey();
            Object value = entry.getValue();
            oldCmObject.getAttributes().put(attribute,value);
        }
    }

    private void flattenBodyForCmPatchRequest(final String body) {
        String flattenBody = compactAuditLoggerCreator.flattenBodyForCmPatchRequest(body);
        cmContextService.setBody(flattenBody);
    }
}
