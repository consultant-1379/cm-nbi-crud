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
package com.ericsson.oss.services.cmnbicrud.ejb.dao;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.cm.cmshared.dto.ActionSpecification;
import com.ericsson.oss.services.cm.cmshared.dto.CmObject;
import com.ericsson.oss.services.cm.cmshared.dto.CmObjectSpecification;
import com.ericsson.oss.services.cm.cmshared.dto.CmResponse;
import com.ericsson.oss.services.cm.cmwriter.api.CmWriterService;
import com.ericsson.oss.services.cmnbicrud.ejb.common.CmRestNbiError;
import com.ericsson.oss.services.cmnbicrud.ejb.common.ExceptionFactory;

import javax.inject.Inject;
import java.util.Collection;

/**
 * Created by enmadmin on 10/27/21.
 */
public class CmWriterDao {

    @Inject
    ExceptionFactory exceptionFactory;

    @EServiceRef
    private CmWriterService cmWriterService;

    public void checkIfValidateFdnAgainstTbac(final String fdn) {
        if ((fdn != null) && !cmWriterService.validateFdnAgainstTbac(fdn)) {
            throw exceptionFactory.createValidationException(CmRestNbiError.TBAC_ACCESS_DENIED);
        }
    }

    /*
    *  CmNbiCrudCreateExecutor
    *
    * */
    public CmResponse createObjectForNbi(String parentFdn, CmObjectSpecification cmObjectSpecification) {
        return cmWriterService.createObjectForNbi(parentFdn, cmObjectSpecification);
    }

    /*
    *  CmNbiCrudPutExecutor
    *
    * */
    public CmResponse setManagedObjectForNbiPATCH(final String targetFdn, final CmObjectSpecification cmObjectSpecification) {
        return  cmWriterService.setManagedObjectForNbiPATCH(targetFdn, cmObjectSpecification);
    }

    public CmResponse setManagedObjectForNbiPUT(final String targetFdn, final CmObjectSpecification cmObjectSpecification) {
        return  cmWriterService.setManagedObjectForNbiPUT(targetFdn, cmObjectSpecification);
    }

    /*
    *  CmNbiCrudDeleteExecutor
    *
    * */
    public void deleteObjectsForNbi(final Collection<CmObject> objectsToBeDeleted) {
        cmWriterService.deleteObjectsForNbi(objectsToBeDeleted);
    }

    public void deleteObjectsForNbiPatch(final String fdn) {
        cmWriterService.deleteObjectsForNbiPatch(fdn);
    }

    /*
     *  CmNbiCrudActionExecutor
     *
     * */
    public CmResponse performActionOnFdn(String targetFdn, ActionSpecification actionSpecification) {
        return cmWriterService.performAction(targetFdn, actionSpecification);
    }
}
