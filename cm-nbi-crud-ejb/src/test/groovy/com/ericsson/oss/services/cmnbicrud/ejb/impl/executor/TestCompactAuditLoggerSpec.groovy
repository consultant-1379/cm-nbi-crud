package com.ericsson.oss.services.cmnbicrud.ejb.impl.executor

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.rule.custom.node.NodeProperty
import com.ericsson.cds.cdi.support.rule.custom.node.RootNode
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder
import com.ericsson.oss.itpf.sdk.security.accesscontrol.EAccessControl
import com.ericsson.oss.itpf.sdk.security.accesscontrol.ESecurityTarget
import com.ericsson.oss.presentation.cmnbirest.api.*
import com.ericsson.oss.services.cm.cmsearch.DynamicBlackListInfoIf
import com.ericsson.oss.services.cm.cmsearch.excessivedata.DynamicBlackListEntry
import com.ericsson.oss.services.cm.cmshared.dto.search.CmSearchCriteria
import com.ericsson.oss.services.cmnbicrud.ejb.common.ResponseType
import com.ericsson.oss.services.cmnbicrud.ejb.common.WriteResponse
import com.ericsson.oss.services.cmnbicrud.ejb.create.CmCreateResponse
import com.ericsson.oss.services.cmnbicrud.ejb.put.CmPutResponse
import com.ericsson.oss.services.cmnbicrud.ejb.testutil.BaseForCommandReceiverSpecs
import com.ericsson.oss.services.cmnbicrud.ejb.testutil.TestUtil
import com.ericsson.oss.itpf.sdk.recording.CommandPhase
import spock.lang.Unroll

import javax.inject.Inject

class TestCompactAuditLoggerSpec extends BaseForCommandReceiverSpecs {

    @ObjectUnderTest
    private CmNbiCommandExecutor objUnderTest


    @Inject
    private TestUtil testUtil

    @Inject
    DynamicBlackListInfoIf mockedDynamicBlackListInfoIf;

    @Inject
    private EAccessControl mockedAccessControl;

    @Inject
    SystemRecorder systemRecorderMock

    final CmSearchCriteria cmSearchCriteria = new CmSearchCriteria()

    String userId = "user"
    String requestId = "crud:1234"
    String fields = null
    String attributes = null
    String filter = null
    String ipAddress = "1.2.3.5"
    String ssoToken = "CookieAABBCCDDEE";

    static def OP_TYPE_DELETE = '"opType":"delete"'
    static def OP_TYPE_UPDATE = '"opType":"update"'
    static def OP_TYPE_CREATE = '"opType":"create"'

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

        def securityTarget3 = ESecurityTarget.map('ERBS001FORNBI');
        mockedAccessControl.isAuthorized(securityTarget3) >> true
        runtimeDps.withTransactionBoundaries()

        setupDbNode()
    }

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
                .onAction('collectDynamicCellStatus').returnValue('[{"cell"":"EUtranCellFDD"="LTE05ERBS00028-1", "ue":26, "srb": 10, "drb": 12}]')
                .onAction('testSensitiveAction').returnValue(null)
                .namespace(ERBS_MODEL)
                .version(ERBS_VERSION)
                .type("ENodeBFunction").target(createMockedTarget('ERBS', '19.Q3-J.3.100'))
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

    /*
    *              NbiCrudGetRequest Section
    * */
    def 'Execute nbiCrudGetRequest with xpath=null => recordCompactAudit FINISHED_WITH_ERROR'() {
        given: 'nbiCrudGetRequest'
            def xpath = null
            NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, xpath , "BASE_ONLY", fields, attributes, 0, filter)
        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudGetRequest)
        then:
            1 * systemRecorderMock.recordCompactAudit(userId, _ , CommandPhase.FINISHED_WITH_ERROR, _, _, ipAddress, ssoToken, '{"errorDetail":"The supplied parameters xpath=null is not allowed."}')
    }

    def 'Execute nbiCrudGetRequest for NetworkElement with BASE_ONLY and attributes empty => recordCompactAudit FINISHED_WITH_SUCCESS'() {
        given: 'nbiCrudGetRequest'
            attributes = ""
            NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , "BASE_ONLY", fields, attributes, 0, filter)
        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudGetRequest)
        then:
            1 * systemRecorderMock.recordCompactAudit(userId, _ , CommandPhase.FINISHED_WITH_SUCCESS, _, _, ipAddress, ssoToken, '{"summaryResult":[{"opType":"read","entity":"ManagedObject","result":{"total":1}}]}')
    }

    def 'Execute nbiCrudGetRequest for NetworkElement with BASE_ALL and attributes=null => recordCompactAudit FINISHED_WITH_SUCCESS'() {
        given: 'nbiCrudGetRequest'
           NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , "BASE_ALL", fields, attributes, 0, filter)
        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudGetRequest)
        then:
            1 * systemRecorderMock.recordCompactAudit(userId, _ , CommandPhase.FINISHED_WITH_SUCCESS, _, _, ipAddress, ssoToken, '{"summaryResult":[{"opType":"read","entity":"ManagedObject","result":{"total":4}}]}')
    }

    @Unroll
    def 'Execute nbiCrudGetRequest for ENodeBFunction with BASE_ALL,filter!=null and attributes=null (when filterCmResponseIfNecessary (regex pattern)) => recordCompactAudit FINISHED_WITH_SUCCESS'() {
        given: 'nbiCrudGetRequest'
            def filter = '/MeContext/ENodeBFunction[attributes/USERLABEL="value"]'
            NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_ALL", fields, attributes, 0, filter)
        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudGetRequest)
        then:
            1 * systemRecorderMock.recordCompactAudit(userId, _ , CommandPhase.FINISHED_WITH_SUCCESS, _, _, ipAddress, ssoToken, '{"summaryResult":[{"opType":"read","entity":"ManagedObject","result":{"total":0}}]}')
    }

    /*
    *              nbiCrudDeleteRequest Section
    * */
    // xpath is an existing FDN with children
    def 'Execute nbiCrudDeleteRequest where xpath is an existing FDN with children => recordCompactAudit FINISHED_WITH_ERROR'() {
        given: 'nbiCrudGetRequest'
            def xpath="MeContext=ERBS002"
            def scopeType = 'BASE_ONLY'
            def filter = null
            NbiCrudDeleteRequest nbiCrudDeleteRequest = new NbiCrudDeleteRequest(userId, requestId, ipAddress, ssoToken, xpath , scopeType, 0, filter)
        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudDeleteRequest)
        then:
            1 * systemRecorderMock.recordCompactAudit(userId, _ , CommandPhase.FINISHED_WITH_ERROR, _, _, ipAddress, ssoToken, '{"errorDetail":"Invalid scopeType=BASE_ONLY for fdn=MeContext=ERBS002 in DELETE operation because there are children"}')
    }

    @Unroll
    def 'Execute nbiCrudDeleteRequest where xpath is an existing FDN with children, if scopeType is #scopeType and filter is #filter => recordCompactAudit FINISHED_WITH_SUCCESS'(scopeType, filter) {
        given: 'nbiCrudDeleteRequest'
            def xpath="MeContext=ERBS002"
            NbiCrudDeleteRequest nbiCrudDeleteRequest = new NbiCrudDeleteRequest(userId, requestId, ipAddress, ssoToken, xpath , scopeType, 0, filter)
        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudDeleteRequest)
        then:
            //output is too long so we check only some fields
            1 * systemRecorderMock.recordCompactAudit(userId, _ , CommandPhase.FINISHED_WITH_SUCCESS, _, _, ipAddress, ssoToken, {
            it -> if (it instanceof String && it.contains('"detailResult"') &&
                    it.contains(OP_TYPE_DELETE) &&
                    it.contains('"id":"MeContext=ERBS002,ManagedElement=1"') &&
                    it.contains('"id":"MeContext=ERBS002,ManagedElement=1,ENodeBFunction=1"')) {
                return true
            }
                return false
        })
        where:
        scopeType              || filter
        'BASE_ALL'             || null
        'BASE_ALL'             || '//ManagedElement'
        'BASE_NTH_LEVEL'       || null
    }

    def 'Execute nbiCrudDeleteRequest for MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=4 with noQuery params => recordCompactAudit FINISHED_WITH_SUCCESS'() {
        given: 'nbiCrudDeleteRequest'
            def filter = null
            NbiCrudDeleteRequest nbiCrudDeleteRequest = new NbiCrudDeleteRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=4", null, 0, filter)
        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudDeleteRequest)
        then:
            //output is too long so we check only some fields
            1 * systemRecorderMock.recordCompactAudit(userId, _ , CommandPhase.FINISHED_WITH_SUCCESS, _, _, ipAddress, ssoToken, {
                it -> if (it instanceof String && it.contains('"detailResult"') &&
                        it.contains(OP_TYPE_DELETE) &&
                        !it.contains('"id":"MeContext=ERBS002,ManagedElement=1"') &&
                        it.contains('"id":"MeContext=ERBS002,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=4"')) {
                    return true
                }
                    return false
            })
    }


    /*
    *              nbiCrudPutRequest (Modify) Section
    * */

    def 'Execute NbiCrudPutRequest to modify ManagedElement attribute with wrong attribute name => recordCompactAudit FINISHED_WITH_ERROR'() {
        given: 'NbiCrudPutRequest'
            def xpath = 'MeContext=ERBS002,ManagedElement=1'
            def wrongAttributeNameBody = '{ "ManagedElement": [{ "id": "1", "attributes": {"logiclName": "newLogicalName"} }] }'
            NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , wrongAttributeNameBody)

        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudPutRequest)
        then:
            1 * systemRecorderMock.recordCompactAudit(userId, _ , CommandPhase.FINISHED_WITH_ERROR, _, _, ipAddress, ssoToken, '{"errorDetail":"An unknown attribute has been encountered; name logiclName in the MO class ManagedElement."}')
    }

    def 'Execute NbiCrudPutRequest to modify NetworkElement attributes utcOffset => recordCompactAudit FINISHED_WITH_SUCCESS'() {
        given: 'NbiCrudPutRequest'
            def xpath = 'NetworkElement=ERBS002'
            def body = '{ "NetworkElement": [{ "id": "ERBS002", "attributes": {"utcOffset": "+00:20"} }] }'
            NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudPutRequest)
        then:
        //output is too long so we check only some fields
            1 * systemRecorderMock.recordCompactAudit(userId, _ , CommandPhase.FINISHED_WITH_SUCCESS, _, _, ipAddress, ssoToken, {
                it -> if (it instanceof String && it.contains('"detailResult"') &&
                        it.contains(OP_TYPE_UPDATE) &&
                        it.contains('"id":"NetworkElement=ERBS002"') &&
                        it.contains('"oldValues":{"utcOffset":"+00:18"') &&
                        it.contains('"currentValues":{"utcOffset":"+00:20"')) {
                    return true
                }
                    return false
            })
    }

    def 'Execute NbiCrudPutRequest to modify ManagedElement test => recordCompactAudit FINISHED_WITH_SUCCESS'() {
        given: 'NbiCrudPutRequest'
        def xpath = 'MeContext=ERBS002,ManagedElement=1'
        def wrongAttributeNameBody = '{"ManagedElement":{"id":"1","attributes":{"userLabel":"test"}}}'
        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , wrongAttributeNameBody)

        when:
        objUnderTest.processCommandRequest(requestId, nbiCrudPutRequest)
        then:
        1 * systemRecorderMock.recordCompactAudit(userId, _ , CommandPhase.FINISHED_WITH_SUCCESS, _, _, ipAddress, ssoToken, {
            it -> if (it instanceof String && it.contains('"detailResult"') &&
                    it.contains(OP_TYPE_UPDATE) &&
                    it.contains('"id":"MeContext=ERBS002,ManagedElement=1"') &&
                    it.contains('"oldValues":{"userLabel":null') &&
                    it.contains('"currentValues":{"userLabel":"test"')) {
                return true
            }
                return false
        })
    }

    def 'Execute NbiCrudPutRequest to modify EUtranCellFDD with sensitive attribute => recordCompactAudit FINISHED_WITH_SUCCESS'() {
        given: 'NbiCrudPutRequest'
            def xpath = 'MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=toBeRemoved'
            def body = '{ "EUtranCellFDD": [{ "id": "toBeRemoved", "attributes": {"userLabel": "newLabel", "testSensitivePersistent": "test1"} }] }'
            NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        objUnderTest.processCommandRequest(requestId, nbiCrudPutRequest)
        then:
        //output is too long so we check only some fields
        1 * systemRecorderMock.recordCompactAudit(userId, {
            it -> if (it instanceof String && it.contains('"testSensitivePersistent":"************"')) {
                return true
            }
            return false},
                CommandPhase.FINISHED_WITH_SUCCESS, _, _, ipAddress, ssoToken, {
            it -> if (it instanceof String && it.contains('"detailResult"') &&
                    it.contains(OP_TYPE_UPDATE) &&
                    it.contains('"id":"MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=toBeRemoved') &&
                    it.contains('"oldValues":{"userLabel":"value","testSensitivePersistent":"************"}') &&
                    it.contains('"currentValues":{"userLabel":"newLabel","testSensitivePersistent":"************"}')) {
                return true
            }
                return false
        })
    }

    def 'Execute NbiCrudPutRequest to modify ManagedElement attribute with wrong attribute name and sensitive => recordCompactAudit FINISHED_WITH_ERROR'() {
        given: 'NbiCrudPutRequest'
            def xpath = 'MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=toBeRemoved'
            def wrongAttributeNameBody = '{ "EUtranCellFDD": [{ "id": "toBeRemoved", "attributes": {"userrLabel": "newLabel", "testSensitivePersistent": "test1"} }] }'
            NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , wrongAttributeNameBody)

        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudPutRequest)
        then:
            1 * systemRecorderMock.recordCompactAudit(userId, {
                it -> if (it instanceof String && it.contains('"testSensitivePersistent":"************"')) {
                    return true
                }
                    return false},
                    CommandPhase.FINISHED_WITH_ERROR, _, _, ipAddress, ssoToken,'{"errorDetail":"An unknown attribute has been encountered; name userrLabel in the MO class EUtranCellFDD."}')
    }

    /*
    *              nbiCrudPutRequest (Create) Section
    * */

    def 'Execute NbiCrudPutRequest to create NetworkElement return fail when invalid attribute => recordCompactAudit FINISHED_WITH_ERROR'() {
        given: 'NbiCrudPutRequest'
            def xpath = 'NetworkElement=LTETA0001FDD'
            def body ='{ "NetworkElement": [ { "id": "LTETA0001FDD", "attributes": { "NetworkElementId":"abc", "INVALID_ATTRIBUTE":"22" , "NETYPE": "ERBS" }}] }'
            NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudPutRequest)
        then:
            1 * systemRecorderMock.recordCompactAudit(userId, _ , CommandPhase.FINISHED_WITH_ERROR, _, _, ipAddress, ssoToken, '{"errorDetail":"An unknown attribute has been encountered; name INVALID_ATTRIBUTE in the MO class NetworkElement."}')
    }

    def 'Execute NbiCrudPutRequest to create NetworkElement (with NetworkElementId) => recordCompactAudit FINISHED_WITH_SUCCESS'() {
        given: 'NbiCrudPutRequest'
            def xpath = 'NetworkElement=LTETA0001FDD'
            def body ='{ "NetworkElement": [ { "id": "LTETA0001FDD", "attributes": { "NetworkElementId": "abc", "ossPREFIX":"22" , "NETYPE": "ERBS" }}] }'
            NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudPutRequest)
        then:
        //output is too long so we check only some fields
        1 * systemRecorderMock.recordCompactAudit(userId, _ , CommandPhase.FINISHED_WITH_SUCCESS, _, _, ipAddress, ssoToken, {
            it -> if (it instanceof String && it.contains('"detailResult"') &&
                    it.contains(OP_TYPE_CREATE) &&
                    it.contains('"id":"NetworkElement=LTETA0001FDD"') &&
                    it.contains('"currentValues":{"networkElementId":"LTETA0001FDD"')) {
                return true
            }
                return false
        })
    }

    def 'Execute NbiCrudPutRequest to create EUtranCellFDD with sensitive attribute => recordCompactAudit FINISHED_WITH_SUCCESS'() {
        given: 'NbiCrudPutRequest'
            def xpath = 'MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20'
            def body ='{ "EUtranCellFDD": [ { "id": "20", "attributes": { "EUtranCellFDDId":"2022", "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testSensitivePersistent": "test1"' +
                ' }}] }'
            NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudPutRequest)
        then:
        //output is too long so we check only some fields
        1 * systemRecorderMock.recordCompactAudit(userId, {
            it -> if (it instanceof String && it.contains('"testSensitivePersistent":"************"')) {
                return true
            }
                return false},
                CommandPhase.FINISHED_WITH_SUCCESS, _, _, ipAddress, ssoToken, {
            it -> if (it instanceof String && it.contains('"detailResult"') &&
                    it.contains(OP_TYPE_CREATE) &&
                    it.contains('"id":"MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=20') &&
                    it.contains('"currentValues":{') &&
                    it.contains('"testSensitivePersistent":"************"')) {
                return true
            }
                return false
        })
    }

    def 'Execute NbiCrudPutRequest to create EUtranCellFDD with wrong attribute name and sensitive attribute => recordCompactAudit FINISHED_WITH_ERROR'() {
        given: 'NbiCrudPutRequest'
            def xpath = 'MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20'
            def body ='{ "EUtranCellFDD": [ { "id": "20", "attributes": { "EUtranCellFDDId":"2022", "userrLabel":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testSensitivePersistent": "test1"' +
                ' }}] }'
            NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudPutRequest)
        then:
        //output is too long so we check only some fields
        1 * systemRecorderMock.recordCompactAudit(userId,{
            it -> if (it instanceof String && it.contains('"testSensitivePersistent":"************"')) {
                return true
            }
                return false},
                CommandPhase.FINISHED_WITH_ERROR, _, _, ipAddress, ssoToken, '{"errorDetail":"An unknown attribute has been encountered; name userrLabel in the MO class EUtranCellFDD."}')
    }

    /*
    *              nbiCrudPostRequest Section
    * */

    @Unroll
    def 'Execute NbiCrudPostRequest to create NetworkElement return fail when invalid attribute => recordCompactAudit FINISHED_WITH_ERROR'() {
        given: 'NbiCrudPostRequest'
            def xpath = null
            def body ='{ "NetworkElement": [ { "id": "LTETA0001FDD", "attributes": { "NetworkElementId":"abc", "INVALID_ATTRIBUTE":"22" , "NETYPE": "ERBS" }}] }'
            NbiCrudPostRequest nbiCrudPostRequest = new NbiCrudPostRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudPostRequest)
        then:
            1 * systemRecorderMock.recordCompactAudit(userId, _ , CommandPhase.FINISHED_WITH_ERROR, _, _, ipAddress, ssoToken, '{"errorDetail":"An unknown attribute has been encountered; name INVALID_ATTRIBUTE in the MO class NetworkElement."}')
    }

    def 'Execute NbiCrudPostRequest to create invalid Mo EUtranCellFDDx  => recordCompactAudit FINISHED_WITH_ERROR'() {
        given: 'NbiCrudPostRequest'
        def xpath = 'MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1'
        def body ='{ "EUtranCellFDDx": [ { "id": "20", "attributes": { "EUtranCellFDDId":"2022", "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testSensitivePersistent": "test1"' +
                ' }}] }'
        NbiCrudPostRequest nbiCrudPostRequest = new NbiCrudPostRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        objUnderTest.processCommandRequest(requestId, nbiCrudPostRequest)
        then:
        1 * systemRecorderMock.recordCompactAudit(userId, {
            it -> if (it instanceof String && it.contains('N/A')) {
                return true
            }
                return false} , CommandPhase.FINISHED_WITH_ERROR, _, _, ipAddress, ssoToken, '{"errorDetail":"EUtranCellFDDx is not valid for neType ERBS and ossModelIdentity 19.Q3-J.3.100"}')
    }

    def 'Execute NbiCrudPostRequest to create NetworkElement (with NetworkElementId) => recordCompactAudit FINISHED_WITH_SUCCESS'() {
        given: 'NbiCrudPostRequest'
            def xpath = null
            def body ='{ "NetworkElement": [ { "id": "LTETA0001FDD", "attributes": { "NetworkElementId": "abc", "ossPREFIX":"22" , "NETYPE": "ERBS" }}] }'
            NbiCrudPostRequest nbiCrudPostRequest = new NbiCrudPostRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudPostRequest)
        then:
        //output is too long so we check only some fields
        1 * systemRecorderMock.recordCompactAudit(userId, _ , CommandPhase.FINISHED_WITH_SUCCESS, _, _, ipAddress, ssoToken, {
            it -> if (it instanceof String && it.contains('"detailResult"') &&
                    it.contains(OP_TYPE_CREATE) &&
                    it.contains('"id":"NetworkElement=LTETA0001FDD"') &&
                    it.contains('"currentValues":{"networkElementId":"LTETA0001FDD"')) {
                return true
            }
                return false
        })
    }

    def 'Execute NbiCrudPostRequest to create EUtranCellFDD with sensitive attribute => recordCompactAudit FINISHED_WITH_SUCCESS'() {
        given: 'NbiCrudPostRequest'
            def xpath = 'MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1'
            def body ='{ "EUtranCellFDD": [ { "id": "20", "attributes": { "EUtranCellFDDId":"2022", "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testSensitivePersistent": "test1"' +
                ' }}] }'
            NbiCrudPostRequest nbiCrudPostRequest = new NbiCrudPostRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudPostRequest)
        then:
        //output is too long so we check only some fields
        1 * systemRecorderMock.recordCompactAudit(userId, {
            it -> if (it instanceof String && it.contains('"testSensitivePersistent":"************"')) {
                return true
            }
                return false},
                CommandPhase.FINISHED_WITH_SUCCESS, _, _, ipAddress, ssoToken, {
            it -> if (it instanceof String && it.contains('"detailResult"') &&
                    it.contains(OP_TYPE_CREATE) &&
                    it.contains('"id":"MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=20') &&
                    it.contains('"currentValues":{') &&
                    it.contains('"testSensitivePersistent":"************"')) {
                return true
            }
                return false
        })
    }

    def 'Execute NbiCrudPostRequest to create EUtranCellFDD with wrong attribute name and sensitive attribute => recordCompactAudit FINISHED_WITH_ERROR'() {
        given: 'NbiCrudPostRequest'
            def xpath = 'MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1'
            def body ='{ "EUtranCellFDD": [ { "id": "20", "attributes": { "EUtranCellFDDId":"2022", "userrLabel":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testSensitivePersistent": "test1"' +
                ' }}] }'
            NbiCrudPostRequest nbiCrudPostRequest = new NbiCrudPostRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudPostRequest)
        then:
        //output is too long so we check only some fields
        1 * systemRecorderMock.recordCompactAudit(userId, {
            it -> if (it instanceof String && it.contains('"testSensitivePersistent":"************"')) {
                return true
            }
                return false},
                CommandPhase.FINISHED_WITH_ERROR, _, _, ipAddress, ssoToken, '{"errorDetail":"An unknown attribute has been encountered; name userrLabel in the MO class EUtranCellFDD."}')
    }

    /*
    *              NbiCrudPatchRequest (3gpp Json Patch) Section
    * */

    @Unroll
    def 'Execute NbiCrudPatchRequest (3GPP_JSON_PATCH) - add NetworkElement with missing or invalid attributes => recordCompactAudit FINISHED_WITH_ERROR'() {
        given: 'NbiCrudPatchRequest'
            def xpath = 'NetworkElement=LTETA0001FDD'
            NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)
        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudPatchRequest)
        then:
            1 * systemRecorderMock.recordCompactAudit(userId, _ , CommandPhase.FINISHED_WITH_ERROR, _, _, ipAddress, ssoToken, '{"errorDetail":"Not allowed json value: an invalid attributes=null has been provided for add "}')
        where:
        body                                                                                                                         | _
        '[ { "op": "add"  ,"path": "", "value": { "id": "LTETA0001FDD"} } ]' | _
        '[ { "op": "add"  ,"path": "", "value": { "id": "LTETA0001FDD", "attributes": null } } ]' | _
    }

    @Unroll
    def 'Execute NbiCrudPatchRequest (3GPP_JSON_PATCH) - add NetworkElement => recordCompactAudit FINISHED_WITH_SUCCESS'() {
        given: 'NbiCrudPatchRequest'
            NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath , body, PatchContentType.THREE_GPP_JSON_PATCH)
        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudPatchRequest)
        then:
        //output is too long so we check only some fields
        1 * systemRecorderMock.recordCompactAudit(userId, _ , CommandPhase.FINISHED_WITH_SUCCESS, _, _, ipAddress, ssoToken, {
            it -> if (it instanceof String && it.contains('"detailResult"') &&
                    it.contains(OP_TYPE_CREATE) &&
                    it.contains('"id":"NetworkElement=LTETA0001FDD"') &&
                    it.contains('"currentValues":{"networkElementId":"LTETA0001FDD"')) {
                return true
            }
                return false
        })
        where:
        xpath                                  | body | _
       'NetworkElement=LTETA0001FDD'      | '[ { "op": "add", "path": "NetworkElement=LTETA0001FDD", "value": { "id": "LTETA0001FDD", "attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" } } } ]' | _
       'NetworkElement=LTETA0001FDD'      | '[ { "op": "add", "path": "NetworkElement=LTETA0001FDDxx", "value": { "id": "LTETA0001FDD", "attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" } } } ]' | _
    }

    @Unroll
    def 'Execute NbiCrudPatchRequest (3GPP_JSON_PATCH) - modify ENodeBFunction with missing or invalid path (null or no attribute) => recordCompactAudit FINISHED_WITH_ERROR'() {
        given: 'NbiCrudPatchRequest'
            NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)
        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudPatchRequest)
        then:
            1 * systemRecorderMock.recordCompactAudit(userId, _ , CommandPhase.FINISHED_WITH_ERROR, _, _, ipAddress, ssoToken, '{"errorDetail":"The supplied parameters path=null for op=replace is not allowed."}')
        where:
        xpath | body                                                                                                                         | _
        'MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1' | '[ { "op": "replace", "value": "newUserLabel" } ] ' | _
        'MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1' | '[ { "op": "replace", "path":null, "value": "newUserLabel" } ] ' | _
    }

    @Unroll
    def 'Execute NbiCrudPatchRequest (3GPP_JSON_PATCH) - modify ENodeBFunction attribute => recordCompactAudit FINISHED_WITH_SUCCESS'() {
        given: 'NbiCrudPatchRequest'
            NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)
        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudPatchRequest)
        then:
        //output is too long so we check only some fields
        1 * systemRecorderMock.recordCompactAudit(userId, _ , CommandPhase.FINISHED_WITH_SUCCESS, _, _, ipAddress, ssoToken, {
            it -> if (it instanceof String && it.contains('"detailResult"') &&
                    it.contains(OP_TYPE_UPDATE) &&
                    it.contains('"id":"MeContext=ERBS002,ManagedElement=1,ENodeBFunction=1"') &&
                    it.contains('"oldValues":{"userLabel":"value"') &&
                    it.contains('"currentValues":{"userLabel":"newUserLabel"')) {
                return true
            }
                return false
        })
        where:
        xpath      |  body                                                                                                                                                                       | _
        "MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1"      | '[ { "op": "replace", "path":"/MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1#attributes/userlabel", "value": "newUserLabel" } ] ' | _
        "MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1"      | '[ { "op": "replace", "path":"/MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1#/attributes/userlabel", "value": "newUserLabel" } ] ' | _
    }

    @Unroll
    def 'Execute NbiCrudPatchRequest (3GPP_JSON_PATCH) - remove with invalid fdn => recordCompactAudit FINISHED_WITH_ERROR'() {
        given: 'NbiCrudPatchRequest'
            NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)
        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudPatchRequest)
        then:
            1 * systemRecorderMock.recordCompactAudit(userId, _ , CommandPhase.FINISHED_WITH_ERROR, _, _, ipAddress, ssoToken, '{"errorDetail":"The supplied FDN MeContext=ERBS002,ManagedElement=1,ENodeBFunction=xxx does not exist in the database"}')
        where:
        xpath | body                                                                                                                         | _
        "MeContext=ERBS002/ManagedElement=1/ENodeBFunction=xxx"      | '[ { "op": "remove", "path":"/MeContext=ERBS002/ManagedElement=1/ENodeBFunction=xxx" } ] ' | _
    }

    @Unroll
    def 'Execute NbiCrudPatchRequest (3GPP_JSON_PATCH) - remove EUtranCellFDD => recordCompactAudit FINISHED_WITH_SUCCESS'() {
        given: 'NbiCrudPatchRequest'
          NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)
        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudPatchRequest)
        then:
        //output is too long so we check only some fields
        1 * systemRecorderMock.recordCompactAudit(userId, _ , CommandPhase.FINISHED_WITH_SUCCESS, _, _, ipAddress, ssoToken, {
            it -> if (it instanceof String && it.contains('"detailResult"') &&
                    it.contains(OP_TYPE_DELETE) &&
                    it.contains('"id":"MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=toBeRemoved"')) {
                return true
            }
                return false
        })
        where:
        xpath                                                                        | body                                                                                                                                                                                                                                                                                                                                                         | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=toBeRemoved"      | '[ { "op": "remove", "path": "/MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=toBeRemoved" } ]' | _
    }

    @Unroll
    def 'Execute NbiCrudPatchRequest (3GPP_JSON_PATCH) - Execute remove ENodeBFunction (with child) => recordCompactAudit FINISHED_WITH_SUCCESS'() {
        given: 'NbiCrudPatchRequest'
            NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)
        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudPatchRequest)
        then:
        //output is too long so we check only some fields
        1 * systemRecorderMock.recordCompactAudit(userId, _ , CommandPhase.FINISHED_WITH_SUCCESS, _, _, ipAddress, ssoToken, {
            it -> if (it instanceof String && it.contains('"detailResult"') &&
                    it.contains(OP_TYPE_DELETE) &&
                    it.contains('"id":"MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1"') &&
                    it.contains('"id":"MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=toBeRemoved"')) {
                return true
            }
                return false
        })
        where:
        xpath     | body                                                                                                                                                                                                                                                                                                                                                         | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1"      | '[ { "op": "remove", "path": "/MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1" } ]' | _
    }

    @Unroll
    def 'Execute NbiCrudPatchRequest (3GPP_JSON_PATCH) - add (with sensitive attribute)/replace/remove EUtranCellFDD => recordCompactAudit FINISHED_WITH_SUCCESS'() {
        given: 'NbiCrudPatchRequest'
            NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)
        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudPatchRequest)
        then:
        //output is too long so we check only some fields
        1 * systemRecorderMock.recordCompactAudit(userId, _ ,
                CommandPhase.FINISHED_WITH_SUCCESS, _, _, ipAddress, ssoToken, {
            it -> if (it instanceof String && it.contains('"detailResult"') &&
                    it.contains(OP_TYPE_CREATE) &&
                    it.contains('"id":"MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=20') &&
                    it.contains('"currentValues":{') &&
                    it.contains('"testSensitivePersistent":"************"') &&
                    it.contains(OP_TYPE_UPDATE) &&
                    it.contains('"id":"MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1') &&
                    it.contains('"oldValues":{"userLabel":"value"}') &&
                    it.contains('"currentValues":{"userLabel":"newUserLabel"}') &&
                    it.contains(OP_TYPE_DELETE) &&
                    it.contains('"id":"MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=toBeRemoved')) {
                return true
            }
                return false
        })
        where:
        xpath                                                                        | body                                                                                                                                                                                                                                                                                                                                                         | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1" | '[ { "op": "add", "path": "/EUtranCellFDD=20", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test", "testSensitivePersistent": "test1" } } }, { "op": "replace", "path":"#attributes/userlabel", "value": "newUserLabel" }, { "op": "remove", "path": "/EUtranCellFDD=toBeRemoved" } ]' | _
    }

    @Unroll
    def 'Execute NbiCrudPatchRequest (3GPP_JSON_PATCH) - add and replace (with sensitive attribute)/remove EUtranCellFDD => recordCompactAudit FINISHED_WITH_SUCCESS'() {
        given: 'NbiCrudPatchRequest'
            NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)
        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudPatchRequest)
        then:
        //output is too long so we check only some fields
        1 * systemRecorderMock.recordCompactAudit(userId, _ ,
                CommandPhase.FINISHED_WITH_SUCCESS, _, _, ipAddress, ssoToken, {
            it -> if (it instanceof String && it.contains('"detailResult"') &&
                    it.contains(OP_TYPE_CREATE) &&
                    it.contains('"id":"MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=20') &&
                    it.contains('"currentValues":{') &&
                    it.contains('"testSensitivePersistent":"************"') &&
                    it.contains(OP_TYPE_UPDATE) &&
                    it.contains('"id":"MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=20') &&
                    it.contains('"oldValues":{"testSensitivePersistent":"************"}') &&
                    it.contains('"currentValues":{"testSensitivePersistent":"************"}') &&
                    it.contains(OP_TYPE_DELETE) &&
                    it.contains('"id":"MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=toBeRemoved')) {
                return true
            }
                return false
        })
        where:
        xpath                                                                        | body                                                                                                                                                                                                                                                                                                                                                         | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1" | '[ { "op": "add", "path": "/EUtranCellFDD=20", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test", "testSensitivePersistent": "test1" } } }, { "op": "replace", "path":"/EUtranCellFDD=20#attributes/testSensitivePersistent", "value": "test2" }, { "op": "remove", "path": "/EUtranCellFDD=toBeRemoved" } ]' | _
    }

    @Unroll
    def 'Execute NbiCrudPatchRequest (3GPP_JSON_PATCH) - add/replace/remove EUtranCellFDD if error in last remove request => recordCompactAudit FINISHED_WITH_ERROR'() {
        given: 'NbiCrudPatchRequest'
            NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)
        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudPatchRequest)
        then:
            1 * systemRecorderMock.recordCompactAudit(userId, _ , CommandPhase.FINISHED_WITH_ERROR, _, _, ipAddress, ssoToken, '{"errorDetail":"Execution Error The supplied FDN MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=xxx does not exist in the database"}')
        where:
        xpath                                                                        | body                                                                                                                                                                                                                                                                                                                                                         | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1" | '[ { "op": "add", "path": "/MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } }, { "op": "replace", "path":"/MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1#attributes/userlabel", "value": "newUserLabel" }, { "op": "remove", "path": "/MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=xxx" } ]' | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1" | '[ { "op": "add", "path": "/EUtranCellFDD=20", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } }, { "op": "replace", "path":"#attributes/userlabel", "value": "newUserLabel" }, { "op": "remove", "path": "/EUtranCellFDD=xxx" } ]' | _
    }

    /*
    *              NbiCrudPatchRequest (Json Patch) Section
    * */

    @Unroll
    def 'Execute NbiCrudPatchRequest (JSON_PATCH) - with xpath=null => recordCompactAudit FINISHED_WITH_ERROR'() {
        given: 'NbiCrudPatchRequest'
            NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath , body, PatchContentType.JSON_PATCH)
        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudPatchRequest)
        then:
            1 * systemRecorderMock.recordCompactAudit(userId, _ , CommandPhase.FINISHED_WITH_ERROR, _, _, ipAddress, ssoToken, '{"errorDetail":"The supplied parameters xpath=null is not allowed."}')
        where:
        xpath                                  | body | _
        null   | '[ { "op": "add", "path": "" , "value": { "id": "LTETA0001FDD", "attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" } } } ]' | _
    }

    @Unroll
    def 'Execute NbiCrudPatchRequest (JSON_PATCH) - add with invalid fdn => recordCompactAudit FINISHED_WITH_ERROR'() {
        given: 'NbiCrudPatchRequest'
            NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.JSON_PATCH)
        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudPatchRequest)
        then:
            1 * systemRecorderMock.recordCompactAudit(userId, _ , CommandPhase.FINISHED_WITH_ERROR, _, _, ipAddress, ssoToken, '{"errorDetail":"The supplied FDN MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=xxx does not exist in the database"}')
        where:
        xpath | body                                                                                                                         | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=xxx/EUtranCellFDD=20"  | '[ { "op": "add", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } } ]' | _
    }

    @Unroll
    def 'Execute NbiCrudPatchRequest (JSON_PATCH) - add NetworkElement => recordCompactAudit FINISHED_WITH_SUCCESS'() {
        given: 'NbiCrudPatchRequest'
            NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath , body, PatchContentType.JSON_PATCH)
        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudPatchRequest)
        then:
        //output is too long so we check only some fields
        1 * systemRecorderMock.recordCompactAudit(userId, _ , CommandPhase.FINISHED_WITH_SUCCESS, _, _, ipAddress, ssoToken, {
            it -> if (it instanceof String && it.contains('"detailResult"') &&
                    it.contains(OP_TYPE_CREATE) &&
                    it.contains('"id":"NetworkElement=LTETA0001FDD"') &&
                    it.contains('"currentValues":{"networkElementId":"LTETA0001FDD"')) {
                return true
            }
                return false
        })
        where:
        xpath                                  | body | _
        'NetworkElement=LTETA0001FDD'      | '[ { "op": "add", "value": { "id": "LTETA0001FDD", "attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" } } } ]' | _
    }

    @Unroll
    def 'Execute NbiCrudPatchRequest (JSON_PATCH) - modify ENodeBFunction with missing or invalid path (null or no attribute) => recordCompactAudit FINISHED_WITH_ERROR'() {
        given: 'NbiCrudPatchRequest'
            NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.JSON_PATCH)
        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudPatchRequest)
        then:
            1 * systemRecorderMock.recordCompactAudit(userId, _ , CommandPhase.FINISHED_WITH_ERROR, _, _, ipAddress, ssoToken, '{"errorDetail":"The supplied parameters path=null for op=replace is not allowed."}')
        where:
        xpath | body                                                                                                                         | _
        'MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1' | '[ { "op": "replace", "value": "newUserLabel" } ] ' | _
    }

    @Unroll
    def 'Execute NbiCrudPatchRequest (JSON_PATCH) - modify ENodeBFunction attribute => recordCompactAudit FINISHED_WITH_SUCCESS'() {
        given: 'NbiCrudPatchRequest'
            NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.JSON_PATCH)
        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudPatchRequest)
        then:
        //output is too long so we check only some fields
        1 * systemRecorderMock.recordCompactAudit(userId, _ , CommandPhase.FINISHED_WITH_SUCCESS, _, _, ipAddress, ssoToken, {
            it -> if (it instanceof String && it.contains('"detailResult"') &&
                    it.contains(OP_TYPE_UPDATE) &&
                    it.contains('"id":"MeContext=ERBS002,ManagedElement=1,ENodeBFunction=1"') &&
                    it.contains('"oldValues":{"userLabel":"value"') &&
                    it.contains('"currentValues":{"userLabel":"newUserLabel"')) {
                return true
            }
                return false
        })
        where:
        xpath      |  body                                                                                                                                                                       | _
        "MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1"      | '[ { "op": "replace", "path":"attributes/userlabel", "value": "newUserLabel" } ] ' | _
        "MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1"      | '[ { "op": "replace", "path":"/attributes/userlabel", "value": "newUserLabel" } ] ' | _
    }

    @Unroll
    def 'Execute NbiCrudPatchRequest (JSON_PATCH) - remove with invalid fdn => recordCompactAudit FINISHED_WITH_ERROR'() {
        given: 'NbiCrudPatchRequest'
            NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.JSON_PATCH)
        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudPatchRequest)
        then:
            1 * systemRecorderMock.recordCompactAudit(userId, _ , CommandPhase.FINISHED_WITH_ERROR, _, _, ipAddress, ssoToken, '{"errorDetail":"The supplied FDN MeContext=ERBS002,ManagedElement=1,ENodeBFunction=xxx does not exist in the database"}')
        where:
        xpath | body                                                                                                                         | _
        "MeContext=ERBS002/ManagedElement=1/ENodeBFunction=xxx"      | '[ { "op": "remove", "path":"" } ] ' | _
    }

    @Unroll
    def 'Execute NbiCrudPatchRequest (JSON_PATCH) - remove ENodeBFunction => recordCompactAudit FINISHED_WITH_SUCCESS'() {
        given: 'NbiCrudPatchRequest'
           NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.JSON_PATCH)
        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudPatchRequest)
        then:
        //output is too long so we check only some fields
        1 * systemRecorderMock.recordCompactAudit(userId, _ , CommandPhase.FINISHED_WITH_SUCCESS, _, _, ipAddress, ssoToken, {
            it -> if (it instanceof String && it.contains('"detailResult"') &&
                    it.contains(OP_TYPE_DELETE) &&
                    it.contains('"id":"MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=toBeRemoved"')) {
                return true
            }
                return false
        })
        where:
        xpath                                                          | body                                                                                                                                                                                                                                                                                                                                                         | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=toBeRemoved"    | '[ { "op": "remove"} ]' | _
    }

    @Unroll
    def 'Execute NbiCrudPatchRequest (JSON_PATCH) - add (with sensitive attribute)/replace EUtranCellFDD => recordCompactAudit FINISHED_WITH_SUCCESS'() {
        given: 'NbiCrudPatchRequest'
            NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.JSON_PATCH)
        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudPatchRequest)
        then:
        //output is too long so we check only some fields
        1 * systemRecorderMock.recordCompactAudit(userId, _ ,
                CommandPhase.FINISHED_WITH_SUCCESS, _, _, ipAddress, ssoToken, {
            it -> if (it instanceof String && it.contains('"detailResult"') &&
                    it.contains(OP_TYPE_CREATE) &&
                    it.contains('"id":"MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=20') &&
                    it.contains('"currentValues":{') &&
                    it.contains('"testSensitivePersistent":"************"') &&
                    it.contains(OP_TYPE_UPDATE) &&
                    it.contains('"id":"MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=20') &&
                    it.contains('"oldValues":{"userLabel":"22"}') &&
                    it.contains('"currentValues":{"userLabel":"newUserLabel"}')) {
                return true
            }
                return false
        })
        where:
        xpath                                                                        | body                                                                                                                                                                                                                                                                                                                                                         | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20" | '[ { "op": "add", "path": "", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test", "testSensitivePersistent": "test1" } } }, { "op": "replace", "path":"attributes/userlabel", "value": "newUserLabel" } ]' | _
      }


    @Unroll
    def 'Execute NbiCrudPatchRequest (JSON_PATCH) - add and replace (with sensitive attribute) EUtranCellFDD => recordCompactAudit FINISHED_WITH_SUCCESS'() {
        given: 'NbiCrudPatchRequest'
            NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.JSON_PATCH)
        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudPatchRequest)
        then:
        //output is too long so we check only some fields
        1 * systemRecorderMock.recordCompactAudit(userId, _ ,
                CommandPhase.FINISHED_WITH_SUCCESS, _, _, ipAddress, ssoToken, {
            it -> if (it instanceof String && it.contains('"detailResult"') &&
                    it.contains(OP_TYPE_CREATE) &&
                    it.contains('"id":"MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=20') &&
                    it.contains('"currentValues":{') &&
                    it.contains('"testSensitivePersistent":"************"') &&
                    it.contains(OP_TYPE_UPDATE) &&
                    it.contains('"id":"MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=20') &&
                    it.contains('"oldValues":{"testSensitivePersistent":"************"}') &&
                    it.contains('"currentValues":{"testSensitivePersistent":"************"}')) {
                return true
            }
                return false
        })
        where:
        xpath                                                                        | body                                                                                                                                                                                                                                                                                                                                                         | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20" | '[ { "op": "add", "path": "", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test", "testSensitivePersistent": "test1" } } }, { "op": "replace", "path":"attributes/testSensitivePersistent", "value": "test2" } ]' | _
    }

    @Unroll
    def 'Execute NbiCrudPatchRequest (JSON_PATCH) - add/replace EUtranCellFDD return fail if error in last replace request (invalidAttr)'() {
        given: 'NbiCrudPatchRequest'
            NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.JSON_PATCH)
        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudPatchRequest)
        then:
            1 * systemRecorderMock.recordCompactAudit(userId, _ , CommandPhase.FINISHED_WITH_ERROR, _, _, ipAddress, ssoToken, '{"errorDetail":"An unknown attribute has been encountered; name invalidAttr in the MO class EUtranCellFDD."}')
        where:
        xpath                                                                        | body                                                                                                                                                                                                                                                                                                                                                         | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20" | '[ { "op": "add", "path": "", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } }, { "op": "replace", "path":"attributes/invalidAttr", "value": "newUserLabel" } ]' | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20" | '[ { "op": "add", "path": "", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } }, { "op": "replace", "path":"/attributes/invalidAttr", "value": "newUserLabel" } ]' | _
    }


    /*
    *              nbiCrudActionRequest Section
    * */

    @Unroll
    def 'Execute NbiCrudPatchActionRequest fails => recordCompactAudit FINISHED_WITH_ERROR'() {
        given: 'NbiCrudPatchActionRequest'
            def action = "updateMMEConnection"
            NbiCrudActionRequest nbiCrudPatchActionRequest = new NbiCrudActionRequest(userId, requestId, ipAddress, ssoToken, xpath ,action , body)

        when:
            objUnderTest.processCommandRequest(requestId, nbiCrudPatchActionRequest)
        then:
            1 * systemRecorderMock.recordCompactAudit(userId, _ , CommandPhase.FINISHED_WITH_ERROR, _, _, ipAddress, ssoToken, summary)

        where:
            xpath                                                          |                body                                   | summary
            // wrong FDN
            'MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=11'   | '{ "input": { "orderCode":"RENEW_ALL_EXISTING" } }'   | '{"errorDetail":"The supplied FDN MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=11 does not exist in the database"}'
            // missing body
            'MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1'    | ''                                                    | '{"errorDetail":"updateMMEConnection requires the following mandatory attribute(s): (orderCode)"}'
            'MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1'    | null                                                  | '{"errorDetail":"updateMMEConnection requires the following mandatory attribute(s): (orderCode)"}'
    }

    @Unroll
    def 'Execute NbiCrudPatchActionRequest with correct params return success => recordCompactAudit FINISHED_WITH_SUCCESS'() {
        given: 'NbiCrudPatchActionRequest'
        NbiCrudActionRequest nbiCrudPatchActionRequest = new NbiCrudActionRequest(userId, requestId, ipAddress, ssoToken, xpath, actionName, body)

        when:
        objUnderTest.processCommandRequest(requestId, nbiCrudPatchActionRequest)
        then:
        1 * systemRecorderMock.recordCompactAudit(userId, _ , CommandPhase.FINISHED_WITH_SUCCESS, _, _, ipAddress, ssoToken, summary)

        where:
        xpath                                                                                       | actionName            | body                                                                 | summary
        "MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1"                                 | "collectDynamicCellStatus" | '{ "input": { "dynamicCellStatusOutput":"FORMATTED_TEXT" } }'   | '{"detailResult":[{"opType":"action","id":"MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1","currentValues":{"RETURN VALUE":"[{\\"cell\\"\\":\\"EUtranCellFDD\\"=\\"LTE05ERBS00028-1\\", \\"ue\\":26, \\"srb\\": 10, \\"drb\\": 12}]"}}]}'
        "MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=ERBS001FORNBI-1"   | "changeFrequency"     | '{ "input": { "earfcn":5 } }'                                        | ''
    }

    @Unroll
    def 'Execute NbiCrudPatchActionRequest with correct params and sensitive action parameters return success => recordCompactAudit FINISHED_WITH_SUCCESS with body obscured'() {
        given: 'NbiCrudPatchActionRequest'
        NbiCrudActionRequest nbiCrudPatchActionRequest = new NbiCrudActionRequest(userId, requestId, ipAddress, ssoToken, xpath, actionName, body)

        when:
        objUnderTest.processCommandRequest(requestId, nbiCrudPatchActionRequest)
        then:
        1 * systemRecorderMock.recordCompactAudit(userId,
                {
                    it -> if (it instanceof String && it.contains('"testSensitiveParameter":"************"')) {
                        return true
                    }
                        return false},
                CommandPhase.FINISHED_WITH_SUCCESS, _, _, ipAddress, ssoToken, _)

        where:
        xpath                                                               | actionName                | body
        "MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1"         | "testSensitiveAction"     | '{ "input": { "testSensitiveParameter": "UNFORMATTED_TEXT" } }'
    }

    def 'dummy test to force maven to run test class with all other test methods annotated with @Unroll'() {
        when:
        objUnderTest.toString()
        then:
        true
    }
}
