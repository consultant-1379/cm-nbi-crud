package com.ericsson.oss.services.cmnbicrud.ejb.impl.executor

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.rule.custom.node.NodeProperty
import com.ericsson.cds.cdi.support.rule.custom.node.RootNode
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.sdk.security.accesscontrol.EAccessControl
import com.ericsson.oss.itpf.sdk.security.accesscontrol.ESecurityTarget
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudPostRequest
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudPutRequest
import com.ericsson.oss.presentation.cmnbirest.api.NbiResponse
import com.ericsson.oss.services.cm.cmsearch.DynamicBlackListInfoIf
import com.ericsson.oss.services.cm.cmsearch.excessivedata.DynamicBlackListEntry
import com.ericsson.oss.services.cm.cmshared.dto.CmResponse
import com.ericsson.oss.services.cm.cmshared.dto.search.CmSearchCriteria
import com.ericsson.oss.services.cmnbicrud.ejb.common.CmRestNbiConstants
import com.ericsson.oss.services.cmnbicrud.ejb.common.CmRestNbiError
import com.ericsson.oss.services.cmnbicrud.ejb.common.ResponseType
import com.ericsson.oss.services.cmnbicrud.ejb.put.CmPutRequest
import com.ericsson.oss.services.cmnbicrud.ejb.put.CmPutResponse
import com.ericsson.oss.services.cmnbicrud.ejb.testutil.BaseForCommandReceiverSpecs
import com.ericsson.oss.presentation.cmnbirest.api.OperationType
import com.ericsson.oss.services.cmnbicrud.spi.output.ErrorResponseType
import spock.lang.Unroll

import javax.inject.Inject

class CmNbiCrudPutExecutorSpec extends BaseForCommandReceiverSpecs {
    @ObjectUnderTest
    private CmNbiCrudPutExecutor objUnderTest

    @Inject
    DynamicBlackListInfoIf mockedDynamicBlackListInfoIf;

    @Inject
    private EAccessControl mockedAccessControl;

    final CmSearchCriteria cmSearchCriteria = new CmSearchCriteria()

    @RootNode(nodeName = 'ERBS001FORNBI', ipAddress = '10.0.0.1', version = '10.3.100', properties = [
            @NodeProperty(name = 'neType', value = 'ERBS'),
            @NodeProperty(name = 'ossPrefix', value = 'MeContext=ERBS001FORNBI'),
            @NodeProperty(name = 'utcOffset', value = '+00:18')
    ])
    private ManagedObject erbsNodeForNbi1

    String userId = "user"
    String requestId = "crud:1234"
    String ipAddress = "1.2.3.5"
    String ssoToken = "CookieAABBCCDDEE";

    def setup() {
        mockedDynamicBlackListInfoIf.isAvailable() >> true
        mockedDynamicBlackListInfoIf.getDynamicBlackListMap() >> new HashMap<String, DynamicBlackListEntry>()
        mockedDynamicBlackListInfoIf.getDynamicBlackListMapForApi() >> new HashMap<String, DynamicBlackListEntry>()
        mockedDynamicBlackListInfoIf.getDynamicBlackListMapForNbi() >> new HashMap<String, DynamicBlackListEntry>()

        def securityTarget = ESecurityTarget.map('ERBS001FORNBI');
        mockedAccessControl.isAuthorized(securityTarget) >> true
        runtimeDps.withTransactionBoundaries()

        setupDbNode()
    }

    //we create a mocked target and associate it to EnodeBFunction .target(createMockedTarget('ERBS', '19.Q3-J.3.100'))
    //This trick allow to have a target when we ask for getMoTypeFromParent.
    // The mechanism seem implemented and getAllChildTypes return one model ERBS_NODE_MODEL version 10.3.100  (as configured in intra-10.3.100.xml)
    // I think that filtering mechanism when target!=null it use only neType(=ERBS) for this kind of Mos (modelIdentity is not used)
    def createMockedTarget(String neType, String modelIdentity) {
        def mockedTarget = Mock(PersistenceObject.class)
        mockedTarget.getAttribute("category") >> "NODE"
        mockedTarget.getAttribute("type") >> neType
        mockedTarget.getAttribute("modelIdentity") >> modelIdentity
        return mockedTarget
    }

    def setupDbNode() {
        runtimeDps.addManagedObject().withFdn("NetworkElement=ERBS001FORNBI,CmFunction=1")
                .addAttribute('CmFunctionId', "1")
                .namespace("OSS_NE_CM_DEF")
                .version("1.0.1")
                .type("CmFunction")
                .build()
        runtimeDps.addManagedObject().withFdn("MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1")
                .addAttribute('ENodeBFunctionId', "1")
                .addAttribute('userLabel', 'value')
                .addAttribute('eNodeBPlmnId', [mcc: 533, mnc: 57, mncLength:2])
                .onAction('updateMMEConnection').returnValue("RETAIN_EXISTING")
                .namespace(ERBS_MODEL)
                .version(ERBS_VERSION)
                .type("ENodeBFunction").target(createMockedTarget('ERBS', '19.Q3-J.3.100'))
                .build()
        runtimeDps.addManagedObject().withFdn("MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=toBeRemoved")
                .addAttribute('EUtranCellFDDId', "toBeRemoved")
                .addAttribute('userLabel', 'value')
                .onAction('startAilg').returnValue(null)
                .onAction('changeFrequency').returnValue(null)
                .namespace(ERBS_MODEL)
                .version(ERBS_VERSION)
                .type("EUtranCellFDD")
                .build()
    }

    def 'Execute modify NetworkElement attributes utcOffset with success'() {
        given: 'NbiCrudPutRequest'
        def xpath = 'NetworkElement=ERBS001FORNBI'
        def body = '{ "NetworkElement": [{ "id": "ERBS001FORNBI", "attributes": {"utcOffset": "+00:18"} }] }'
        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        CmPutResponse cmPutResponse = objUnderTest.executor(nbiCrudPutRequest)
        then:
        System.out.println("cmPutResponse=" + cmPutResponse)
        with(cmPutResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects == null
            httpCode == 204
        }
    }


    @Unroll
    def 'Execute modify ManagedElement attribute with success'() {
        given: 'NbiCrudPutRequest'
        def xpath = 'MeContext=ERBS001FORNBI,ManagedElement=1'
        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        CmPutResponse cmPutResponse = objUnderTest.executor(nbiCrudPutRequest)
        then:
        System.out.println("cmPutResponse=" + cmPutResponse)
        with(cmPutResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 200
            def resource = ((List)moObjects.moObjects.get("ManagedElement")).iterator().next()
            with (resource) {
                id == '1'
                resource.toString().contains("logicalName")
                !resource.toString().contains("userLabel")
            }
        }

        where:
        body | _
        '{ "ManagedElement": [{ "id": "1", "attributes": {"logicalName": "newLogicalName"} }] }' | _
        '{ "ManagedElement": { "id": "1", "attributes": {"logicalName": "newLogicalName"} } }' | _
    }

    def 'Execute modify ManagedElement attributes with success'() {
        given: 'NbiCrudPutRequest'
        def xpath = 'MeContext=ERBS001FORNBI,ManagedElement=1'
        def body = '{ "ManagedElement": [{ "id": "1", "attributes": {"logicalName": "newLogicalName", "site": "World"} }] }'
        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        CmPutResponse cmPutResponse = objUnderTest.executor(nbiCrudPutRequest)
        then:
        System.out.println("cmPutResponse=" + cmPutResponse)
        with(cmPutResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 200
            def resource = ((List)moObjects.moObjects.get("ManagedElement")).iterator().next()
            with (resource) {
                id == '1'
                resource.toString().contains("logicalName")
                resource.toString().contains("site")
                !resource.toString().contains("userLabel")
            }
        }
    }

    def 'Execute modify ENodeBFunction complex attribute with success'() {
        given: 'NbiCrudPutRequest'
        def xpath = 'MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1'
        def body = '{ "ENodeBFunction": [{ "id": "1", "attributes": {"eNodeBPlmnId": { "mncLength" : 2, "mcc" : 352, "mnc" : 55 } } }] }'
        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        CmPutResponse cmPutResponse = objUnderTest.executor(nbiCrudPutRequest)
        then:
        System.out.println("cmPutResponse=" + cmPutResponse)
        with(cmPutResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 200
            def resource = ((List)moObjects.moObjects.get("ENodeBFunction")).iterator().next()
            with (resource) {
            id == '1'
            attributes.get("eNodeBPlmnId") == [mcc:352, mnc:55, mncLength:2]
            !attributes.containsKey('userLabel')
            }
        }
    }

    def 'Execute modify ENodeBFunction complex attribute (partial) return fail'() {
        given: 'NbiCrudPutRequest'
        def xpath = 'MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1'
        def body = '{ "ENodeBFunction": [{ "id": "1", "attributes": {"eNodeBPlmnId": { "mnc" : 77 } } }] }'
        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        CmPutResponse cmPutResponse = objUnderTest.executor(nbiCrudPutRequest)
        then:
        with(cmPutResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            moObjects == null
            errorResponseType.error.errorInfo.contains("eNodeBPlmnId requires the following mandatory attribute(s): (mncLength, mcc)")
            httpCode == 417
        }
    }

    def 'Execute modify EUtranCellFDD with sensitive attribute with success'() {
        given: 'NbiCrudPutRequest'
        def xpath = 'MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=toBeRemoved'
        def body = '{ "EUtranCellFDD": [{ "id": "toBeRemoved", "attributes": {"userLabel": "newLabel", "testSensitivePersistent": "test1"} }] }'
        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        CmPutResponse cmPutResponse = objUnderTest.executor(nbiCrudPutRequest)
        then:
        System.out.println("cmPutResponse=" + cmPutResponse)
        with(cmPutResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 200
            def resource = ((List)moObjects.moObjects.get("EUtranCellFDD")).iterator().next()
            with (resource) {
                id == 'toBeRemoved'
                resource.toString().contains("userLabel")
                resource.toString().contains("testSensitivePersistent=************")
            }
        }
    }

    /*
     CHECK INPUT PARAMETERS
    */
    def 'Execute put request with xpath=null fails'() {
        given: 'nbiCrudPutRequest'
        def xpath = null
        def body = "{}"
        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        CmPutResponse cmPutResponse = objUnderTest.executor(nbiCrudPutRequest)
        then:
        with(cmPutResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            moObjects == null
            errorResponseType.error.errorInfo.contains("The supplied parameters xpath=null is not allowed.")
            httpCode == 400
        }
    }

    def 'Execute modify ManagedElement attribute with wrong Json body fails'() {
        given: 'NbiCrudPutRequest'
        def xpath = 'MeContext=ERBS001FORNBI,ManagedElement=1'
        def wrongBody = '{ "ManagedElement": { "id": "1", "attributes": {"logicalName": "newLogicalName"} }] }'
        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , wrongBody)

        when:
        CmPutResponse cmPutResponse = objUnderTest.executor(nbiCrudPutRequest)
        then:
        System.out.println("cmPutResponse=" + cmPutResponse)
        with(cmPutResponse) {
            responseType == ResponseType.FAIL
            httpCode == CmRestNbiError.INVALID_JSON.getHttpcode()
            errorResponseType != null
            moObjects == null
            errorResponseType.error.errorInfo.contains("Invalid json in body")
            httpCode == 400
        }
    }

    def 'Execute modify ManagedElement attribute with wrong attribute name fails'() {
        given: 'NbiCrudPutRequest'
        def xpath = 'MeContext=ERBS001FORNBI,ManagedElement=1'
        def wrongAttributeNameBody = '{ "ManagedElement": [{ "id": "1", "attributes": {"logiclName": "newLogicalName"} }] }'
        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , wrongAttributeNameBody)

        when:
        CmPutResponse cmPutResponse = objUnderTest.executor(nbiCrudPutRequest)
        then:
        System.out.println("cmPutResponse=" + cmPutResponse)
        with(cmPutResponse) {
            responseType == ResponseType.FAIL
            httpCode == CmRestNbiConstants.HTTP_EXPECTATION_FAILED
            errorResponseType != null
            moObjects == null
            errorResponseType.error.errorInfo.contains("unknown attribute")
            httpCode == 417
        }
    }

    def 'Execute modify ManagedElement attribute with wrong attribute value fails'() {
        given: 'NbiCrudPutRequest'
        def xpath = 'MeContext=ERBS001FORNBI,ManagedElement=1'
        def wrongAttributeValueBody = '{ "ManagedElement": [{ "id": "1", "attributes": {"logicalName": {}} }] }'
        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , wrongAttributeValueBody)

        when:
        CmPutResponse cmPutResponse = objUnderTest.executor(nbiCrudPutRequest)
        then:
        System.out.println("cmPutResponse=" + cmPutResponse)
        with(cmPutResponse) {
            responseType == ResponseType.FAIL
            httpCode == CmRestNbiConstants.HTTP_EXPECTATION_FAILED
            errorResponseType != null
            moObjects == null
            errorResponseType.error.errorInfo.contains("incorrect attribute data type")
            httpCode == 417
        }
    }

    /*
        TEST HTTP RETURN CODES
    */
    def 'Execute nbiCrudPutRequest with objUnderTest.executeOperation return success'() {
        given: 'nbiCrudPutRequest'
        def xpath = 'MeContext=ERBS001FORNBI,ManagedElement=1'
        def body = '{ "ManagedElement": [{ "id": "1", "attributes": {"logicalName": "newLogicalName"} }] }'

        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPutRequest)
        then:
        with(nbiResponse) {
            httpCode == 200
            jsonContent == '{"ManagedElement":{"id":"1","attributes":{"logicalName":"newLogicalName"}}}'
        }
    }

    def 'Execute nbiCrudPutRequest with objUnderTest.executeOperation with testDate (Date() field) return success'() {
        given: 'nbiCrudPostRequest'
        def xpath = 'NetworkElement=ERBS001FORNBI'
        def body = '{ "NetworkElement": [ { "id": "ERBS001FORNBI", "attributes": { "testDate":"Sat Sep 18 11:00:00 CEST 2021"}}] }'

        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPutRequest)
        then:
        with(nbiResponse) {
            httpCode == 200
            jsonContent.contains('Sep ') //minimal check cause on PCR env different timezone
        }
    }

    def 'Execute nbiCrudPutRequest with objUnderTest.executeOperation return success + unchanged userlabel'() {
        given:
        def xpath = 'MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1'
        def body = '{ "ENodeBFunction": [ { "id": "1", "attributes": { "userLabel":"value", "eNodeBPlmnId": {"mcc": 533, "mnc": 57, "mncLength":3} }}] }'

        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPutRequest)
        then:
        with(nbiResponse) {
            httpCode == 200
            jsonContent.contains('mcc') //minimal check cause on PCR env different timezone
            !jsonContent.contains('userLabel') //minimal check cause on PCR env different timezone
        }
    }

    @Unroll
    def 'Execute nbiCrudPutRequest with objUnderTest.executeOperation return success + unchanged userlabel + unchanged pmZtemporary34 (cause read-only attribute)'() {
        given:
        def xpath = 'MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1'
        def body = '{ "ENodeBFunction": [ { "id": "1", "attributes": { "userLabel":"value", "pmZtemporary34":'+ value +' }}] }'

        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPutRequest)
        then:
        with(nbiResponse) {
            httpCode == 204
            jsonContent == '{}'
        }

        where:
        value | _
        null  |_
        '1'   |_
    }

    def 'Execute nbiCrudPutRequest with objUnderTest.executeOperation with utcOffset return success'() {
        given: 'nbiCrudPostRequest'
        def xpath = 'NetworkElement=ERBS001FORNBI'
        def body = '{ "NetworkElement": [{ "id": "ERBS001FORNBI", "attributes": {"utcOffset": "+00:18"} }] }'

        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPutRequest)
        then:
        with(nbiResponse) {
            httpCode == 204
            jsonContent == '{}'
        }
    }



    def 'Execute nbiCrudPutRequest with objUnderTest.executeOperation return fail'() {
        given: 'nbiCrudPutRequest'
        def xpath = 'MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=xxx'
        def body ='{ "EUtranCellFDD": [ { "id": "1", "attributes": { "userLaBEL":22 }}]}'

        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPutRequest)
        then:
        with(nbiResponse) {
            httpCode == 404
            jsonContent == '{"error":{"errorInfo":"The supplied FDN MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=xxx does not exist in the database"}}'
        }
    }

    def 'Execute nbiCrudPutRequest with objUnderTest.executeOperation return fail when invalid Json'() {
        given: 'nbiCrudPutRequest'
        def xpath = 'MeContext=ERBS001FORNBI,ManagedElement=1'
        def body ='{ invalidjson }'

        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPutRequest)
        then:
        with(nbiResponse) {
            httpCode == 400
            jsonContent.contains('Invalid json in body:')
        }
    }

    def 'Execute nbiCrudPutRequest with objUnderTest.executeOperation return fail when invalid TBAC node ERBS001'() {
        given: 'nbiCrudPutRequest'
        def xpath = 'MeContext=ERBS001,ManagedElement=1'
        def body = '{ "ManagedElement": [{ "id": "1", "attributes": {"logicalName": "newLogicalName"} }] }'

        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPutRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 401
            nbiResponse.jsonContent.contains('Access Denied')
        }
    }

    def 'CmPutRequest and CmPutResponse for_coverage_only'() {
        when:
        CmPutRequest cmPutRequest = new CmPutRequest("fdn", '1' , 'type', null)
        CmPutResponse cmPutResponse = new CmPutResponse(null, null , null, 0)

        then:
        cmPutRequest.getDescriptionForUnimplementedUseCase()
        cmPutRequest.toString()
        cmPutResponse.toString()
    }

    def 'formatCmObject when empty response fail (for_coverage_only)'() {
        when:
        CmPutResponse cmPutResponse = objUnderTest.formatCmObject(new CmResponse())

        then:
        with(cmPutResponse) {
            responseType == ResponseType.FAIL
            moObjects == null
            cmObjects == null
            errorResponseType.error.errorInfo.contains("NOT FOUND MOs matching conditions")
            httpCode == 404
        }
    }

    def 'formatCmObject when errored response fail (for_coverage_only)'() {
        when:
        CmResponse erroredCmResponse = new CmResponse()
        erroredCmResponse.setErrorCode(9999)
        erroredCmResponse.setStatusMessage("some error")
        CmPutResponse cmPutResponse = objUnderTest.formatCmObject(erroredCmResponse)

        then:
        with(cmPutResponse) {
            responseType == ResponseType.FAIL
            moObjects == null
            cmObjects == null
            errorResponseType.error.errorInfo.contains("some error")
            httpCode == 417
        }
    }

    def 'dummy test to force maven to run test class with all other test methods annotated with @Unroll'() {
        when:
        objUnderTest.toString()
        then:
        1 == 1
    }
}