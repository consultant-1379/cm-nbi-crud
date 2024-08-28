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
import com.ericsson.oss.services.cm.cmreader.api.CmReaderService;
import com.ericsson.oss.services.cm.cmshared.dto.CmOutputSpecification;
import com.ericsson.oss.services.cm.cmshared.dto.CmResponse;
import com.ericsson.oss.services.cm.cmshared.dto.search.CmSearchCriteria;
import com.ericsson.oss.services.cmnbicrud.ejb.common.CmRestNbiError;
import com.ericsson.oss.services.cmnbicrud.ejb.common.ExceptionFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by enmadmin on 10/27/21.
 */
public class CmReaderDao {

    @Inject
    ExceptionFactory exceptionFactory;

    @EServiceRef
    private CmReaderService cmReaderService;

    public void checkIfDatabaseIsAvailable() {
        if (!cmReaderService.isDatabaseAvailable()) {
            throw exceptionFactory.createValidationException(CmRestNbiError.DATABASE_NOT_AVAILABLE);
        }
    }

    public void checkIfExistsFdn(final String fdn) {
        if ((fdn != null) && !cmReaderService.existsFdn(fdn)) {
            throw exceptionFactory.createValidationException(CmRestNbiError.FDN_NOT_FOUND, fdn);
        }
    }

    public boolean checkIfExistsFdnWithoutException(final String fdn) {
        return cmReaderService.isDatabaseAvailable() && cmReaderService.existsFdn(fdn);
    }
    
    /*
    *  CmNbiCrudGetExecutor / CmNbiCrudDeleteExecutor
    *
    * */
    public CmResponse getMoByFdnForCmRestNbi(String fdn, Map<String, Set<String>> attributes) {
        return  cmReaderService.getMoByFdnForCmRestNbi(fdn, attributes);
    }

    public CmResponse search(CmSearchCriteria cmSearchCriteria, List<CmOutputSpecification> cmOutputSpecifications) {
        return cmReaderService.searchForNbi(cmSearchCriteria, cmOutputSpecifications);
    }

    public CmResponse  getAllHierarchyWithBaseAll(final String fdn, Map<String, Set<String>> attributes) {
        return cmReaderService.getAllHierarchyWithBaseAll(fdn, attributes);
    }

    public CmResponse  getAllHierarchyWithBaseSubTree(final String fdn, Map<String, Set<String>> attributes, final int distance) {
        return cmReaderService.getAllHierarchyWithBaseSubTree(fdn, attributes, distance);
    }

    public CmResponse  getAllHierarchyWithBaseNthLevel(final String fdn, Map<String, Set<String>> attributes, final int distance) {
        return cmReaderService.getAllHierarchyWithBaseNthLevel(fdn, attributes, distance);
    }

    public Set<String> getSensitiveAttributeNamesGivenFdn(final String fdn) {
        return cmReaderService.getSensitiveAttributeNamesGivenFdn(fdn);
    }

    public Set<String> getSensitiveAttributeNamesGivenParentFdn(final String parentFdn, final String childMoType) {
        return cmReaderService.getSensitiveAttributeNamesGivenParentFdn(parentFdn, childMoType);
    }

    public Set<String> getSensitiveActionParameterNamesGivenFdn(final String fdn, final String actionName) {
        return cmReaderService.getSensitiveActionParameterNamesGivenFdn(fdn, actionName);
    }
}
