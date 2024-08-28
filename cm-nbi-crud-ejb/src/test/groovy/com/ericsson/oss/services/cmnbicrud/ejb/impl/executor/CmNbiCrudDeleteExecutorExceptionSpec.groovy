package com.ericsson.oss.services.cmnbicrud.ejb.impl.executor

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudDeleteRequest
import com.ericsson.oss.presentation.cmnbirest.api.NbiResponse
import com.ericsson.oss.services.cm.cmreader.api.CmReaderService
import com.ericsson.oss.services.cm.cmsearch.DynamicBlackListInfoIf
import com.ericsson.oss.services.cm.cmshared.dto.CmResponse
import com.ericsson.oss.services.cm.cmwriter.api.CmWriterService
import com.ericsson.oss.services.cmnbicrud.ejb.common.ResponseType
import com.ericsson.oss.services.cmnbicrud.ejb.common.ScopeType
import com.ericsson.oss.services.cmnbicrud.ejb.delete.CmDeleteRequest
import com.ericsson.oss.services.cmnbicrud.ejb.delete.CmDeleteResponse
import com.ericsson.oss.services.cmnbicrud.ejb.exceptions.CmRestNbiValidationException
import com.ericsson.oss.services.cmnbicrud.ejb.testutil.TestUtil
import spock.lang.Unroll

import javax.inject.Inject

class CmNbiCrudDeleteExecutorExceptionSpec extends CdiSpecification {
    @ObjectUnderTest
    private CmNbiCrudDeleteExecutor objUnderTest

    @Inject
    CmWriterService cmWriterService;

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

    def mockedCmResponse = Mock(CmResponse.class)

    def setup() {
        cmReaderService.existsFdn(_) >> true
        cmWriterService.validateFdnAgainstTbac(_) >> true
    }

    /*
     *                 DELETE COMMAND REQUEST
     *
     * */

    def 'Execute nbiCrudDeleteRequest when cmReaderService throws Exception return fail'() {
        given: 'nbiCrudDeleteRequest'
            NbiCrudDeleteRequest nbiCrudDeleteRequest = new NbiCrudDeleteRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , "BASE_ONLY", 0, filter)

        and:'database available'
        cmReaderService.isDatabaseAvailable() >> true

        and:
            cmReaderService.getAllHierarchyWithBaseSubTree(*_) >> {throw new Exception("error message")}
        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudDeleteRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 405
            nbiResponse.jsonContent == '{"error":{"errorInfo":"Nbi Rest Execution Error (error message)"}}'
        }
    }

    def 'Execute nbiCrudDeleteRequest when database not available return fail'() {
        given: 'nbiCrudDeleteRequest'
        NbiCrudDeleteRequest nbiCrudDeleteRequest = new NbiCrudDeleteRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , "BASE_ONLY", 0, filter)

        and:'database not available'
        cmReaderService.isDatabaseAvailable() >> false

        and:
        cmReaderService.getAllHierarchyWithBaseSubTree(*_) >> {throw new Exception("error message")}
        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudDeleteRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 503
            nbiResponse.jsonContent == '{"error":{"errorInfo":"Service is currently unavailable."}}'
        }
    }

    def 'Execute nbiCrudDeleteRequest when cmReaderService return errored CmResponse return fail'() {
        given: 'nbiCrudDeleteRequest'
        NbiCrudDeleteRequest nbiCrudDeleteRequest = new NbiCrudDeleteRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , "BASE_ALL", 0, filter)

        and: 'mockedCmResponse'
        mockedCmResponse.getErrorCode() >> -1
        mockedCmResponse.getStatusMessage() >> "error message inside cmreader"

        and:'database available'
        cmReaderService.isDatabaseAvailable() >> true

        and:
        cmReaderService.getAllHierarchyWithBaseAll(*_) >> mockedCmResponse
        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudDeleteRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 417
            nbiResponse.jsonContent == '{"error":{"errorInfo":"error message inside cmreader"}}'
        }
    }

    def 'Execute nbiCrudDeleteRequest with scope BASE_NTH_LEVEL when cmReaderService return errored CmResponse return fail'() {
        given: 'nbiCrudDeleteRequest'
        NbiCrudDeleteRequest nbiCrudDeleteRequest = new NbiCrudDeleteRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , "BASE_NTH_LEVEL", 1, null)

        and: 'mockedCmResponse'
        mockedCmResponse.getErrorCode() >> -1
        mockedCmResponse.getStatusMessage() >> "error message inside cmreader"

        and:'database available'
        cmReaderService.isDatabaseAvailable() >> true

        and:
        cmReaderService.getAllHierarchyWithBaseNthLevel(*_) >> mockedCmResponse
        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudDeleteRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 417
            nbiResponse.jsonContent == '{"error":{"errorInfo":"error message inside cmreader"}}'
        }
    }
    /*
    *                 DELETE COMMAND REQUEST - FROM PATCH
    *
    * */

    @Unroll
    def 'Execute cmDeleteRequest (From Patch) when cmWriterService throws Exception/CmRestNbiValidationException return fail'() {
        given: 'cmDeleteRequest'
        def fdn = 'NetworkElement=ERBS002'

        CmDeleteRequest cmDeleteRequest = new CmDeleteRequest(fdn, ScopeType.BASE_ALL, 0, null, false);

        and:'database available'
        cmReaderService.isDatabaseAvailable() >> true

        and:
        cmWriterService.deleteObjectsForNbiPatch(*_)  >> {throw exception}
        when:
        CmDeleteResponse cmDeleteResponse = objUnderTest.executor(cmDeleteRequest);

        then:
        with(cmDeleteResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            moObjects == null
            httpCode == 405
        }

        where:
        exception                                                    | _
        new Exception("error message")                               | _
        new CmRestNbiValidationException("error message", 405)       | _
    }


}