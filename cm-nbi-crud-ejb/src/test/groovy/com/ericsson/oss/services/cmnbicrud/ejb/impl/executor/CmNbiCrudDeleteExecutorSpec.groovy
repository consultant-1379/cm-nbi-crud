/*
    This class, and model class are copied from SetCommandSpec.groovy
 */
package com.ericsson.oss.services.cmnbicrud.ejb.impl.executor

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.rule.custom.node.NodeProperty
import com.ericsson.cds.cdi.support.rule.custom.node.RootNode
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject
import com.ericsson.oss.itpf.sdk.security.accesscontrol.EAccessControl
import com.ericsson.oss.itpf.sdk.security.accesscontrol.ESecurityTarget
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudDeleteRequest
import com.ericsson.oss.presentation.cmnbirest.api.NbiResponse
import com.ericsson.oss.services.cm.cmsearch.DynamicBlackListInfoIf
import com.ericsson.oss.services.cm.cmsearch.excessivedata.DynamicBlackListEntry
import com.ericsson.oss.services.cm.cmshared.dto.CmObject
import com.ericsson.oss.services.cmnbicrud.ejb.common.ResponseType
import com.ericsson.oss.services.cmnbicrud.ejb.common.ScopeType
import com.ericsson.oss.services.cmnbicrud.ejb.delete.CmDeleteRequest
import com.ericsson.oss.services.cmnbicrud.ejb.delete.CmDeleteResponse
import com.ericsson.oss.services.cmnbicrud.ejb.testutil.BaseForCommandReceiverSpecs
import com.ericsson.oss.services.cmnbicrud.ejb.testutil.TestUtil
import spock.lang.Unroll

import javax.inject.Inject

class CmNbiCrudDeleteExecutorSpec extends BaseForCommandReceiverSpecs {

    @RootNode(nodeName = 'ERBS881', ipAddress = '10.0.0.8', version = '10.3.100', properties = [
            @NodeProperty(name = 'neType', value = 'ERBS'),
            @NodeProperty(name = 'ossPrefix', value = 'SubNetwork=Sample,MeContext=ERBS881'),
            @NodeProperty(name = 'utcOffset', value = '+00:18')
    ])
    private ManagedObject erbsNode8

    @ObjectUnderTest
    private CmNbiCrudDeleteExecutor objUnderTest

    @Inject
    private TestUtil testUtil

    @Inject
    DynamicBlackListInfoIf mockedDynamicBlackListInfoIf;

    @Inject
    private EAccessControl mockedAccessControl;

    String userId = "user"
    String requestId = "crud:1234"
    String ipAddress = "1.2.3.5"
    String ssoToken = "CookieAABBCCDDEE";

    def setup() {
        mockedDynamicBlackListInfoIf.isAvailable() >> true
        mockedDynamicBlackListInfoIf.getDynamicBlackListMap() >> new HashMap<String, DynamicBlackListEntry>()
        mockedDynamicBlackListInfoIf.getDynamicBlackListMapForApi() >> new HashMap<String, DynamicBlackListEntry>()
        mockedDynamicBlackListInfoIf.getDynamicBlackListMapForNbi() >> new HashMap<String, DynamicBlackListEntry>()

        def securityTarget = ESecurityTarget.map('ERBS002');
        mockedAccessControl.isAuthorized(securityTarget) >> true

        def securityTarget2 = ESecurityTarget.map('ERBS881');
        mockedAccessControl.isAuthorized(securityTarget2) >> true

        runtimeDps.withTransactionBoundaries()
        setupDbNodesInSubNetwork(2,4)
    }

    /*
         CHECK INPUT PARAMETERS
     */

    // xpath = null
    @Unroll
    def 'Execute nbiCrudDeleteRequest where xpath is null, the response is always FAIL (scopeType=#scopeType, filter=#filter)'(scopeType, filter) {
        given: 'nbiCrudDeleteRequest'
        def xpath = null

        NbiCrudDeleteRequest nbiCrudDeleteRequest = new NbiCrudDeleteRequest(userId, requestId, ipAddress, ssoToken, xpath , scopeType, 0, filter)

        when:
        CmDeleteResponse cmDeleteResponse = objUnderTest.executor(nbiCrudDeleteRequest)
        then:
        with(cmDeleteResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            errorResponseType.error.errorInfo.contains("The supplied parameters xpath=null is not allowed.")
            httpCode == 400
        }
        where:
        scopeType           ||  filter
        "ANY_SCOPE_TYPE"    ||  null
        "ANY_SCOPE_TYPE"    ||  "ANY_FILTER"
        null                ||  "ANY_FILTER"
        null                ||  null
    }

    // xpath is a not existing FDN
    @Unroll
    def 'Execute nbiCrudDeleteRequest where xpath is a not existing FDN, if scopeType and filter are any valid values, the response is always FAIL (xpath="NetworkElement=xxx", scopeType=#scopeType, filter=#filter)'(scopeType, filter) {

        given: 'nbiCrudDeleteRequest'
        def xpath = "NetworkElement=xxx"
        NbiCrudDeleteRequest nbiCrudDeleteRequest = new NbiCrudDeleteRequest(userId, requestId, ipAddress, ssoToken, xpath, scopeType, 0, filter)

        when:
        CmDeleteResponse cmDeleteResponse = objUnderTest.executor(nbiCrudDeleteRequest)
        then:
        with(cmDeleteResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            errorResponseType.error.errorInfo.contains("The supplied FDN NetworkElement=xxx does not exist in the database")
            httpCode == 404
        }
        where:
        scopeType                ||  filter
        "BASE_ONLY"             ||  "anyFilter"
        "BASE_ONLY"             ||  null
        "BASE_ALL"              ||  "anyFilter"
        "BASE_ALL"              ||  null
        "BASE_NTH_LEVEL"        ||  "anyFilter"
        "BASE_NTH_LEVEL"        ||  null
        "BASE_SUBTREE"          ||  "anyFilter"
        "BASE_SUBTREE"          ||  null
    }

    // xpath is an existing FDN without children
    @Unroll
    def 'Execute nbiCrudDeleteRequest where xpath is an existing FDN without children, if scopeType is any valid value and filter null, the response is always SUCCESS (xpath="MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=4" scopeType=#scopeType)'(scopeType, filter) {
        given: 'nbiCrudDeleteRequest'
        def xpath="MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=4"
        NbiCrudDeleteRequest nbiCrudDeleteRequest = new NbiCrudDeleteRequest(userId, requestId, ipAddress, ssoToken, xpath, scopeType, 0, filter)

        when:
        CmDeleteResponse cmDeleteResponse = objUnderTest.executor(nbiCrudDeleteRequest)
        then:
        with(cmDeleteResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            cmObjects.size() == 1
            httpCode == 200

            def cmObjectList =  getCmObjectList(cmObjects);
            cmObjectList.get(0).fdn == 'MeContext=ERBS002,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=4'
        }
        where:
        scopeType               ||  filter
        "BASE_ONLY"             ||  null
        "BASE_ALL"              ||  null
        "BASE_NTH_LEVEL"        ||  null
    }

    // xpath is an existing FDN with children
    def 'Execute nbiCrudDeleteRequest where xpath is an existing FDN with children, if scopeType is BASE_ONLY and filter null, the response is FAIL (xpath="MeContext=ERBS002" scopeType="BASE_ONLY"'() {

        given: 'nbiCrudGetRequest'
        def xpath="MeContext=ERBS002"
        def scopeType = 'BASE_ONLY'
        def filter = null
        NbiCrudDeleteRequest nbiCrudDeleteRequest = new NbiCrudDeleteRequest(userId, requestId, ipAddress, ssoToken, xpath , scopeType, 0, filter)

        when:
        CmDeleteResponse cmDeleteResponse = objUnderTest.executor(nbiCrudDeleteRequest)
        then:
        with(cmDeleteResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            errorResponseType.error.errorInfo.contains("because there are children")
            httpCode == 400
        }
    }

    @Unroll
    def 'Execute nbiCrudDeleteRequest where xpath is an existing FDN with children, if scopeType is #scopeType and filter is #filter, the response is SUCCESS (xpath="MeContext=ERBS002"'(scopeType, filter) {
        given: 'nbiCrudDeleteRequest'
        def xpath="MeContext=ERBS002"

        NbiCrudDeleteRequest nbiCrudDeleteRequest = new NbiCrudDeleteRequest(userId, requestId, ipAddress, ssoToken, xpath , scopeType, 0, filter)

        when:
        CmDeleteResponse cmDeleteResponse = objUnderTest.executor(nbiCrudDeleteRequest)
        then:
        with(cmDeleteResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            httpCode == 200

            def cmObjectList =  getCmObjectList(cmObjects);
            cmObjectList.size() > 0
            cmObjectList.get(0).fdn.contains('MeContext')
        }
        where:
        scopeType              || filter
        'BASE_ALL'             || null
        'BASE_ALL'             || '//ManagedElement'
        'BASE_NTH_LEVEL'       || null
    }

    @Unroll
    def 'Execute nbiCrudDeleteRequest where xpath is an existing FDN with children, if scopeType is #scopeType and filter is #filter, the response is FAIL (xpath="MeContext=ERBS002"'(scopeType, filter) {
        given: 'nbiCrudDeleteRequest'
        def xpath="MeContext=ERBS002"

        NbiCrudDeleteRequest nbiCrudDeleteRequest = new NbiCrudDeleteRequest(userId, requestId, ipAddress, ssoToken, xpath , scopeType, 0, filter)

        when:
        CmDeleteResponse cmDeleteResponse = objUnderTest.executor(nbiCrudDeleteRequest)
        then:
        with(cmDeleteResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            errorResponseType.error.errorInfo.contains("Filter not allowed with scopeType=BASE_NTH_LEVEL")
            httpCode == 400
        }
        where:
        scopeType              || filter
        'BASE_NTH_LEVEL'       || '//ManagedElement'
    }

    @Unroll
    def 'Execute nbiCrudDeleteRequest with #scopeType scopeType AND filter NOT NULL return fail'(scopeType, errorMessage) {
        given: 'nbiCrudDeleteRequest'
        def filter = "Anyfilter"
        NbiCrudDeleteRequest nbiCrudDeleteRequest = new NbiCrudDeleteRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , scopeType, 0, filter)

        when:
        CmDeleteResponse cmDeleteResponse = objUnderTest.executor(nbiCrudDeleteRequest)
        then:
        with(cmDeleteResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            errorResponseType.error.errorInfo.contains(errorMessage)
            httpCode == 400
        }
        where:
        scopeType           ||  errorMessage
        "BASE_NTH_LEVEL"    ||  "Filter not allowed with scopeType"
        "BASE_ONLY"         ||  "Filter not allowed with scopeType"
        "BASE_SUBTREE"      ||  "Invalid scopeType=BASE_SUBTREE for DELETE operation"
        "INVALID"           ||  "The supplied parameters scopeType=INVALID is not allowed."
    }

    def 'Execute nbiCrudGetRequest with scopeLevel < 0 return fail'() {

        given: 'nbiCrudGetRequest'
        def filter = null
        int scopeLevel = -1
        NbiCrudDeleteRequest nbiCrudDeleteRequest = new NbiCrudDeleteRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , "BASE_ONLY", scopeLevel, filter)

        when:
        CmDeleteResponse cmDeleteResponse = objUnderTest.executor(nbiCrudDeleteRequest)
        then:
        with(cmDeleteResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            errorResponseType.error.errorInfo.contains("The supplied parameters scopeLevel=-1 is not allowed.")
            httpCode == 400
        }
    }

    /*
        BASE_ONLY / BASE_ALL
     */

    def 'Execute nbiCrudDeleteRequest with BASE_ONLY for SubNetwork, it returns success'() {
        given: 'nbiCrudDeleteRequest'
        NbiCrudDeleteRequest nbiCrudDeleteRequest = new NbiCrudDeleteRequest(userId, requestId, ipAddress, ssoToken, "SubNetwork=Sample" , "BASE_ONLY", 0, null)

        when:
        CmDeleteResponse cmDeleteResponse = objUnderTest.executor(nbiCrudDeleteRequest)
        then:
        with(cmDeleteResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            httpCode == 200

            def cmObjectList =  getCmObjectList(cmObjects);
            cmObjectList.get(0).fdn.contains('SubNetwork')
        }
    }

    def 'Execute nbiCrudDeleteRequest for SubNetwork with BASE_ALL return fail'() {
        given: 'nbiCrudDeleteRequest'
        NbiCrudDeleteRequest nbiCrudDeleteRequest = new NbiCrudDeleteRequest(userId, requestId, ipAddress, ssoToken, "SubNetwork=Sample" , "BASE_ALL", 0, null)

        when:
        CmDeleteResponse cmDeleteResponse = objUnderTest.executor(nbiCrudDeleteRequest)
        then:
        with(cmDeleteResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            errorResponseType.error.errorInfo.contains("to DELETE a SubNetwork. Valid value is BASE ONLY")
            httpCode == 400
        }
    }

    def 'Execute nbiCrudDeleteRequest for ENodeBFunction in SubNetwork with BASE_ALL return success'() {
        given: 'nbiCrudDeleteRequest'
        NbiCrudDeleteRequest nbiCrudDeleteRequest = new NbiCrudDeleteRequest(userId, requestId, ipAddress, ssoToken, "SubNetwork=Sample/MeContext=ERBS881/ManagedElement=1/ENodeBFunction=1" , "BASE_ALL", 0, null)

        when:
        CmDeleteResponse cmDeleteResponse = objUnderTest.executor(nbiCrudDeleteRequest)

        then:
        with(cmDeleteResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            httpCode == 200

            def cmObjectList =  getCmObjectList(cmObjects);
            cmObjectList.size() > 0
            cmObjectList.get(0).fdn.contains('SubNetwork')
        }
    }

    @Unroll
    def 'Execute nbiCrudDeleteRequest for ENodeBFunction in SubNetwork with BASE_ALL and valid #filter filter return success'(filter) {

        given: 'nbiCrudDeleteRequest'
        NbiCrudDeleteRequest nbiCrudDeleteRequest = new NbiCrudDeleteRequest(userId, requestId, ipAddress, ssoToken, "SubNetwork=Sample/MeContext=ERBS881/ManagedElement=1/ENodeBFunction=1" , "BASE_ALL", 0, filter)

        when:
        CmDeleteResponse cmDeleteResponse = objUnderTest.executor(nbiCrudDeleteRequest)

        then:
        with(cmDeleteResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            httpCode == 200

            def cmObjectList =  getCmObjectList(cmObjects);
            cmObjectList.size() > 0
            cmObjectList.get(0).fdn.contains('SubNetwork')
        }

        where:
        filter                                                                                              || _
        null                                                                                                || _
        "//EUtranCellFDD"                                                                                   || _
        "/SubNetwork/MeContext/ManagedElement/ENodeBFunction/EUtranCellFDD"                                 || _
        '/SubNetwork/MeContext/ManagedElement/ENodeBFunction/EUtranCellFDD[attributes/userLabel="value"]'   || _
    }

    @Unroll
    def 'Execute nbiCrudDeleteRequest for ENodeBFunction with BASE_ALL and valid #filterfilter return success'(filter) {

        given: 'nbiCrudDeleteRequest'
        NbiCrudDeleteRequest nbiCrudDeleteRequest = new NbiCrudDeleteRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1" , "BASE_ALL", 0, filter)

        when:
        CmDeleteResponse cmDeleteResponse = objUnderTest.executor(nbiCrudDeleteRequest)
        then:
        with(cmDeleteResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            httpCode == 200

            def cmObjectList =  getCmObjectList(cmObjects);
            cmObjectList.size() > 0
        }

        where:
        filter                                                                                  || _
        null                                                                                    || _
        "//EUtranCellFDD"                                                                       || _
        '/MeContext/ManagedElement/ENodeBFunction/EUtranCellFDD'                                || _
        '/MeContext/ManagedElement/ENodeBFunction/EUtranCellFDD[attributes/userLabel="value"]'  || _

    }

    def 'Execute nbiCrudDeleteRequest for MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=4 with BASE_ALL return success'() {
        given: 'nbiCrudDeleteRequest'
        def filter = null
        NbiCrudDeleteRequest nbiCrudDeleteRequest = new NbiCrudDeleteRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=4", "BASE_ALL", 0, filter)

        when:
        CmDeleteResponse cmDeleteResponse = objUnderTest.executor(nbiCrudDeleteRequest)
        then:
        with(cmDeleteResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            cmObjects.size() == 1
            httpCode == 200

            def cmObjectList =  getCmObjectList(cmObjects);
            cmObjectList.get(0).fdn == 'MeContext=ERBS002,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=4'
        }
    }

    def 'Execute nbiCrudDeleteRequest for MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=4 with noQuery params return success'() {
        given: 'nbiCrudDeleteRequest'
        def filter = null
        NbiCrudDeleteRequest nbiCrudDeleteRequest = new NbiCrudDeleteRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=4", null, 0, filter)

        when:
        CmDeleteResponse cmDeleteResponse = objUnderTest.executor(nbiCrudDeleteRequest)
        then:
        with(cmDeleteResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            cmObjects.size() == 0
            httpCode == 204
        }
    }

    /*
    TEST HTTP RETURN CODES
    */
    def 'Execute nbiCrudDeleteRequest with objUnderTest.executeOperation and BASE_ALL return success'() {
        given: 'nbiCrudDeleteRequest'
        def filter = null
        NbiCrudDeleteRequest nbiCrudDeleteRequest = new NbiCrudDeleteRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , "BASE_ALL", 0, filter)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudDeleteRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 200
            nbiResponse.jsonContent == '["NetworkElement=ERBS002","NetworkElement=ERBS002/ConnectivityInformation=1","NetworkElement=ERBS002/SecurityFunction=1","NetworkElement=ERBS002/SecurityFunction=1/NetworkElementSecurity=1"]'
        }
    }

    def 'Execute nbiCrudDeleteRequest with objUnderTest.executeOperation and noQuery params return success '() {
        given: 'nbiCrudDeleteRequest'
        def filter = null
        NbiCrudDeleteRequest nbiCrudDeleteRequest = new NbiCrudDeleteRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=4", null, 0, filter)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudDeleteRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 204
            nbiResponse.jsonContent == "[]"
        }
    }

    def 'Execute nbiCrudDeleteRequest with objUnderTest.executeOperation return fail'() {
        given: 'nbiCrudDeleteRequest'
        def filter = null
        NbiCrudDeleteRequest nbiCrudDeleteRequest = new NbiCrudDeleteRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=xxx" , "BASE_ONLY", 0, filter)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudDeleteRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 404
            nbiResponse.jsonContent.contains("The supplied FDN NetworkElement=xxx does not exist in the database")
        }
    }

    def 'Execute nbiCrudDeleteRequest with objUnderTest.executeOperation return fail when invalid TBAC node ERBS001'() {
        given:
        def filter = null
        NbiCrudDeleteRequest nbiCrudDeleteRequest = new NbiCrudDeleteRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS001/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=4", null, 0, filter)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudDeleteRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 401
            nbiResponse.jsonContent.contains('Access Denied')
        }
    }

    def 'Execute nbiCrudDeleteRequest with objUnderTest.executeOperation return fail when it has children'() {
        given: 'nbiCrudDeleteRequest'
        def filter = null
        NbiCrudDeleteRequest nbiCrudDeleteRequest = new NbiCrudDeleteRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002", "BASE_ONLY", 0, filter)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudDeleteRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 400
            nbiResponse.jsonContent == '{"error":{"errorInfo":"Invalid scopeType=BASE_ONLY for fdn=NetworkElement=ERBS002 in DELETE operation because there are children"}}'
        }
    }

    /*
    TEST SINGLE METHODS
*/
    def 'CmDeleteRequest and CmDeleteResponse for_coverage_only'() {
        when:
        CmDeleteRequest cmDeleteRequest = new CmDeleteRequest("fdn", ScopeType.BASE_ONLY , 0, null, false)
        CmDeleteResponse cmDeleteResponse = new CmDeleteResponse(null, null , null, 0)

        then:
        cmDeleteRequest.getScopeLevel() == 0
        cmDeleteRequest.toString()
        cmDeleteResponse.toString()
    }

    def 'dummy test to force maven to run test class with all other test methods annotated with @Unroll'() {
        when:
        objUnderTest.toString()
        then:
        1 == 1
    }

    List<CmObject> getCmObjectList(Collection<CmObject> cmObjects) {
        List<CmObject> cmObjectList = new ArrayList<CmObject>()
        cmObjectList.addAll(cmObjects);
        return  cmObjectList
    }
}