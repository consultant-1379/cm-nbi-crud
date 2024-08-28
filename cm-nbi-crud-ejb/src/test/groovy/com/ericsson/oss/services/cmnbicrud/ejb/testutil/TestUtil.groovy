package com.ericsson.oss.services.cmnbicrud.ejb.testutil

import javax.inject.Inject

import com.ericsson.oss.services.cm.cmparser.dto.ParserResult
import com.ericsson.oss.services.cm.cmparser.impl.CmParserServiceBean
import com.ericsson.oss.services.cm.cmshared.dto.search.CmSearchCriteria

/**
 * Basic utilities for test classes.
 */
class TestUtil {

    @Inject
    private CmParserServiceBean cmParserServiceBean

    /**
     * Uses CmParser to parse the command string and update the CmSearchCriteria passed into this method.
     *
     * @param command - the command to parse.
     * @param cmSearchCriteria - the search criteria to be updated with the ParserResult.
     * @return ParserResult
     */
    ParserResult parseCommandAndSetupCmSearchCriteria(final String command, final CmSearchCriteria cmSearchCriteria) {
        ParserResult parserResult = cmParserServiceBean.parseCommand(command)
        cmSearchCriteria.setCmSearchScopes(parserResult.getCmScopes())
        cmSearchCriteria.setCmObjectSpecifications(parserResult.getCmObjectSpecifications())
        parserResult
    }
}

