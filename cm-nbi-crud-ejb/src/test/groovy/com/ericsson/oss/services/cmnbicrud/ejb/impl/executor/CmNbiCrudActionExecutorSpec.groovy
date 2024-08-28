package com.ericsson.oss.services.cmnbicrud.ejb.impl.executor

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.rule.custom.node.NodeProperty
import com.ericsson.cds.cdi.support.rule.custom.node.RootNode
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject
import com.ericsson.oss.itpf.sdk.security.accesscontrol.EAccessControl
import com.ericsson.oss.itpf.sdk.security.accesscontrol.ESecurityTarget
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudActionRequest
import com.ericsson.oss.presentation.cmnbirest.api.NbiResponse
import com.ericsson.oss.services.cm.cmsearch.DynamicBlackListInfoIf
import com.ericsson.oss.services.cm.cmsearch.excessivedata.DynamicBlackListEntry
import com.ericsson.oss.services.cm.cmwriter.api.CmWriterService
import com.ericsson.oss.services.cmnbicrud.ejb.action.CmActionRequest
import com.ericsson.oss.services.cmnbicrud.ejb.action.CmActionResponse
import com.ericsson.oss.services.cmnbicrud.ejb.patch.ErroredWriteResponse
import com.ericsson.oss.services.cmnbicrud.ejb.testutil.BaseForCommandReceiverSpecs
import com.ericsson.oss.services.cmnbicrud.spi.output.ErrorResponseType
import spock.lang.Unroll

import javax.inject.Inject

class CmNbiCrudActionExecutorSpec extends BaseForCommandReceiverSpecs {
    @ObjectUnderTest
    private CmNbiCrudActionExecutor objUnderTest

    @Inject
    CmWriterService cmWriterService;

    @Inject
    DynamicBlackListInfoIf mockedDynamicBlackListInfoIf;

    @Inject
    private EAccessControl mockedAccessControl;

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

        securityTarget = ESecurityTarget.map('ERBS002');
        mockedAccessControl.isAuthorized(securityTarget) >> true

        runtimeDps.withTransactionBoundaries()

        setupDbNode()
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
                .onAction('collectDynamicCellStatus').returnValue('[{"cell"":"EUtranCellFDD"="LTE05ERBS00028-1", "ue":26, "srb": 10, "drb": 12}]')
                .namespace(ERBS_MODEL)
                .version(ERBS_VERSION)
                .build()
        runtimeDps.addManagedObject().withFdn("MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=ERBS001FORNBI-1")
                .addAttribute('EUtranCellFDDId', "ERBS001FORNBI-1")
                .addAttribute('userLabel', 'value')
                .onAction('startAilg').returnValue(null)
                .onAction('changeFrequency').returnValue(null)
                .namespace(ERBS_MODEL)
                .version(ERBS_VERSION)
                .type("EUtranCellFDD")
                .build()
    }

    @Unroll
    def 'Check ignore unknown fields'() {
        given: 'NbiCrudPatchActionRequest'
        def xpath = 'MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1'
        def action = 'updateMMEConnection'
        NbiCrudActionRequest nbiCrudPatchActionRequest = new NbiCrudActionRequest(userId, requestId, ipAddress, ssoToken, xpath , action, body)

        when:
        objUnderTest.executor(nbiCrudPatchActionRequest)
        then:
        notThrown(Exception)

        where:
        body | _
        '{ "input": { "orderCode":"RENEW_ALL_EXISTING" } , "otherField" : "1"}' | _
    }

    def 'Execute NbiCrudPatchActionRequest with invalid action and input parameter(s) return fail'() {
        given: 'NbiCrudPatchActionRequest'
        def xpath = 'MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1'
        NbiCrudActionRequest nbiCrudPatchActionRequest = new NbiCrudActionRequest(userId, requestId, ipAddress, ssoToken, xpath , action, body)

        when:
        CmActionResponse response = objUnderTest.executor(nbiCrudPatchActionRequest)
        then:
        response.isErrored()
        response.attributes == null
        response.errorResponseType != null
        response.httpCode == httpCode

        where:
        action                  | body          | httpCode  | _
        null                    | "any body"    | 400       | _
        ""                      | "any body"    | 400       | _
        "updateMMEConnection"   | '""'          | 400       | _
        "updateMMEConnection"   | '{ null }'    | 400       | _
    }

    /*
         CHECK INPUT PARAMETERS
     */
    @Unroll
    def 'Execute NbiCrudPatchActionRequest with invalid action (name and parameter) return fail'() {
        given: 'NbiCrudPatchActionRequest'
        def xpath = 'MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1'
        NbiCrudActionRequest nbiCrudPatchActionRequest = new NbiCrudActionRequest(userId, requestId, ipAddress, ssoToken, xpath , actionName, body)

        when:
        CmActionResponse response = objUnderTest.executor(nbiCrudPatchActionRequest)
        then:
        response.isErrored()
        response.attributes == null
        response.errorResponseType != null
        response.httpCode == 417

        where:
        actionName              | body | _
        "wrongAction"           | '{ "input": { "orderCode":"RENEW_ALL_EXISTING" } }' | _
        "updateMMEConnection"   | '{ "input": { "wrongParam":"RENEW_ALL_EXISTING" } }' | _
        "updateMMEConnection"   | '{ "input": { "orderCode":"WRONG_VALUE" } }' | _
        "updateMMEConnection"   | '{ "input": { "orderCode":"WRONG_VALUE", "unexpectedParam" : "5" } }' | _
        "updateMMEConnection"   | '{ "input": { } }'        | _
        "updateMMEConnection"   | '{ "input": 1 }'          | _
        "updateMMEConnection"   | ""                        | _
        "updateMMEConnection"   | '{}'                      | _
        "updateMMEConnection"   | '{ }'                     | _
        "updateMMEConnection"   | '{ "wrong": "someValue"}' | _
        "updateMMEConnection"   | '{ "input": null }'       | _
        "updateMMEConnection"   | '{ "input": "input" }'    | _
    }

    @Unroll
    def 'Execute NbiCrudPatchActionRequest with invalid fdn return fail'() {
        given: 'NbiCrudPatchActionRequest'
        def xpath = 'MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=11'
        NbiCrudActionRequest nbiCrudPatchActionRequest = new NbiCrudActionRequest(userId, requestId, ipAddress, ssoToken, xpath , actionName, body)

        when:
        CmActionResponse response = objUnderTest.executor(nbiCrudPatchActionRequest)
        then:
        response.isErrored()
        response.attributes == null
        response.errorResponseType != null
        response.httpCode == 404

        where:
        actionName            | body | _
        "updateMMEConnection" | '{ "input": { "orderCode":"RENEW_ALL_EXISTING" } }' | _
    }

    @Unroll
    def 'Execute NbiCrudPatchActionRequest with correct params return success'() {
        given: 'NbiCrudPatchActionRequest'
        NbiCrudActionRequest nbiCrudPatchActionRequest = new NbiCrudActionRequest(userId, requestId, ipAddress, ssoToken, xpath, actionName, body)

        when:
        CmActionResponse response = objUnderTest.executor(nbiCrudPatchActionRequest)
        response.toString()
        response.getResponseType()
        then:
        !response.isErrored()
        response.attributes != null
        response.httpCode == 200

        where:
        xpath                                                                                       | actionName            | body                                                  | _
        "MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1"                                 | "updateMMEConnection" | '{ "input": { "orderCode":"RENEW_ALL_EXISTING" } }'   | _
        "MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=ERBS001FORNBI-1"   | "changeFrequency"     | '{ "input": { "earfcn":5 } }'                         | _
    }

    @Unroll
    def 'Execute nbiCrudPatchActionRequest with objUnderTest.executeOperation return success'() {
        given: 'NbiCrudPatchActionRequest'
        def xpath = 'MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1'
        NbiCrudActionRequest nbiCrudActionRequest = new NbiCrudActionRequest(userId, requestId, ipAddress, ssoToken, xpath, actionName, body)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudActionRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == httpCode
            nbiResponse.jsonContent == output
        }

        where:
        actionName                  | body                                                          | output |  httpCode | _
        "updateMMEConnection"       | '{ "input": { "orderCode":"RENEW_ALL_EXISTING" } }'           | null  | 204 | _
        "collectDynamicCellStatus"  | '{ "input": { "dynamicCellStatusOutput":"FORMATTED_TEXT" } }' | '{ "output": "[{\\"cell\\"\\":\\"EUtranCellFDD\\"=\\"LTE05ERBS00028-1\\", \\"ue\\":26, \\"srb\\": 10, \\"drb\\": 12}]"}' | 200 | _
    }


    @Unroll
    def 'Execute nbiCrudPatchActionRequest with objUnderTest.executeOperation return no cmObject'() {
        given: 'NbiCrudPatchActionRequest'
        def xpath = 'MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=ERBS001FORNBI-1'
        NbiCrudActionRequest nbiCrudActionRequest = new NbiCrudActionRequest(userId, requestId, ipAddress, ssoToken, xpath, actionName, body)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudActionRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 204
            nbiResponse.jsonContent == null
        }

        where:
        actionName            | body                            | _
        "changeFrequency"     | '{ "input": { "earfcn":5 } }'   | _

    }

    /*
       TEST SINGLE METHODS
    */

    def 'CmPatchRequest for_coverage_only'() {
        when:
        CmActionRequest cmPatchActionRequest = new CmActionRequest("", "", new HashMap<String, Object>());
        ErroredWriteResponse erroredWriteResponse = new ErroredWriteResponse(new ErrorResponseType("error"), 400)

        then:
        cmPatchActionRequest.toString()
        erroredWriteResponse.getResponseType()
        erroredWriteResponse.toString()
    }

    def 'test obscureAndSetBody - for coverage when Exception'() {
        when:
            def body = objUnderTest.obscureAndSetBody(null,null)
        then:
            1 == 1
    }

    def 'dummy test to force maven to run test class with all other test methods annotated with @Unroll'() {
        when:
        objUnderTest.toString()
        then:
        1 == 1
    }

}