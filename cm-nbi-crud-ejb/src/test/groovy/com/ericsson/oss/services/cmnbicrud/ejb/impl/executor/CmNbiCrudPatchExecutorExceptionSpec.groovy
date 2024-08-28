package com.ericsson.oss.services.cmnbicrud.ejb.impl.executor

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.sdk.security.accesscontrol.SecurityViolationException
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudPatchRequest
import com.ericsson.oss.presentation.cmnbirest.api.NbiResponse
import com.ericsson.oss.presentation.cmnbirest.api.PatchContentType
import com.ericsson.oss.services.cm.cmreader.api.CmReaderService
import com.ericsson.oss.services.cm.cmwriter.api.CmWriterService
import spock.lang.Unroll

import javax.inject.Inject

class CmNbiCrudPatchExecutorExceptionSpec extends CdiSpecification {
    @ObjectUnderTest
    private CmNbiCrudPatchExecutor objUnderTest

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

    def setup() {
        cmReaderService.existsFdn(_) >> true
        cmWriterService.validateFdnAgainstTbac(_) >> true
    }

    @Unroll
    def '3GPP_JSON_PATCH - Execute nbiCrudPatchRequest when cmWriterService throws Exception return fail'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)

        and:'database available'
        cmReaderService.isDatabaseAvailable() >> true

        and:
        mockedCmNbiCrudPatchExecutorBean.executor(_) >> {throw new Exception("error message")}

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPatchRequest)

        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 405
            nbiResponse.jsonContent == '{"error":{"errorInfo":"Nbi Rest Execution Error error message"}}'
        }

        where:
        xpath                                                       | body                                                                                                                                                                                                                                                                                                                                                         | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1" | '[ { "op": "add", "path": "/EUtranCellFDD=20", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } }, { "op": "replace", "path":"#attributes/userlabel", "value": "newUserLabel" }, { "op": "remove", "path": "/EUtranCellFDD=toBeRemoved" } ]' | _
    }

    @Unroll
    def '3GPP_JSON_PATCH - Execute nbiCrudPatchRequest when database not available return fail'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)

        and:'database not available'
        cmReaderService.isDatabaseAvailable() >> false

        and:
        mockedCmNbiCrudPatchExecutorBean.executor(_) >> {throw new Exception("error message")}

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPatchRequest)

        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 503
            nbiResponse.jsonContent == '{"error":{"errorInfo":"Service is currently unavailable."}}'
        }

        where:
        xpath                                                                        | body                                                                                                                                                                                                                                                                                                                                                         | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1" | '[ { "op": "add", "path": "/EUtranCellFDD=20", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } }, { "op": "replace", "path":"#attributes/userlabel", "value": "newUserLabel" }, { "op": "remove", "path": "/EUtranCellFDD=toBeRemoved" } ]' | _
    }

    @Unroll
    def 'JSON_PATCH - Execute nbiCrudPatchRequest when cmWriterService throws Exception return fail'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.JSON_PATCH)

        and:'database available'
        cmReaderService.isDatabaseAvailable() >> true

        and:
        mockedCmNbiCrudPatchExecutorBean.executor(_) >> {throw new Exception("error message")}

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPatchRequest)

        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 405
            nbiResponse.jsonContent == '{"error":{"errorInfo":"Nbi Rest Execution Error error message"}}'
        }

        where:
        xpath                                                                        | body                                                                                                                                                                                                                                                                                                                                                         | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20" | '[ { "op": "add", "path": "", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } }, { "op": "replace", "path":"/attributes/userlabel", "value": "newUserLabel" } ]' | _
    }

    @Unroll
    def 'JSON_PATCH - Execute nbiCrudPatchRequest when database not available return fail'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.JSON_PATCH)

        and:'database not available'
        cmReaderService.isDatabaseAvailable() >> false

        and:
        mockedCmNbiCrudPatchExecutorBean.executor(_) >> {throw new Exception("error message")}

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPatchRequest)

        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 503
            nbiResponse.jsonContent == '{"error":{"errorInfo":"Service is currently unavailable."}}'
        }

        where:
        xpath                                                                        | body                                                                                                                                                                                                                                                                                                                                                         | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20" | '[ { "op": "add", "path": "", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } }, { "op": "replace", "path":"/attributes/userlabel", "value": "newUserLabel" } ]' | _
    }

    @Unroll
    def '3GPP_JSON_PATCH - Execute nbiCrudPatchRequest when cmWriterService throws SecurityViolationException return fail'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)

        and:'database available'
        cmReaderService.isDatabaseAvailable() >> true

        and:
        SecurityViolationException securityViolationException = new SecurityViolationException("Security Violation error message")
        mockedCmNbiCrudPatchExecutorBean.executor(_) >> {throw new Exception("error message", securityViolationException)}

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPatchRequest)

        then:
        thrown(SecurityViolationException)

        where:
        xpath                                                                        | body                                                                                                                                                                                                                                                                                                                                                         | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1" | '[ { "op": "add", "path": "/EUtranCellFDD=20", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } }, { "op": "replace", "path":"#attributes/userlabel", "value": "newUserLabel" }, { "op": "remove", "path": "/EUtranCellFDD=toBeRemoved" } ]' | _
    }
}