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

package com.ericsson.oss.services.cmnbicrud.ejb.common;

import static com.ericsson.oss.services.cmnbicrud.ejb.common.CmRestNbiConstants.*;


public enum CmRestNbiError {

    /*
      S E R V E R - E R R O R
     */
    UNEXPECTED_ERROR( "This is an unhandled system error, please check the error log for more details", HTTP_METHOD_NOT_ALLOWED),
    SERVER_ERROR("{0}", HTTP_EXPECTATION_FAILED),
    //this should happen only with get + filter
    NOT_FOUND_MOS  ("NOT FOUND MOs matching conditions", HTTP_NOT_FOUND),

    /*
     V A L I D A T I O N - E R R O R S
     */

    //DB or access validation
    DATABASE_NOT_AVAILABLE( "Service is currently unavailable.", HTTP_SERVICE_UNAVAILABLE),
    FDN_NOT_FOUND("The supplied FDN {0} does not exist in the database", HTTP_NOT_FOUND),
    TBAC_ACCESS_DENIED("Access Denied. Your Target Based Access Control rights do not allow you to perform actions on the Network Element(s).", HTTP_UNAUTHORIZED),
    ACCESS_DENIED("Insufficient access rights to perform the operation", HTTP_UNAUTHORIZED),

    // unimplemented use cases
    UNIMPLEMENTED_USE_CASE ( "Unimplemented Use Case for {0}", HTTP_UNPROCESSABLE_ENTITY),
    OVERALL_PROTECTION( "Request not supported{0}", HTTP_UNPROCESSABLE_ENTITY),

    //filter management
    INVALID_FILTER_FOR_FDN ("Invalid filter={0} for fdn={1}", HTTP_UNPROCESSABLE_ENTITY),
    TOO_MANY_SLASH_IN_FILTER("Invalid filter without attributes={0}. It should contain only / or //", HTTP_UNPROCESSABLE_ENTITY),
    OR_CLAUSE_IN_FILTER( "Invalid filter={0}. It should not contain |", HTTP_UNPROCESSABLE_ENTITY),

    //CRUD input validation
    INVALID_SYNTAX ("The supplied parameters {0} is not allowed.", HTTP_BAD_REQUEST),
    INVALID_JSON ("Invalid json in body: ", HTTP_BAD_REQUEST),
    INVALID_JSON_VALUE ( "Not allowed json value: {0} ", HTTP_BAD_REQUEST),
    MISSING_JSON_FIELD("Missing json field: {0} ", HTTP_BAD_REQUEST),
    INVALID_SCOPE_TYPE_FOR_DELETE ( "Invalid scopeType={0} for DELETE operation", HTTP_BAD_REQUEST),
    INVALID_SCOPE_TYPE_TO_DELETE_A_SUBNETWORK ( "Invalid scopeType={0} to DELETE a SubNetwork. Valid value is BASE ONLY", HTTP_BAD_REQUEST),
    FILTER_NOT_ALLOWED_WITH_SCOPE_TYPE_IN_DELETE ( "Filter not allowed with scopeType={0}.", HTTP_BAD_REQUEST),
    FDN_HAS_CHIDREN ( "Invalid scopeType={0} for fdn={1} in DELETE operation because there are children", HTTP_BAD_REQUEST),
    PATCH_FDN_HAS_CHILDREN ("Invalid \"remove\" operation : fdn={0} has children", HTTP_BAD_REQUEST),
    /* Specific for PUT (CREATE)*/
    PUT_CREATE_MISMATCH_BETWEEN_URI_MO_AND_RESOURCE_MO("Mismatch between URI Mo={0} and Resource Mo={1}", HTTP_BAD_REQUEST),
    /* Specific for PATCH (ACTION)*/
    MISSING_PARAM ("The required parameter: '{0}' is missing.", HTTP_BAD_REQUEST);

    private final int httpcode;
    private final String message;

    CmRestNbiError(final String message, final int httpcode) {
        this.message = message;
        this.httpcode = httpcode;
    }

    public String getMessage() {
        return message;
    }

    public int getHttpcode() { return httpcode; }
}
