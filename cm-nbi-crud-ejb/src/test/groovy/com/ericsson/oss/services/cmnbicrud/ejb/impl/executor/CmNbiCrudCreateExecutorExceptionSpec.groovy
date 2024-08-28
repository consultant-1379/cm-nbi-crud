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
import com.ericsson.oss.services.cmnbicrud.ejb.create.CmCreateRequest
import com.ericsson.oss.services.cmnbicrud.ejb.create.CmCreateResponse
import com.ericsson.oss.services.cmnbicrud.ejb.exceptions.CmRestNbiValidationException
import com.ericsson.oss.services.cmnbicrud.ejb.testutil.TestUtil
import spock.lang.Unroll

import javax.inject.Inject

class CmNbiCrudCreateExecutorExceptionSpec extends CdiSpecification {
    @ObjectUnderTest
    private CmNbiCrudCreateExecutor objUnderTest

   @Inject
   CmWriterService cmWriterService;

    @Inject
    CmReaderService cmReaderService;

    String userId = "user"
    String requestId = "crud:1234"
    String ipAddress = "1.2.3.5"
    String ssoToken = "CookieAABBCCDDEE";

    def setup() {
        cmReaderService.existsFdn(_) >> true
        cmWriterService.validateFdnAgainstTbac(_) >> true
    }

    /*
     *                 POST COMMAND REQUEST
     *
    * */

    def 'POST - Execute nbiCrudPostRequest when cmWriterService throws Exception return fail'() {
        given: 'nbiCrudPostRequest'
        def xpath = 'MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1'
        def body ='{ "EUtranCellFDD": [ { "id": "1", "attributes": { "EUtranCellFDDId":"2022", "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test"' +
                ' }}] }'

        NbiCrudPostRequest nbiCrudPostRequest = new NbiCrudPostRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        and:'database available'
        cmReaderService.isDatabaseAvailable() >> true

        and:
            cmWriterService.createObjectForNbi(*_)  >> {throw new Exception("error message")}
        when:
             NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPostRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 405
            nbiResponse.jsonContent == '{"error":{"errorInfo":"Nbi Rest Execution Error (error message)"}}'
        }
    }

    def 'POST - Execute nbiCrudPostRequest when database not available instance fail'() {
        given: 'nbiCrudPostRequest'
        def xpath = 'MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1'
        def body ='{ "EUtranCellFDD": [ { "id": "1", "attributes": { "EUtranCellFDDId":"2022", "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test"' +
                ' }}] }'

        NbiCrudPostRequest nbiCrudPostRequest = new NbiCrudPostRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        and:'database not available'
        cmReaderService.isDatabaseAvailable() >> false

        and:
        cmWriterService.createObjectForNbi(*_)  >> {throw new Exception("error message")}
        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPostRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 503
            nbiResponse.jsonContent == '{"error":{"errorInfo":"Service is currently unavailable."}}'
        }
    }

    def 'POST - Execute nbiCrudPostRequest when cmWriterService return 0 instance fail'() {
        given: 'nbiCrudPostRequest'
        def xpath = 'MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1'
        def body ='{ "EUtranCellFDD": [ { "id": "1", "attributes": { "EUtranCellFDDId":"2022", "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test"' +
                ' }}] }'

        NbiCrudPostRequest nbiCrudPostRequest = new NbiCrudPostRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        and:'database available'
        cmReaderService.isDatabaseAvailable() >> true

        and:
        cmWriterService.createObjectForNbi(*_)  >> new CmResponse()
        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPostRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 404
            nbiResponse.jsonContent == '{"error":{"errorInfo":"NOT FOUND MOs matching conditions"}}'
        }
    }


    /*
     *                 PUT (CREATE) COMMAND REQUEST
     *
    * */

    def 'PUT - Execute nbiCrudPutRequest when cmWriterService throws Exception return fail'() {
        given: 'nbiCrudPutRequest'
        def xpath = 'MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=1'
        def body ='{ "EUtranCellFDD": [ { "id": "1", "attributes": { "EUtranCellFDDId":"2022", "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test"' +
                ' }}] }'

        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        and:'database available'
        cmReaderService.isDatabaseAvailable() >> true

        and:
        cmWriterService.createObjectForNbi(*_)  >> {throw new Exception("error message")}
        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPutRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 405
            nbiResponse.jsonContent == '{"error":{"errorInfo":"Nbi Rest Execution Error (error message)"}}'
        }
    }

    def 'PUT - Execute nbiCrudPutRequest when database not available return fail'() {
        given: 'nbiCrudPutRequest'
        def xpath = 'MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=1'
        def body ='{ "EUtranCellFDD": [ { "id": "1", "attributes": { "EUtranCellFDDId":"2022", "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test"' +
                ' }}] }'

        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        and:'database not available'
        cmReaderService.isDatabaseAvailable() >> false

        and:
        cmWriterService.createObjectForNbi(*_)  >> {throw new Exception("error message")}
        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPutRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 503
            nbiResponse.jsonContent == '{"error":{"errorInfo":"Service is currently unavailable."}}'
        }
    }

    def 'PUT - Execute nbiCrudPutRequest when cmWriterService return 0 instance fail'() {
        given: 'nbiCrudPutRequest'
        def xpath = 'MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=1'
        def body ='{ "EUtranCellFDD": [ { "id": "1", "attributes": { "EUtranCellFDDId":"2022", "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test"' +
                ' }}] }'

        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        and:'database available'
        cmReaderService.isDatabaseAvailable() >> true

        and:
        cmWriterService.createObjectForNbi(*_)  >> new CmResponse()
        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPutRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 404
            nbiResponse.jsonContent == '{"error":{"errorInfo":"NOT FOUND MOs matching conditions"}}'
        }
    }


    /*
    *                 POST (CREATE) COMMAND REQUEST - FROM PATCH
    *
    * */

    @Unroll
    def 'POST - Execute cmCreateRequest (From Patch) when cmWriterService throws Exception/CmRestNbiValidationException return fail'() {
        given: 'nbiCrudPostRequest'
        def parentFdn = 'MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1'
        def id = "1"
        def childType = "EUtranCellFDD"

        Map<String, Object> attributes = new HashMap<>();
        attributes.put( "EUtranCellFDDId", "2022");
        attributes.put( "userLaBEL", 22);
        attributes.put( "physicalLayerSubCellId", 1);
        attributes.put( "earfcndl", 3);
        attributes.put( "earfcnul", 18000);
        attributes.put( "tac", "2");
        attributes.put( "physicalLayerCellIdGroup", "1");
        attributes.put( "EUtranCellFDDId", "2022");
        attributes.put( "cellId", "1");
        attributes.put( "testObsoleteNonPersistent", "test");

        CmCreateRequest cmCreateRequest = new CmCreateRequest(parentFdn, id, childType, attributes )

        and:'database available'
        cmReaderService.isDatabaseAvailable() >> true

        and:
        cmWriterService.createObjectForNbi(*_)  >> {throw exception}
        when:
        CmCreateResponse cmCreateResponse = objUnderTest.executor(cmCreateRequest);

        then:
        with(cmCreateResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            moObjects == null
            errorResponseType.error.errorInfo.contains("error message")
            httpCode == 405
        }

        where:
        exception                                                  || _
        new Exception("error message")                             || _
        new CmRestNbiValidationException("error message", 405)     || _
    }
}