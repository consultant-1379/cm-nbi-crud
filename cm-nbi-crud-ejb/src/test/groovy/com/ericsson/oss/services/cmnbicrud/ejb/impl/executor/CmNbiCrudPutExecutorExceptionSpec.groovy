package com.ericsson.oss.services.cmnbicrud.ejb.impl.executor

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudPostRequest
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudPutRequest
import com.ericsson.oss.presentation.cmnbirest.api.NbiResponse
import com.ericsson.oss.presentation.cmnbirest.api.OperationType
import com.ericsson.oss.services.cm.cmreader.api.CmReaderService
import com.ericsson.oss.services.cm.cmsearch.DynamicBlackListInfoIf
import com.ericsson.oss.services.cm.cmshared.dto.CmResponse
import com.ericsson.oss.services.cm.cmwriter.api.CmWriterService
import com.ericsson.oss.services.cmnbicrud.ejb.common.ResponseType
import com.ericsson.oss.services.cmnbicrud.ejb.exceptions.CmRestNbiValidationException
import com.ericsson.oss.services.cmnbicrud.ejb.put.CmPutRequest
import com.ericsson.oss.services.cmnbicrud.ejb.put.CmPutResponse
import com.ericsson.oss.services.cmnbicrud.ejb.testutil.TestUtil
import spock.lang.Unroll

import javax.inject.Inject

class CmNbiCrudPutExecutorExceptionSpec extends CdiSpecification {
    @ObjectUnderTest
    private CmNbiCrudPutExecutor objUnderTest

   @Inject
   CmWriterService cmWriterService;

    @Inject
    CmReaderService cmReaderService;

    String userId = "user"
    String requestId = "crud:1234"
    String ipAddress = "1.2.3.5"
    String ssoToken = "CookieAABBCCDDEE";

    def setup() {
        cmWriterService.validateFdnAgainstTbac(_) >> true
    }

    /*
    *                 PUT COMMAND REQUEST
    *
    * */

    def 'Execute nbiCrudPutRequest when cmWriterService throws Exception return fail'() {
        given: 'nbiCrudPutRequest'
        def xpath = 'MeContext=ERBS001FORNBI,ManagedElement=1'
        def body = '{ "ManagedElement": [{ "id": "1", "attributes": {"logicalName": "newLogicalName", "site": "World"} }] }'

        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        and:'database available'
        cmReaderService.isDatabaseAvailable() >> true
        cmReaderService.existsFdn(_) >> true

        and:
        cmWriterService.setManagedObjectForNbiPUT(*_) >> {throw new Exception("error message")}
        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPutRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 405
            nbiResponse.jsonContent == '{"error":{"errorInfo":"Nbi Rest Execution Error (error message)"}}'
        }
    }

    def 'Execute nbiCrudPutRequest when database not available return fail'() {
        given: 'nbiCrudPutRequest'
        def xpath = 'MeContext=ERBS001FORNBI,ManagedElement=1'
        def body = '{ "ManagedElement": [{ "id": "1", "attributes": {"logicalName": "newLogicalName", "site": "World"} }] }'

        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        and:'database not available'
        cmReaderService.isDatabaseAvailable() >> false
        cmReaderService.existsFdn(_) >> true

        and:
        cmWriterService.setManagedObjectForNbiPUT(*_) >> {throw new Exception("error message")}
        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPutRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 503
            nbiResponse.jsonContent == '{"error":{"errorInfo":"Service is currently unavailable."}}'
        }
    }

    def 'Execute nbiCrudPutRequest when cmWriterService return 0 instance fail'() {
        given: 'nbiCrudPutRequest'
        def xpath = 'MeContext=ERBS001FORNBI,ManagedElement=1'
        def body = '{ "ManagedElement": [{ "id": "1", "attributes": {"logicalName": "newLogicalName", "site": "World"} }] }'

        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        and:'database available'
        cmReaderService.isDatabaseAvailable() >> true
        cmReaderService.existsFdn(_) >> true

        and:
        cmWriterService.setManagedObjectForNbiPUT(*_)  >> new CmResponse()
        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPutRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 404
            nbiResponse.jsonContent == '{"error":{"errorInfo":"NOT FOUND MOs matching conditions"}}'
        }
    }

    /*
    *                 PUT COMMAND REQUEST - FROM PATCH
    *
    * */

    @Unroll
    def 'Execute cmPutRequest (From Patch) when cmWriterService throws Exception/CmRestNbiValidationException return fail'() {
        given: 'cmPutRequest'
        def fdn = 'MeContext=ERBS001FORNBI,ManagedElement=1'
        def id = "1"
        def type = "ManagedElement"

        Map<String, Object> attributes = new HashMap<>();
        attributes.put( "logicalName", "newLogicalName");
        attributes.put( "site", "World");

        CmPutRequest cmPutRequest = new CmPutRequest(fdn, id, type, attributes);

        and:'database available'
        cmReaderService.isDatabaseAvailable() >> true
        cmReaderService.existsFdn(_) >> true

        and:
        cmWriterService.setManagedObjectForNbiPATCH(*_)  >> {throw exception}
        when:
        CmPutResponse cmPutResponse = objUnderTest.executor(cmPutRequest);

        then:
        with(cmPutResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            moObjects == null
            errorResponseType.error.errorInfo.contains("error message")
            httpCode == 405
        }

        where:
        exception                                                    | _
        new Exception("error message")                               | _
        new CmRestNbiValidationException("error message", 405)       | _
    }

    @Unroll
    def 'Execute isAnUpdateOperation'() {
        given: 'nbiCrudPutRequest'

        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , null)

        and:'database not available'
        cmReaderService.isDatabaseAvailable() >> dataBaseAvailable
        cmReaderService.existsFdn(xpath) >> existFdn
        when:
        boolean result = objUnderTest.isAnUpdateOperation(nbiCrudPutRequest)
        then:
            result == expectedResult
        where:
        dataBaseAvailable   |   existFdn    |    xpath      || expectedResult
        true                |   true        |   "A=B"       ||  true
        true                |   true        |   null        ||  false
        true                |   false       ||  "A=B"       || false
        false               |   true        ||  "A=B"       || false
        false               |   false       ||  "A=B"       || false

    }
}