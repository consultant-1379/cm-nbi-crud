package com.ericsson.oss.services.cmnbicrud.ejb.impl.executor

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudGetRequest
import com.ericsson.oss.presentation.cmnbirest.api.NbiResponse
import com.ericsson.oss.services.cm.cmreader.api.CmReaderService
import com.ericsson.oss.services.cm.cmsearch.DynamicBlackListInfoIf
import com.ericsson.oss.services.cmnbicrud.ejb.testutil.TestUtil
import javax.inject.Inject

class CmNbiCrudGetExecutorExceptionSpec extends CdiSpecification {
    @ObjectUnderTest
    private CmNbiCrudGetExecutor objUnderTest

   @Inject
   CmReaderService cmReaderService;

    @Inject
    private TestUtil testUtil

    @Inject
    DynamicBlackListInfoIf mockedDynamicBlackListInfoIf;

    String userId = "user"
    String requestId = "crud:1234"
    String fields = null
    String attributes = null
    String filter = null
    String ipAddress = "1.2.3.5"
    String ssoToken = "CookieAABBCCDDEE";

    def setup() {
        cmReaderService.existsFdn(_) >> true
    }

    def 'Execute nbiCrudGetRequest when cmReaderService throws Exception return fail'() {
        given: 'nbiCrudGetRequest'
            NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , "BASE_ONLY", fields, attributes, 0, filter)
        and:'database available'
            cmReaderService.isDatabaseAvailable() >> true

        and:
            cmReaderService.getMoByFdnForCmRestNbi(*_) >> {throw new Exception("error message")}
        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudGetRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 405
            nbiResponse.jsonContent == '{"error":{"errorInfo":"Nbi Rest Execution Error (error message)"}}'
        }
    }

    def 'Execute nbiCrudGetRequest when database not available return fail'() {
        given: 'nbiCrudGetRequest'
            NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , "BASE_ONLY", fields, attributes, 0, filter)
        and:'database not available'
            cmReaderService.isDatabaseAvailable() >> false

        when:
            NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudGetRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 503
            nbiResponse.jsonContent == '{"error":{"errorInfo":"Service is currently unavailable."}}'
        }
    }
}
