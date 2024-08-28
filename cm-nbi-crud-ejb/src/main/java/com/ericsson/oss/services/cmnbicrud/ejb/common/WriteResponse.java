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

import com.ericsson.oss.services.cm.cmshared.dto.CmObject;
import com.ericsson.oss.services.cmnbicrud.spi.output.ErrorResponseType;
import com.ericsson.oss.services.cmnbicrud.spi.output.MoObjects;

import java.io.Serializable;
import java.util.Collection;

/**
 * Created by enmadmin on 11/9/21.
 */
public interface WriteResponse extends Serializable {
    public boolean isErrored();
    public ErrorResponseType getErrorResponseType();
    public MoObjects getMoObjects();
    public Collection<CmObject> getCmObjects();
    public int getHttpCode();
}
