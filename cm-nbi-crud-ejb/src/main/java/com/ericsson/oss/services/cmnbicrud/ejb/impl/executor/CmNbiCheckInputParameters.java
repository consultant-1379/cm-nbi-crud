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

import com.ericsson.oss.presentation.cmnbirest.api.*;
import com.ericsson.oss.services.cmnbicrud.ejb.common.CmRestNbiError;
import com.ericsson.oss.services.cmnbicrud.ejb.common.ExceptionFactory;
import com.ericsson.oss.services.cmnbicrud.ejb.common.ScopeType;

import com.ericsson.oss.services.cmnbicrud.ejb.action.CmActionRequest;
import com.ericsson.oss.services.cmnbicrud.ejb.common.WriteRequest;
import com.ericsson.oss.services.cmnbicrud.ejb.create.CmCreateRequest;
import com.ericsson.oss.services.cmnbicrud.ejb.delete.CmDeleteRequest;
import com.ericsson.oss.services.cmnbicrud.ejb.get.CmGetRequest;
import com.ericsson.oss.services.cmnbicrud.ejb.patch.CmPatchRequest;
import com.ericsson.oss.services.cmnbicrud.ejb.patch.MapWithOnlyOneComplexMember;
import com.ericsson.oss.services.cmnbicrud.ejb.put.CmPutRequest;
import com.ericsson.oss.services.cmnbicrud.spi.output.JsonPatchObject;
import com.ericsson.oss.services.cmnbicrud.spi.output.MoObjects;
import com.ericsson.oss.services.cmnbicrud.spi.output.ResourceRepresentationType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import java.io.IOException;
import java.util.*;

import static com.ericsson.oss.services.cmnbicrud.ejb.common.CmRestNbiConstants.*;
import static com.ericsson.oss.services.cmnbicrud.ejb.common.FdnUtility.extractNameFromFdn;
import static com.ericsson.oss.services.cmnbicrud.ejb.common.FdnUtility.extractTypeFromFdn;
import static com.ericsson.oss.services.cmnbicrud.ejb.common.FdnUtility.getParentFdn;
import static com.ericsson.oss.services.cmnbicrud.ejb.common.CmForbiddenBaseType.isForbiddenType;

public class CmNbiCheckInputParameters  {

    private static final String ELEMENTS_SEPARATOR = ",";
    public static final String SLASH = "/";
    private static final String ATTRIBUTES = "attributes";
    private static final String HASHTAG = "#";
    public static final String ATTRIBUTES_IN_3GPP_JSON_PATCH1 = HASHTAG + SLASH + ATTRIBUTES + SLASH;
    public static final String ATTRIBUTES_IN_3GPP_JSON_PATCH2 = HASHTAG + ATTRIBUTES + SLASH;
    public static final String ATTRIBUTES_IN_JSON_PATCH1 = SLASH + ATTRIBUTES + SLASH;
    public static final String ATTRIBUTES_IN_JSON_PATCH2 = ATTRIBUTES + SLASH;
    private static final String EQUAL_SEPARATOR = "=";


    private static final long serialVersionUID = 5616557036361539638L;
    private static final Logger logger = LoggerFactory.getLogger(CmNbiCheckInputParameters.class);

    @Inject
    private ExceptionFactory exceptionFactory;

    /*
    *                 GET COMMAND REQUEST
    *
    * */
    public CmGetRequest generateCmGetRequest(final NbiCrudGetRequest nbiCrudGetRequest) {

        return new CmGetRequest(
                getFdnFromXpathForGet(nbiCrudGetRequest.getXpath(), nbiCrudGetRequest.getFilter()),
                getScopeType(nbiCrudGetRequest.getScopeType()),
                getScopeLevel(nbiCrudGetRequest.getScopeLevel()),
                getAttributesAndFields(nbiCrudGetRequest.getAttributes(), nbiCrudGetRequest.getFields()),
                getFilter(nbiCrudGetRequest.getFilter()));
    }

    public void validateCmGetRequest(final CmGetRequest cmGetRequest) {
        if (cmGetRequest.getScopeType() == ScopeType.BASE_ONLY) {
            if (cmGetRequest.hasFilter()) {
                throw exceptionFactory.createValidationException(CmRestNbiError.UNIMPLEMENTED_USE_CASE, cmGetRequest.getDescritionForUnimplementedUseCase());
            }
        } else {
            if (!cmGetRequest.hasFilter() && isForbiddenTypeForCmGetRequest(cmGetRequest)) {
                throw exceptionFactory.createValidationException(CmRestNbiError.OVERALL_PROTECTION, "");
            }
        }

    }

    public boolean isForbiddenTypeForCmGetRequest(final CmGetRequest cmGetRequest) {
        String baseType = cmGetRequest.getFdn() == null? null: extractTypeFromFdn(cmGetRequest.getFdn());
        return isForbiddenType(baseType);
    }

    /*
    *                 PUT COMMAND REQUEST
    *
    * */
    public CmPutRequest generateCmPutRequest(final NbiCrudPutRequest nbiCrudPutRequest) throws IOException {
        String xpath = getFdnFromXpath(nbiCrudPutRequest.getXpath());

        ObjectMapper mapper = new ObjectMapper();
        MoObjects moos = mapper.readValue(nbiCrudPutRequest.getBody(), MoObjects.class);

        Map<String, List<ResourceRepresentationType>> mapRRType = moos.getMoObjects();
        String type = mapRRType.keySet().iterator().next();
        List<ResourceRepresentationType> listRRType = mapRRType.values().iterator().next();
        ResourceRepresentationType rRType = listRRType.get(0);
        return new CmPutRequest(xpath, rRType.getId(), type, rRType.getAttributes());
    }

    /*
    *                 POST (CREATE) COMMAND REQUEST
    *
    * */
    public CmCreateRequest generateCmCreateRequest(final NbiCrudPostRequest nbiCrudPostRequest) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        MoObjects moos = mapper.readValue(nbiCrudPostRequest.getBody(), MoObjects.class);

        Map<String, List<ResourceRepresentationType>> mapRRType = moos.getMoObjects();
        String childType = mapRRType.keySet().iterator().next();
        ResourceRepresentationType rRType = mapRRType.get(childType).iterator().next();
        
        String id = rRType.getId();
        if ("null".equals(id)) {
            id = null;
        }

        final String parentFdn = nbiCrudPostRequest.getXpath() == null? null : getFdnFromXpath(nbiCrudPostRequest.getXpath());
        Map<String, Object> attributes = rRType.getAttributes();
        return new CmCreateRequest(parentFdn, id, childType, attributes);
        }

    /*
    *                 PUT (CREATE) COMMAND REQUEST
    *
    * */
    public CmCreateRequest generateCmCreateRequest(final NbiCrudPutRequest nbiCrudPutRequest) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        MoObjects moos = mapper.readValue(nbiCrudPutRequest.getBody(), MoObjects.class);

        Map<String, List<ResourceRepresentationType>> mapRRType = moos.getMoObjects();
        String childType = mapRRType.keySet().iterator().next();
        ResourceRepresentationType rRType = mapRRType.get(childType).iterator().next();

        String id = rRType.getId();
        if ("null".equals(id)) {
            id = null;
        }

        checkIfIdNull(id);

        final String fdn = nbiCrudPutRequest.getXpath() == null? null : getFdnFromXpath(nbiCrudPutRequest.getXpath());
        checkParentChildMismatch(fdn, childType, id);

        Map<String, Object> attributes = rRType.getAttributes();
        return new CmCreateRequest(getParentFdn(fdn), id, childType, attributes);
    }

    /*
    *                 DELETE COMMAND REQUEST
    *
    * */
    public CmDeleteRequest generateCmDeleteRequest(final NbiCrudDeleteRequest nbiCrudDeleteRequest) {
        boolean noQueryParameters = (nbiCrudDeleteRequest.getScopeType() == null)
                && (nbiCrudDeleteRequest.getScopeLevel() == 0)
                && (nbiCrudDeleteRequest.getFilter() == null);

        return new CmDeleteRequest(
                getFdnFromXpath(nbiCrudDeleteRequest.getXpath()),
                getScopeType(nbiCrudDeleteRequest.getScopeType()),
                getScopeLevel(nbiCrudDeleteRequest.getScopeLevel()),
                getFilter(nbiCrudDeleteRequest.getFilter()),
                noQueryParameters);
    }

    /*
    *                 PATCH COMMAND REQUEST
    *
    * */
    public CmPatchRequest generateCmPatchRequest(final NbiCrudPatchRequest nbiCrudPatchRequest) throws IOException {
        if (nbiCrudPatchRequest.getPatchContentType() == PatchContentType.JSON_PATCH) {
            return generateCmPatchRequestJsonPatch(nbiCrudPatchRequest);
        } else if (nbiCrudPatchRequest.getPatchContentType() == PatchContentType.THREE_GPP_JSON_PATCH) {
            return generateCmPatchRequest3gppJsonPatch(nbiCrudPatchRequest);
        } else {
            throw exceptionFactory.createValidationException(CmRestNbiError.UNIMPLEMENTED_USE_CASE, "ContentType="+nbiCrudPatchRequest.getPatchContentType());
        }
    }

    /*
    *                 ACTION COMMAND REQUEST
    *
    * */
    @SuppressWarnings("squid:S1948")
    public CmActionRequest generateCmActionRequest(final NbiCrudActionRequest nbiCrudActionRequest) throws IOException {

        String action = checkAndGetAction(nbiCrudActionRequest.getAction());
        String fdn = getFdnFromXpath(nbiCrudActionRequest.getXpath());
        String body = nbiCrudActionRequest.getBody();
        Map<String, Object> attributes = new HashMap<>();

        logger.debug("CmNbiCheckInputParameters::generateCmActionRequest - nbiCrudActionRequest = {}", nbiCrudActionRequest);
        if (body != null && !body.isEmpty()) {
            ObjectMapper mapper = new ObjectMapper();
            HashMap<String, Object> jsonPatchActionInputMap = mapper.readValue(body, HashMap.class);

            Object inputValue = jsonPatchActionInputMap.get("input");
            if (inputValue instanceof HashMap) {
                attributes = (HashMap<String, Object>) inputValue;
            }
        }
        return new CmActionRequest(fdn, action, attributes);
    }

    /*
    *           3gpp Json Patch
    * */
    public CmPatchRequest generateCmPatchRequest3gppJsonPatch(final NbiCrudPatchRequest nbiCrudPatchRequest) throws IOException {
        List<WriteRequest> requests = new ArrayList<>();
        String commonParent = nbiCrudPatchRequest.getXpath() == null? null : getFdnFromXpath(nbiCrudPatchRequest.getXpath());
        String longestSon = null;

        logger.debug("CmNbiCheckInputParameters::generateCmPatchRequest3gppJsonPatch - nbiCrudPatchRequest = {}", nbiCrudPatchRequest);
        ObjectMapper mapper = new ObjectMapper();
        JsonPatchObject[] jsonPatchObjects = mapper.readValue(nbiCrudPatchRequest.getBody(), JsonPatchObject[].class);

        for (JsonPatchObject jsonPatchObject : jsonPatchObjects) {
            String jpOpType = jsonPatchObject.getOp();
            if (JSON_PATCH_ADD_OP_TYPE.equals(jpOpType)) {
                CmCreateRequest cmCreateRequest = generateCmCreateRequest3gppJsonPatch(nbiCrudPatchRequest.getXpath(), jsonPatchObject);
                commonParent = getCommonParent(commonParent, cmCreateRequest.getParentFdn());
                longestSon = getLongestSon(longestSon, cmCreateRequest.getParentFdn());
                requests.add(cmCreateRequest);
            } else if (JSON_PATCH_REMOVE_OP_TYPE.equals(jpOpType)) {
                CmDeleteRequest cmDeleteRequest = generateCmDeleteRequest3gppJsonPatch(nbiCrudPatchRequest.getXpath(), jsonPatchObject);
                commonParent = getCommonParent(commonParent, cmDeleteRequest.getFdn());
                longestSon = getLongestSon(longestSon, cmDeleteRequest.getFdn());
                requests.add(cmDeleteRequest);
            } else if (JSON_PATCH_REPLACE_OP_TYPE.equals(jpOpType)) {
                CmPutRequest cmPutRequest = generateCmPutRequest3gppJsonPatch(nbiCrudPatchRequest.getXpath(), jsonPatchObject);
                commonParent = getCommonParent(commonParent, cmPutRequest.getFdn());
                longestSon = getLongestSon(longestSon, cmPutRequest.getFdn());
                requests.add(cmPutRequest);
            } else {
                throw exceptionFactory.createValidationException(CmRestNbiError.UNIMPLEMENTED_USE_CASE, "op="+jpOpType);
            }
        }
        return new CmPatchRequest(commonParent, longestSon, requests);
    }

    private String getCommonParent(final String fdn, final String otherFdn) {
        if (fdn == null || otherFdn == null) {
            return null;
        }
        if (fdn.startsWith(otherFdn) && otherFdn.length() < fdn.length()) {
            return otherFdn;
        } else {
            return fdn;
        }
    }

    private String getLongestSon(final String fdn, final String otherFdn) {
        if (otherFdn == null) {
            return fdn;
        }
        if (fdn == null) {
            return otherFdn;
        }
        return otherFdn.length() > fdn.length() ? otherFdn : fdn;
    }

    private CmCreateRequest generateCmCreateRequest3gppJsonPatch(final String xpath, JsonPatchObject jsonPatchObject ) {

        if (!jsonPatchObject.valueIsAnObject()) {
            throw exceptionFactory.createValidationException(CmRestNbiError.INVALID_JSON_VALUE, "an invalid value="+jsonPatchObject.getValue()+" has been provided for add");
        }

        String fdn = getActualFdnFrom3gppJsonPatch(xpath, jsonPatchObject.getPath());
        logger.debug("generateCmCreateRequest3gppJsonPatch - actual fdn: '{}'", fdn);
        HashMap<String, Object> value = ((HashMap)(jsonPatchObject.getValue()));

        //extract id info
        checkExistingJsonField(value, "id");
        String id = (String)value.get("id");
        if ("null".equals(id)) {
            id = null;
        }

        //extract attributes map
        final Object attributesMapObject = value.get("attributes");
        if (!isValidAttributesMap(attributesMapObject)) {
            throw exceptionFactory.createValidationException(CmRestNbiError.INVALID_JSON_VALUE, "an invalid attributes="+attributesMapObject+" has been provided for add");
        }
        Map<String, Object> attributesMap = (HashMap)(attributesMapObject);

        //extract child and parent info
        String childType = extractTypeFromFdn(fdn);
        return new CmCreateRequest(getParentFdn(fdn), id, childType, attributesMap);
    }

    private CmDeleteRequest generateCmDeleteRequest3gppJsonPatch(final String xpath, JsonPatchObject jsonPatchObject ) {
        String fdn = getActualFdnFrom3gppJsonPatch(xpath, jsonPatchObject.getPath());
        logger.debug("generateCmDeleteRequest3gppJsonPatch - actual fdn: '{}'", fdn);
        return new CmDeleteRequest(fdn, ScopeType.BASE_ALL, 0, null, false);
    }

    private CmPutRequest generateCmPutRequest3gppJsonPatch(final String xpath, JsonPatchObject jsonPatchObject ) {
        String fdn = getActualFdnFrom3gppJsonPatch(xpath, extractPathWithoutAttribute(jsonPatchObject.getPath()));
        logger.debug("generateCmPutRequest3gppJsonPatch - actual fdn: '{}'", fdn);
        Map<String, Object> attributesMap = fillAttributesMap3gppJsonPatch(jsonPatchObject);
        return new CmPutRequest(fdn, extractNameFromFdn(fdn), extractTypeFromFdn(fdn), attributesMap);
    }

    /*
    *           Json Patch
    * */
    public CmPatchRequest generateCmPatchRequestJsonPatch(final NbiCrudPatchRequest nbiCrudPatchRequest) throws IOException {

        if (nbiCrudPatchRequest.getXpath() == null) {
                throw exceptionFactory.createValidationException(CmRestNbiError.INVALID_SYNTAX, "xpath=null");
        }

        List<WriteRequest> requests = new ArrayList<>();
        String commonParent = getFdnFromXpath(nbiCrudPatchRequest.getXpath());
        String longestSon = null;

        logger.debug("CmNbiCheckInputParameters::generateCmPatchRequestJsonPatch - nbiCrudPatchRequest = {}", nbiCrudPatchRequest);
        ObjectMapper mapper = new ObjectMapper();
        JsonPatchObject[] jsonPatchObjects = mapper.readValue(nbiCrudPatchRequest.getBody(), JsonPatchObject[].class);

        for (JsonPatchObject jsonPatchObject : jsonPatchObjects) {
            String jpOpType = jsonPatchObject.getOp();
            if (JSON_PATCH_ADD_OP_TYPE.equals(jpOpType)) {
                CmCreateRequest cmCreateRequest = generateCmCreateRequestJsonPatch(nbiCrudPatchRequest.getXpath(), jsonPatchObject);
                commonParent = getCommonParent(commonParent, cmCreateRequest.getParentFdn());
                longestSon = getLongestSon(longestSon, cmCreateRequest.getParentFdn());
                requests.add(cmCreateRequest);
            } else if (JSON_PATCH_REMOVE_OP_TYPE.equals(jpOpType)) {
                CmDeleteRequest cmDeleteRequest = generateCmDeleteRequestJsonPatch(nbiCrudPatchRequest.getXpath(), jsonPatchObject);
                commonParent = getCommonParent(commonParent, cmDeleteRequest.getFdn());
                longestSon = getLongestSon(longestSon, cmDeleteRequest.getFdn());
                requests.add(cmDeleteRequest);
            } else if (JSON_PATCH_REPLACE_OP_TYPE.equals(jpOpType)) {
                CmPutRequest cmPutRequest = generateCmPutRequestJsonPatch(nbiCrudPatchRequest.getXpath(), jsonPatchObject);
                commonParent = getCommonParent(commonParent, cmPutRequest.getFdn());
                longestSon = getLongestSon(longestSon, cmPutRequest.getFdn());
                requests.add(cmPutRequest);
            } else {
                throw exceptionFactory.createValidationException(CmRestNbiError.UNIMPLEMENTED_USE_CASE, "op="+jpOpType);
            }
        }
        return new CmPatchRequest(commonParent, longestSon, requests);
    }


    private CmCreateRequest generateCmCreateRequestJsonPatch(final String xpath, JsonPatchObject jsonPatchObject ) {

        if (!jsonPatchObject.valueIsAnObject()) {
            throw exceptionFactory.createValidationException(CmRestNbiError.INVALID_JSON_VALUE, "an invalid value="+jsonPatchObject.getValue()+" has been provided for add");
        }

        String fdn = getFdnFromXpath(xpath);

        final String path = jsonPatchObject.getPath();
        if (path != null && !path.equals("")) {
            throw exceptionFactory.createValidationException(CmRestNbiError.INVALID_SYNTAX, "path=" + path + " for op=" + jsonPatchObject.getOp());
        }

        logger.debug("generateCmCreateRequestJsonPatch - actual fdn: '{}'", fdn);
        HashMap<String, Object> value = ((HashMap)(jsonPatchObject.getValue()));

        //extract id info
        checkExistingJsonField(value, "id");
        String id = (String)value.get("id");
        if ("null".equals(id)) {
            id = null;
        }

        //extract attributes map
        final Object attributesMapObject = value.get("attributes");
        if (!isValidAttributesMap(attributesMapObject)) {
            throw exceptionFactory.createValidationException(CmRestNbiError.INVALID_JSON_VALUE, "an invalid attributes="+attributesMapObject+" has been provided for add");
        }
        Map<String, Object> attributesMap = (HashMap)(attributesMapObject);

        //extract child and parent info
        String childType = extractTypeFromFdn(fdn);
        return new CmCreateRequest(getParentFdn(fdn), id, childType, attributesMap);
    }

    private CmDeleteRequest generateCmDeleteRequestJsonPatch(final String xpath, JsonPatchObject jsonPatchObject ) {
        String fdn = getFdnFromXpath(xpath);

        final String path = jsonPatchObject.getPath();
        if (path != null && !path.equals("")) {
            throw exceptionFactory.createValidationException(CmRestNbiError.INVALID_SYNTAX, "path=" + path + " for op=" + jsonPatchObject.getOp());
        }

        logger.debug("generateCmDeleteRequestJsonPatch - actual fdn: '{}'", fdn);
        return new CmDeleteRequest(fdn, ScopeType.BASE_ONLY, 0, null, false);
    }

    private CmPutRequest generateCmPutRequestJsonPatch(final String xpath, JsonPatchObject jsonPatchObject ) {
        String fdn = getFdnFromXpath(xpath);

        logger.debug("generateCmPutRequestJsonPatch - actual fdn: '{}'", fdn);
        Map<String, Object> attributesMap = fillAttributesMapJsonPatch(jsonPatchObject);
        return new CmPutRequest(fdn, extractNameFromFdn(fdn), extractTypeFromFdn(fdn), attributesMap);
    }


    private void checkExistingJsonField(HashMap<String, Object> value, String field) {
        if (value == null || !value.containsKey(field)) {
            throw exceptionFactory.createValidationException(CmRestNbiError.MISSING_JSON_FIELD, field);
        }
    }

    private void checkParentChildMismatch(final String fdn, final String childType, final String id) {
        String fdnString = fdn + "";
        if (!fdnString.endsWith(childType + "=" + id)) {
                logger.debug("checkParentRelationShip:: fdn={}, childType={}, id={}", fdn, childType, id);
                throw exceptionFactory.createValidationException(CmRestNbiError.PUT_CREATE_MISMATCH_BETWEEN_URI_MO_AND_RESOURCE_MO, "{" + fdn +"}","{" + childType + "="+id+"}" );
        }
    }

    private void checkIfIdNull(String id) {
        if (id == null) {
            throw exceptionFactory.createValidationException(CmRestNbiError.INVALID_SYNTAX, "id=null");
        }
    }

    private boolean isValidAttributesMap(final Object attributesMap) {
        return (attributesMap instanceof Map);
    }


    private String getFdnFromXpathForGet(final String xpath, String filter) {
        if (xpath == null && filter != null) {
            return null;
        }
        return getFdnFromXpath(xpath);
    }

    /*
     *           Common Utilility
    *
    */
    public String getFdnFromXpath(final String xpath) {
        if (xpath == null) {
            throw exceptionFactory.createValidationException(CmRestNbiError.INVALID_SYNTAX, "xpath=null");
        }

        String fdn = fromXpathOrPathToFdn(xpath);
        logger.debug("CmNbiCheckInputParameters::getFdnFromXpath:: xpath={} return fdn={}", xpath, fdn);
        return fdn;
    }

    private String getFdnFromPath(String path) {
        if (path == null || path.equals("")) {
            return path;
        }

        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        String fdn = fromXpathOrPathToFdn(path);
        logger.debug("CmNbiCheckInputParameters::getFdnFromPath:: path={} return fdn={}", path, fdn);
        return fdn;
    }

    private boolean isNullFdnFromPath(final String fdnFromPath) {
        return fdnFromPath == null || fdnFromPath.equals("");
    }

    private String checkAndGetAction(final String action) {
        if (action == null || action.equals("")) {
            throw exceptionFactory.createValidationException(CmRestNbiError.MISSING_PARAM, "action");
        }
        return action;
    }

    private String getActualFdnFrom3gppJsonPatch(final String xpath, final String path) {
        final String fdnFromXPath = xpath == null? null : getFdnFromXpath(xpath);
        final String fdnFromPath = getFdnFromPath(path);

        if (fdnFromXPath == null && (isNullFdnFromPath(fdnFromPath))) {
            throw exceptionFactory.createValidationException(CmRestNbiError.INVALID_SYNTAX, "xpath=null and path==" + path);
        }

        if (fdnFromXPath == null) {
            return fdnFromPath;
        }

        if (isNullFdnFromPath(fdnFromPath)) {
            return fdnFromXPath;
        }

        if (fdnFromPath.startsWith(fdnFromXPath)) {
            return fdnFromPath;
        }
        return fdnFromXPath + ELEMENTS_SEPARATOR + fdnFromPath;
    }


    private ScopeType getScopeType(final String value) {
        ScopeType scopeType = ScopeType.BASE_ONLY;

        try {
            if (value != null) {
                scopeType = ScopeType.valueOf(value);
            }
        } catch (Exception e) {
            logger.debug("CmNbiCheckInputParameters::getScopeType:: invalid scopeType={}, e.getMessage()={}", scopeType, e.getMessage());
            throw exceptionFactory.createValidationException(CmRestNbiError.INVALID_SYNTAX, "scopeType=" +value);
        }
        logger.debug("CmNbiCheckInputParameters::getScopeType:: return scopeType={}", scopeType);
        return scopeType;
    }

    private int getScopeLevel(final int value) {
        if (value < 0) {
            throw exceptionFactory.createValidationException(CmRestNbiError.INVALID_SYNTAX, "scopeLevel=" +value);
        }
        return value;
    }

    private String getFilter(final String value) {
        return value == null ? value : value.trim();
    }

    @SuppressWarnings({"squid:S3776"})
    private Map<String, Set<String>> getAttributesAndFields(final String attributes, final String fields) {
        Map<String, Set<String>> attributesAndFields = null;
        if (attributes != null) {
            attributesAndFields = new HashMap<>();
            if (!attributes.isEmpty()) {
                String[] tokens = attributes.split(ELEMENTS_SEPARATOR);
                for (String token:tokens) {
                    attributesAndFields.put(token.trim(), new HashSet<>());
                }
            }
        }

        if (fields != null) {
            if (attributesAndFields == null) {
                attributesAndFields = new HashMap<>();
            }
            if (!fields.isEmpty()) {
                String[] tokens = fields.split(ELEMENTS_SEPARATOR);
                for (String token:tokens) {

                    if (!token.contains(ATTRIBUTES + SLASH)) {
                        throw exceptionFactory.createValidationException(CmRestNbiError.INVALID_SYNTAX, "fields=" +fields);
                    }

                    token = token.replaceAll(ATTRIBUTES + SLASH,"");
                    if (!token.contains(SLASH)) {
                        attributesAndFields.put(token.trim(), new HashSet<>());
                    } else {
                        String[] subtokens = token.split(SLASH);
                        String subtoken0 = subtokens[0].trim();
                        String subtoken1 = subtokens[1].trim();

                        if (!attributesAndFields.containsKey(subtoken0)) {
                            attributesAndFields.put(subtoken0, new HashSet<>());
                        }
                        attributesAndFields.get(subtoken0).add(subtoken1);
                    }
                }
            }
        }
        logger.debug("CmNbiCheckInputParameters::getAttributesAndFields:: return attributesAndFields={}", attributesAndFields);
        return attributesAndFields;
    }

    //for 3gpp Json Patch we accept both #/attributes/attr and #attributes/attr
    private String extractAttribute3gppJsonPatch(final String path) {
        if (path == null || path.equals("")) {
            return null;
        }

        if (path.contains(ATTRIBUTES_IN_3GPP_JSON_PATCH1)) {
            String[] tokens = path.split(ATTRIBUTES_IN_3GPP_JSON_PATCH1);
            return tokens.length>1 ? tokens[1] : null;
        }
        if (path.contains(ATTRIBUTES_IN_3GPP_JSON_PATCH2)) {
            String[] tokens = path.split(ATTRIBUTES_IN_3GPP_JSON_PATCH2);
            return tokens.length>1 ? tokens[1] : null;
        }
        return null;
    }

    private String extractPathWithoutAttribute(final String path) {
        if (path == null || path.equals("")) {
            return path;
        }

        if (path.contains(ATTRIBUTES_IN_3GPP_JSON_PATCH1)) {
            String[] tokens = path.split(ATTRIBUTES_IN_3GPP_JSON_PATCH1);
            return tokens.length>0 ? tokens[0] : path;
        }
        if (path.contains(ATTRIBUTES_IN_3GPP_JSON_PATCH2)) {
            String[] tokens = path.split(ATTRIBUTES_IN_3GPP_JSON_PATCH2);
            return tokens.length>0 ? tokens[0] : path;
        }

        return path;
    }

    private boolean isComplexAttributeName(final String attributeName) {
        return attributeName.contains(SLASH);
    }

    private Map<String, Object> fillAttributesMap3gppJsonPatch(JsonPatchObject jsonPatchObject) {
        Map<String, Object> attributesMap = new HashMap<>();
        String attributeName = extractAttribute3gppJsonPatch(jsonPatchObject.getPath());
        if (attributeName != null) {
            if (isComplexAttributeName(attributeName)) {
                final String[] tokens = attributeName.split(SLASH);
                final String complexAttributeName = tokens[0];
                final String complexAttributeMember = tokens[1];

                MapWithOnlyOneComplexMember mapWithOnlyOneComplexMember = new MapWithOnlyOneComplexMember(complexAttributeMember, jsonPatchObject.getValue());
                attributesMap.put(complexAttributeName, mapWithOnlyOneComplexMember);
            } else {
                attributesMap.put(attributeName, jsonPatchObject.getValue());
            }
        } else {
            throw exceptionFactory.createValidationException(CmRestNbiError.INVALID_SYNTAX, "path=" +jsonPatchObject.getPath() + " for op="+ jsonPatchObject.getOp());
        }
        return attributesMap;
    }

    //for Json Patch we accept both /attributes/attr and attributes/attr
    private String extractAttributeJsonPatch(final String path) {
        if (path == null || path.equals("")) {
            return null;
        }

        if (path.startsWith(ATTRIBUTES_IN_JSON_PATCH1)) {
            String[] tokens = path.split(ATTRIBUTES_IN_JSON_PATCH1);
            return tokens.length>1 ? tokens[1] : null;
        }
        if (path.startsWith(ATTRIBUTES_IN_JSON_PATCH2)) {
            String[] tokens = path.split(ATTRIBUTES_IN_JSON_PATCH2);
            return tokens.length>1 ? tokens[1] : null;
        }
        return null;
    }

    private Map<String, Object> fillAttributesMapJsonPatch(JsonPatchObject jsonPatchObject) {
        Map<String, Object> attributesMap = new HashMap<>();
        String attributeName = extractAttributeJsonPatch(jsonPatchObject.getPath());
        if (attributeName != null) {
            if (isComplexAttributeName(attributeName)) {
                final String[] tokens = attributeName.split(SLASH);
                final String complexAttributeName = tokens[0];
                final String complexAttributeMember = tokens[1];

                MapWithOnlyOneComplexMember mapWithOnlyOneComplexMember = new MapWithOnlyOneComplexMember(complexAttributeMember, jsonPatchObject.getValue());
                attributesMap.put(complexAttributeName, mapWithOnlyOneComplexMember);
            } else {
                attributesMap.put(attributeName, jsonPatchObject.getValue());
            }
        } else {
            throw exceptionFactory.createValidationException(CmRestNbiError.INVALID_SYNTAX, "path=" +jsonPatchObject.getPath() + " for op="+ jsonPatchObject.getOp());
        }
        return attributesMap;
    }

    /*
    * This method recover fdn from xpath replacing '/' with ',' (but managing FDN_ID containing /)
    * Example: MeContext=SPFRER60001,ManagedElement=1,interfaces=1,interface=1/2
    *
    * This simple algorithm replaces last '/' with ',' for every token (except the last one)
    *
    * */
    private String fromXpathOrPathToFdn(final String xpath) {
        if (xpath == null) {
            return null;
        }

        StringBuilder fdnBuilder = new StringBuilder();
        String[] tokens = xpath.split(EQUAL_SEPARATOR);
        int size = tokens.length;
        for (int i=0; i<=size-2; i++) {
            final String token = tokens[i];
            String modifiedToken = replaceLast(token, SLASH, ELEMENTS_SEPARATOR);
            fdnBuilder.append(modifiedToken);
            fdnBuilder.append(EQUAL_SEPARATOR);
        }

        fdnBuilder.append(tokens[size-1]);
        return fdnBuilder.toString();
    }

    private String replaceLast(String string, String substring, String replacement)
    {
        int index = string.lastIndexOf(substring);
        if (index == -1)
            return string;
        return string.substring(0, index) + replacement
                + string.substring(index+substring.length());
    }

}
