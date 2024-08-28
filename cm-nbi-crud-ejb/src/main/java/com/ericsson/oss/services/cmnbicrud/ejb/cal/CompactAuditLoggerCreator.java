package com.ericsson.oss.services.cmnbicrud.ejb.cal;

import com.ericsson.oss.presentation.cmnbirest.api.PatchContentType;
import com.ericsson.oss.presentation.cmnbirest.api.NbiRequest;
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudGetRequest;
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudPutRequest;
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudPostRequest;
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudDeleteRequest;
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudPatchRequest;
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudActionRequest;

import com.ericsson.oss.services.cm.cmshared.dto.CmObject;
import com.ericsson.oss.services.cmnbicrud.ejb.common.CmContextService;
import com.ericsson.oss.services.cmnbicrud.ejb.common.WriteResponse;
import com.ericsson.oss.services.cmnbicrud.ejb.create.CmCreateResponse;
import com.ericsson.oss.services.cmnbicrud.ejb.dao.CmReaderDao;
import com.ericsson.oss.services.cmnbicrud.ejb.delete.CmDeleteResponse;
import com.ericsson.oss.services.cmnbicrud.ejb.put.CmPutResponse;
import com.ericsson.oss.services.cmnbicrud.spi.output.MoObjects;
import com.ericsson.oss.services.cmnbicrud.spi.output.JsonPatchObject;
import com.ericsson.oss.services.cmnbicrud.spi.output.ResourceRepresentationType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Locale;
import java.util.List;
import java.util.HashMap;

public class CompactAuditLoggerCreator {
    public static final String SENSITIVE_STRING = "************";
    public static final String NOT_AVAILABLE = "N/A";
    public static final String MANAGED_OBJECT = "ManagedObject";
    public static final String RESULT_TOTAL_OBJECTS = "total";

    @Inject
    private Logger logger;

    @Inject
    private CmContextService cmContextService;

    @Inject
    CmReaderDao cmReaderDao;
    /*
    * Recover URI and query parameter information
    * */

    public String getUriAndParamsFromRequest(final NbiRequest nbiRequest) {
        if (nbiRequest instanceof NbiCrudGetRequest) {
            return getUriAndParamsFromRequest((NbiCrudGetRequest)nbiRequest);
        }
        else if (nbiRequest instanceof NbiCrudPutRequest) {
            return getUriAndParamsFromRequest((NbiCrudPutRequest)nbiRequest);
        }
        else if (nbiRequest instanceof NbiCrudPostRequest) {
            return getUriAndParamsFromRequest((NbiCrudPostRequest)nbiRequest);
        }
        else if (nbiRequest instanceof NbiCrudDeleteRequest) {
            return getUriAndParamsFromRequest((NbiCrudDeleteRequest)nbiRequest);
        }
        else if (nbiRequest instanceof NbiCrudPatchRequest) {
            return getUriAndParamsFromRequest((NbiCrudPatchRequest)nbiRequest);
        }
        else if (nbiRequest instanceof NbiCrudActionRequest) {
            return getUriAndParamsFromRequest((NbiCrudActionRequest)nbiRequest);
        }
        return NOT_AVAILABLE;
    }

    private String getUriAndParamsFromRequest(NbiCrudGetRequest nbiCrudGetRequest) {
        return "/enm-nbi/cm/v1/data/" + nbiCrudGetRequest.getXpath() + getParamsFromRequest(nbiCrudGetRequest);
    }

    private String getUriAndParamsFromRequest(NbiCrudPutRequest nbiCrudPutRequest) {
        return "/enm-nbi/cm/v1/data/" + nbiCrudPutRequest.getXpath();
    }

    private String getUriAndParamsFromRequest(NbiCrudPostRequest nbiCrudPostRequest) {
        return "/enm-nbi/cm/v1/data/" + nbiCrudPostRequest.getXpath();
    }

    private String getUriAndParamsFromRequest(NbiCrudDeleteRequest nbiCrudDeleteRequest) {
        return "/enm-nbi/cm/v1/data/" + nbiCrudDeleteRequest.getXpath() + getParamsFromRequest(nbiCrudDeleteRequest);
    }

    private String getUriAndParamsFromRequest(NbiCrudPatchRequest nbiCrudPatchRequest) {
        return "/enm-nbi/cm/v1/data/" + nbiCrudPatchRequest.getXpath();
    }


    private String getUriAndParamsFromRequest(NbiCrudActionRequest nbiCrudActionRequest) {
        return "/enm-nbi/cm/v1/data/" + nbiCrudActionRequest.getXpath() + getParamsFromRequest(nbiCrudActionRequest);
    }

    private String getParamsFromRequest(NbiCrudGetRequest nbiCrudGetRequest) {
        String params = null;
        if (nbiCrudGetRequest.getScopeType() != null) {
            params = "scopeType=" + nbiCrudGetRequest.getScopeType();
        }
        if (nbiCrudGetRequest.getFields() != null) {
            params = ((params != null) ?  (params + "&fields=") : "fields=") + nbiCrudGetRequest.getFields();
        }
        if (nbiCrudGetRequest.getAttributes() != null) {
            params = ((params != null) ?  (params + "&attributes=") : "attributes=") + nbiCrudGetRequest.getAttributes();
        }
        if (nbiCrudGetRequest.getScopeLevel() != 0) {
            params = ((params != null) ?  (params + "&scopeLevel=") : "scopeLevel=") + nbiCrudGetRequest.getScopeLevel();
        }
        if (nbiCrudGetRequest.getFilter() != null) {
            params = ((params != null) ?  (params + "&filter=") : "filter=") + nbiCrudGetRequest.getFilter();
        }
        if (params != null) {
            params = "?" + params;
        }
        return (params != null) ?  params : "";
    }

    private String getParamsFromRequest(NbiCrudDeleteRequest nbiCrudDeleteRequest) {
        String params = null;
        if (nbiCrudDeleteRequest.getScopeType() != null) {
            params = "scopeType=" + nbiCrudDeleteRequest.getScopeType();
        }
        if (nbiCrudDeleteRequest.getScopeLevel() != 0) {
            params = ((params != null) ?  (params + "&scopeLevel=") : "scopeLevel=") + nbiCrudDeleteRequest.getScopeLevel();
        }
        if (nbiCrudDeleteRequest.getFilter() != null) {
            params = ((params != null) ?  (params + "&filter=") : "filter=") + nbiCrudDeleteRequest.getFilter();
        }
        if (params != null) {
            params = "?" + params;
        }
        return (params != null) ?  params : "";
    }

    private String getParamsFromRequest(NbiCrudActionRequest nbiCrudActionRequest) {
        String params = null;
        if (nbiCrudActionRequest.getAction() != null) {
            params = "?action=" + nbiCrudActionRequest.getAction();
        }
        return (params != null) ?  params : "";
    }


    /*
     * Recover Slogan information
     * */
    public String getSloganFromRequest(final NbiRequest nbiRequest) {
        if (nbiRequest instanceof NbiCrudGetRequest) {
            return OperationSlogan.EXECUTE_GET.getSlogan();
        }
        else if (nbiRequest instanceof NbiCrudPutRequest) {
            return OperationSlogan.EXECUTE_PUT.getSlogan();
        }
        else if (nbiRequest instanceof NbiCrudPostRequest) {
            return OperationSlogan.EXECUTE_POST.getSlogan();
        }
        else if (nbiRequest instanceof NbiCrudDeleteRequest) {
            return OperationSlogan.EXECUTE_DELETE.getSlogan();
        }
        else if (nbiRequest instanceof NbiCrudPatchRequest) {
            if (((NbiCrudPatchRequest) nbiRequest).getPatchContentType() == PatchContentType.JSON_PATCH) {
                return OperationSlogan.EXECUTE_JSON_PATCH.getSlogan();
            } else {
                return OperationSlogan.EXECUTE_THREE_GPP_JSON_PATCH.getSlogan();
            }
        }
        else if (nbiRequest instanceof NbiCrudActionRequest) {
            return OperationSlogan.EXECUTE_ACTION.getSlogan();
        }
        return OperationSlogan.NA.getSlogan();
    }

    public String getBodyBeforeStartingRequest(final NbiRequest nbiRequest) {
        if (nbiRequest instanceof NbiCrudPutRequest || nbiRequest instanceof NbiCrudPostRequest || nbiRequest instanceof NbiCrudPatchRequest || nbiRequest instanceof NbiCrudActionRequest) {
            return NOT_AVAILABLE;
        }
        return null;
    }

    /*
     * Prepare Summary information
     * */

    public void setCompactAuditLogSummaryForGet(int num) {
        CompactSummary compactSummary = new CompactSummary();
        SummaryResultOperation summaryResultOperation = new SummaryResultOperation(CompactSummary.OP_TYPE_READ, MANAGED_OBJECT);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put(RESULT_TOTAL_OBJECTS, num);
        summaryResultOperation.setResult(result);
        compactSummary.addSummaryResultOperation(summaryResultOperation);
        cmContextService.setCompactAuditLogSummary(convertToJson(compactSummary));
    }

    public void setCompactAuditLogSummaryForDelete(Collection<CmObject> deletedCmObjects) {
        CompactSummary compactSummary = new CompactSummary();
        for (CmObject cmObject : deletedCmObjects) {
            DetailResultOperation detailResultOperation = new DetailResultOperation(CompactSummary.OP_TYPE_DELETE, cmObject.getFdn());
            compactSummary.addDetailResultOperation(detailResultOperation);
        }
        cmContextService.setCompactAuditLogSummary(convertToJson(compactSummary));
    }

    public void setCompactAuditLogSummaryForPutModify(CmObject cmObject) {
        CompactSummary compactSummary = new CompactSummary();
        DetailResultOperation detailResultOperation = new DetailResultOperation(CompactSummary.OP_TYPE_UPDATE, cmObject.getFdn());
        detailResultOperation.setCurrentValues(cmObject.getAttributes());
        detailResultOperation.setOldValues(cmObject.getOldAttributes());
        compactSummary.addDetailResultOperation(detailResultOperation);
        cmContextService.setCompactAuditLogSummary(convertToJson(compactSummary));
    }

    public void setCompactAuditLogSummaryForCreate(CmObject cmObject) {
        CompactSummary compactSummary = new CompactSummary();
        DetailResultOperation detailResultOperation = new DetailResultOperation(CompactSummary.OP_TYPE_CREATE, cmObject.getFdn());
        detailResultOperation.setCurrentValues(cmObject.getAttributes());
        compactSummary.addDetailResultOperation(detailResultOperation);
        cmContextService.setCompactAuditLogSummary(convertToJson(compactSummary));
    }

    public void setCompactAuditLogSummaryForPatch(List<WriteResponse> responses) {
        CompactSummary compactSummary = new CompactSummary();
        for (final WriteResponse response:responses) {
            if (response instanceof CmCreateResponse) {
                for (CmObject cmObject:response.getCmObjects()) {
                    DetailResultOperation detailResultOperation = new DetailResultOperation(CompactSummary.OP_TYPE_CREATE, cmObject.getFdn());
                    detailResultOperation.setCurrentValues(cmObject.getAttributes());
                    compactSummary.addDetailResultOperation(detailResultOperation);
                }
            } else if (response instanceof CmPutResponse) {
                for (CmObject cmObject:response.getCmObjects()) {
                    DetailResultOperation detailResultOperation = new DetailResultOperation(CompactSummary.OP_TYPE_UPDATE, cmObject.getFdn());
                    detailResultOperation.setCurrentValues(cmObject.getAttributes());
                    detailResultOperation.setOldValues(cmObject.getOldAttributes());
                    compactSummary.addDetailResultOperation(detailResultOperation);
                }
            } else if (response instanceof CmDeleteResponse) {
                for (CmObject cmObject:response.getCmObjects()) {
                    DetailResultOperation detailResultOperation = new DetailResultOperation(CompactSummary.OP_TYPE_DELETE, cmObject.getFdn());
                    compactSummary.addDetailResultOperation(detailResultOperation);
                }
            }
        } //end for
        cmContextService.setCompactAuditLogSummary(convertToJson(compactSummary));
    }

    public void setCompactAuditLogSummaryForAction(CmObject cmObject) {
        if (cmObject.getAttributes().size() > 0) {
            CompactSummary compactSummary = new CompactSummary();
            DetailResultOperation detailResultOperation = new DetailResultOperation(CompactSummary.OP_TYPE_ACTION, cmObject.getFdn());
            detailResultOperation.setCurrentValues(cmObject.getAttributes());
            compactSummary.addDetailResultOperation(detailResultOperation);
            cmContextService.setCompactAuditLogSummary(convertToJson(compactSummary));
        } else {
            cmContextService.setCompactAuditLogSummary("");
        }
    }

    @SuppressWarnings({"squid:S1144"})
    private String convertToJson(final CompactSummary compactSummary) {
        final ObjectMapper objectMapper = new ObjectMapper();
        try {
            final JsonNode jsonNode = objectMapper.valueToTree(compactSummary);
            return jsonNode.toString();
        } catch (Exception ee) {
            logger.info("convertToJson exception so return empty json = NA  message={}", ee.getMessage());
        }
        return "NA";
    }

    /*
     *  Obscure Body Section for sensitive attributes
     * */

    /*
     *  Obscure POST/PUT(create)/PUT(modify) Body
     * */
    public String obscureBodyForCmPutCreateRequest(final String body, Set<String> sensitiveAttributeNames) {
        String newBody = body;

        ObjectMapper mapper = new ObjectMapper();
        MoObjects moos = null;
        try {
            moos = mapper.readValue(body, MoObjects.class);

            if (sensitiveAttributeNames != null && !sensitiveAttributeNames.isEmpty()) {
                Map<String, List<ResourceRepresentationType>> mapRRType = moos.getMoObjects();
                List<ResourceRepresentationType> listRRType = mapRRType.values().iterator().next();
                ResourceRepresentationType rRType = listRRType.get(0);

                //change attributes map
                rRType.setAttributes(obscureSensitiveAttributes(rRType.getAttributes(), sensitiveAttributeNames));
            }

            final JsonNode jsonNode = mapper.valueToTree(moos);
            newBody = jsonNode.toString();
        } catch (Exception e) {
            logger.info("obscureBodyForCmPutCreateRequest::  Exception message={}", e.getMessage());
        }

        return newBody;
    }

    public String obscureBodyForCmActionRequest(final String body, Set<String> sensitiveActionParameterNames) {
        String newBody = body;
        if (body != null && !body.isEmpty()) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                HashMap<String, Object> jsonPatchActionInputMap = mapper.readValue(body, HashMap.class);
                Object inputValue = jsonPatchActionInputMap.get("input");
                if (inputValue instanceof HashMap) {
                    Map<String, Object> attributes = (HashMap<String, Object>) inputValue;
                    jsonPatchActionInputMap.put("input", obscureSensitiveAttributes(attributes, sensitiveActionParameterNames));
                    final JsonNode jsonNode = mapper.valueToTree(jsonPatchActionInputMap);
                    newBody = jsonNode.toString();
                }
            } catch (Exception e) {
                logger.info("obscureBodyForCmActionRequest::  Exception message={}", e.getMessage());
            }
        }
        return newBody;
    }

    private Map<String, Object> obscureSensitiveAttributes(final Map<String, Object> attributes, Set<String> sensitiveAttributeNames) {
        Map<String, Object> obscuredAttributes = new LinkedHashMap();
        Set<String> sensitiveAttributeNamesInLowerCase = getSensitiveAttributeNamesInLowerCase(sensitiveAttributeNames);
        for (Map.Entry<String,Object> entry : attributes.entrySet()) {
            final String attributeName = entry.getKey();
            final Object atttributeValue = entry.getValue();
            if (sensitiveAttributeNamesInLowerCase.contains(attributeName.toLowerCase(Locale.ENGLISH))) {
                obscuredAttributes.put(attributeName, SENSITIVE_STRING);
            } else {
                obscuredAttributes.put(attributeName, atttributeValue);
            }
        }
        return obscuredAttributes;
    }

    private Set<String> getSensitiveAttributeNamesInLowerCase(Set<String> sensitiveAttributeNames) {
        Set<String> sensitiveAttributeNamesInLowerCase = new LinkedHashSet<>();
        for (final String attributeName:sensitiveAttributeNames) {
            sensitiveAttributeNamesInLowerCase.add(attributeName.toLowerCase(Locale.ENGLISH));
        }
        return sensitiveAttributeNamesInLowerCase;
    }

    /*
     *  Flatten Body for PATCH
     * */
    public String flattenBodyForCmPatchRequest(final String body) {
        String newBody = body;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonPatchObject[] jsonPatchObjects = mapper.readValue(body, JsonPatchObject[].class);
            final JsonNode jsonNode = mapper.valueToTree(jsonPatchObjects);
            newBody = jsonNode.toString();
        } catch (Exception e) {
            logger.info("flattenBodyForCmPatchRequest::  Exception message={}", e.getMessage());
        }
        return newBody;
    }
}
