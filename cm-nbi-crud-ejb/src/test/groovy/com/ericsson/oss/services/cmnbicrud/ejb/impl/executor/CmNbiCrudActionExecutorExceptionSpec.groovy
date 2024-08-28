package com.ericsson.oss.services.cmnbicrud.ejb.impl.executor

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudActionRequest
import com.ericsson.oss.presentation.cmnbirest.api.NbiResponse
import com.ericsson.oss.services.cm.cmreader.api.CmReaderService
import com.ericsson.oss.services.cm.cmwriter.api.CmWriterService
import spock.lang.Unroll

import javax.inject.Inject

class CmNbiCrudActionExecutorExceptionSpec extends CdiSpecification {
    @ObjectUnderTest
    private CmNbiCrudActionExecutor objUnderTest

   @Inject
   CmWriterService cmWriterService;

    @Inject
    CmReaderService cmReaderService;

    String userId = "user"
    String requestId = "crud:1234"
    String ipAddress = "1.2.3.5"
    String ssoToken = "CookieAABBCCDDEE";

    @MockedImplementation
    CmNbiCrudPatchExecutorBean mockedCmNbiCrudPatchExecutorBean;

    @MockedImplementation
    CmNbiCrudPatchExecutor mockedCmNbiCrudPatchExecutor;

    def setup() {
        cmReaderService.existsFdn(_) >> true
        cmWriterService.validateFdnAgainstTbac(_) >> true
    }

    @Unroll
    def 'Execute nbiCrudPatchActionRequest when cmWriterService throws Exception return fail'() {
        given: 'NbiCrudPatchActionRequest'
        NbiCrudActionRequest nbiCrudActionRequest = new NbiCrudActionRequest(userId, requestId, ipAddress, ssoToken, xpath, action, body)

        and:'database available'
        cmReaderService.isDatabaseAvailable() >> true

        and:
        cmWriterService.performAction(*_) >> {throw new Exception("error message")}

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudActionRequest)

        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 405
            nbiResponse.jsonContent.contains('{"error":{"errorInfo":')
            nbiResponse.jsonContent.contains("error message")
        }

        where:
        xpath                                                       | action                | body
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1" | "updateMMEConnection" | '{ "input": { "orderCode":"RENEW_ALL_EXISTING" } }'
    }

    @Unroll
    def 'Execute nbiCrudPatchActionRequest when database not available return fail'() {
        given: 'nbiCrudPatchActionRequest'
        NbiCrudActionRequest nbiCrudActionRequest = new NbiCrudActionRequest(userId, requestId, ipAddress, ssoToken, xpath, action, body)

        and:'database not available'
        cmReaderService.isDatabaseAvailable() >> false

        and:
        mockedCmNbiCrudPatchExecutorBean.executor(_) >> {throw new Exception("error message")}

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudActionRequest)

        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 503
            nbiResponse.jsonContent == '{"error":{"errorInfo":"Service is currently unavailable."}}'
        }

        where:
        xpath                                                       | action                | body
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1" | "updateMMEConnection" | '{ "input": { "orderCode":"RENEW_ALL_EXISTING" } }'
    }
}