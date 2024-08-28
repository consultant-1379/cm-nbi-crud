/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.cmnbicrud.ejb.common;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class CmRestNbiConstants {

    /*
    *    Fail Http Codes
    * */

    public static final int HTTP_UNAUTHORIZED = 401;
    public static final int HTTP_NOT_FOUND = 404;
    public static final int HTTP_BAD_REQUEST = 400;
    public static final int HTTP_METHOD_NOT_ALLOWED = 405;
    public static final int HTTP_UNPROCESSABLE_ENTITY = 422;
    public static final int HTTP_EXPECTATION_FAILED = 417;
    public static final int HTTP_SERVICE_UNAVAILABLE = 503;

    /*
    *    Success Http Codes
    * */
    public static final int GET_SUCCESS_HTTP_CODE = 200;

    public static final int POST_AND_PUT_CREATE_SUCCESS_HTTP_CODE = 201;

    public static final int PUT_MODIFY_SUCCESS_HTTP_CODE = 200;
    public static final int PUT_MODIFY_SUCCESS_HTTP_CODE_NO_CONTENT = 204;

    public static final int DELETE_SUCCESS_HTTP_CODE = 200;
    public static final int DELETE_SUCCESS_HTTP_CODE_NO_QUERY_PARAMETERS = 204;

    public static final int PATCH_SUCCESS_HTTP_CODE = 200;
    public static final int PATCH_SUCCESS_HTTP_CODE_NO_CONTENT = 204;  //EMARDEP not used now

    public static final int ACTION_SUCCESS_HTTP_CODE = 200;
    public static final int ACTION_SUCCESS_HTTP_CODE_NO_CONTENT = 204;

    /*
    *    Success Http Codes for command logging purpose
    * */
    public static final Set<Integer> GET_SUCCESS_HTTP_CODES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(GET_SUCCESS_HTTP_CODE)));
    public static final Set<Integer> POST_SUCCESS_HTTP_CODES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(POST_AND_PUT_CREATE_SUCCESS_HTTP_CODE)));
    public static final Set<Integer> PUT_CREATE_SUCCESS_HTTP_CODES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(POST_AND_PUT_CREATE_SUCCESS_HTTP_CODE)));
    public static final Set<Integer> PUT_MODIFY_SUCCESS_HTTP_CODES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(PUT_MODIFY_SUCCESS_HTTP_CODE, PUT_MODIFY_SUCCESS_HTTP_CODE_NO_CONTENT)));
    public static final Set<Integer> DELETE_SUCCESS_HTTP_CODES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(DELETE_SUCCESS_HTTP_CODE, DELETE_SUCCESS_HTTP_CODE_NO_QUERY_PARAMETERS)));
    public static final Set<Integer> PATCH_SUCCESS_HTTP_CODES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(PATCH_SUCCESS_HTTP_CODE, PATCH_SUCCESS_HTTP_CODE_NO_CONTENT)));
    public static final Set<Integer> ACTION_SUCCESS_HTTP_CODES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(ACTION_SUCCESS_HTTP_CODE, ACTION_SUCCESS_HTTP_CODE_NO_CONTENT)));
    public static final Set<Integer> UNKNOWN_SUCCESS_HTTP_CODES = Collections.unmodifiableSet(new HashSet<>());

    /*
    *    RBAC constants
    * */
    public static final String RBAC_RESOURCE_CM_NBI_CRUD = "cm_nbi_crud";
    public static final String RBAC_ACTION_CREATE = "create";
    public static final String RBAC_ACTION_DELETE = "delete";
    public static final String RBAC_ACTION_READ = "read";
    public static final String RBAC_ACTION_UPDATE = "update";

    /*
    *    JJson-Patch operation types
    * */
    public static final String JSON_PATCH_ADD_OP_TYPE = "add";
    public static final String JSON_PATCH_REMOVE_OP_TYPE = "remove";
    public static final String JSON_PATCH_REPLACE_OP_TYPE = "replace";
    public static final String JSON_PATCH_MOVE_OP_TYPE = "move";      // ha senso per noi???
    public static final String JSON_PATCH_COPY_OP_TYPE = "copy";      // idem???

    /*
    *    Date and Time constants
    * */
    // We force objectMapper.setDateFormat so that jackson convert Date in string with this format that is used in AttibuteValidationServiceImpl. This works cause this format produces same output as "EEE MMM dd HH:mm:ss zzz yyyy" that is default format used in Date().toString() implementation.
    public static final String SIMPLE_DATE_FORMAT_STRING = "EE MMM dd HH:mm:ss z yyyy";

    protected CmRestNbiConstants() {}
}
