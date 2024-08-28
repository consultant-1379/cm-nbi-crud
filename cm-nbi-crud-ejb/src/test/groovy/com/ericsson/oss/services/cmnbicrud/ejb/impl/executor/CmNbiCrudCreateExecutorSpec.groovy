/*
    This class, and model class are copied from SetCommandSpec.groovy
 */
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
import com.ericsson.oss.presentation.cmnbirest.api.OperationType
import com.ericsson.oss.services.cm.cmsearch.DynamicBlackListInfoIf
import com.ericsson.oss.services.cm.cmsearch.excessivedata.DynamicBlackListEntry
import com.ericsson.oss.services.cm.cmshared.dto.CmResponse
import com.ericsson.oss.services.cm.cmshared.dto.search.CmSearchCriteria
import com.ericsson.oss.services.cmnbicrud.ejb.common.ResponseType
import com.ericsson.oss.services.cmnbicrud.ejb.create.CmCreateRequest
import com.ericsson.oss.services.cmnbicrud.ejb.create.CmCreateResponse
import com.ericsson.oss.services.cmnbicrud.ejb.testutil.BaseForCommandReceiverSpecs
import com.ericsson.oss.services.cmnbicrud.ejb.testutil.TestUtil
import spock.lang.Unroll

import javax.inject.Inject

class CmNbiCrudCreateExecutorSpec extends BaseForCommandReceiverSpecs {
    @ObjectUnderTest
    private CmNbiCrudCreateExecutor objUnderTest

    @Inject
    private TestUtil testUtil

    @Inject
    DynamicBlackListInfoIf mockedDynamicBlackListInfoIf;

    @Inject
    private EAccessControl mockedAccessControl;


    final CmSearchCriteria cmSearchCriteria = new CmSearchCriteria()

    public static final ACCESS_ALL = null
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

    /*
    *                 POST COMMAND REQUEST
    *
    * */

    /*
      Root Mo  (parent = null)
     */
    @Unroll
    def 'POST - Execute create NetworkElement return success'() {
        given: 'NbiCrudPostRequest'
        def xpath = null
        NbiCrudPostRequest nbiCrudPostRequest = new NbiCrudPostRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        CmCreateResponse cmCreateResponse = objUnderTest.executor(nbiCrudPostRequest)
        then:
        with(cmCreateResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 201
            def resource = ((List)moObjects.moObjects.get("NetworkElement")).iterator().next()
            with (resource) {
                id == 'LTETA0001FDD'

                attributes.get("networkElementId") == id
                attributes.get("neType") == 'ERBS'
                attributes.get("ossPrefix") == '22'
                !attributes.containsKey("technologyDomain")
            }
        }

        where:
        body | _
        '{ "NetworkElement": [ { "id": "LTETA0001FDD", "attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" }}] }' | _
        '{ "NetworkElement": { "id": "LTETA0001FDD", "attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" }}}' | _
    }

    def 'POST - Execute create NetworkElement (with NetworkElementId) return success'() {
        given: 'NbiCrudPostRequest'
        def xpath = null
        def body ='{ "NetworkElement": [ { "id": "LTETA0001FDD", "attributes": { "NetworkElementId": "abc", "ossPREFIX":"22" , "NETYPE": "ERBS" }}] }'
        NbiCrudPostRequest nbiCrudPostRequest = new NbiCrudPostRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        CmCreateResponse cmCreateResponse = objUnderTest.executor(nbiCrudPostRequest)
        then:
        with(cmCreateResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 201
            def resource = ((List)moObjects.moObjects.get("NetworkElement")).iterator().next()
            with (resource) {
                id == 'LTETA0001FDD'

                attributes.get("networkElementId") == 'LTETA0001FDD'
                attributes.get("neType") == 'ERBS'
                attributes.get("ossPrefix") == '22'
                !attributes.containsKey("technologyDomain")
            }
        }
    }

    def 'POST - Execute create SubNetwork return success'() {
        given: 'NbiCrudPostRequest'
        def xpath = null
        def body ='{ "SubNetwork": [ { "id": "aa", "attributes": { "SubNetworkId": "bb" }}] }'
        NbiCrudPostRequest nbiCrudPostRequest = new NbiCrudPostRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        CmCreateResponse cmCreateResponse = objUnderTest.executor(nbiCrudPostRequest)
        then:
        with(cmCreateResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 201
            def resource = ((List)moObjects.moObjects.get("SubNetwork")).iterator().next()
            with (resource) {
                id == 'aa'

                attributes.get("SubNetworkId") == 'aa'
            }
        }
    }


    /*
        Root Mo  (parent != null and target = null)
     */
    def 'POST - Execute create MeContext under SubNetwork return success'() {
        given: 'NbiCrudPostRequest'
        def xpath = 'SubNetwork=Sample'
        def body ='{ "MeContext": [ { "id": "ERBS_node", "attributes": { "MEContextId": "bb", "neTYpe":"ERBS", "testObsoletePersistent":"test" }}] }'
        NbiCrudPostRequest nbiCrudPostRequest = new NbiCrudPostRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        CmCreateResponse cmCreateResponse = objUnderTest.executor(nbiCrudPostRequest)
        then:
        with(cmCreateResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 201
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                id == 'ERBS_node'

                attributes.get("MeContextId") == 'ERBS_node'
                attributes.get("neType") == 'ERBS'
                attributes.get("testObsoletePersistent") == null
            }
        }
    }

    def 'POST - Execute create CmFunction under NetworkElement return fail cause System Created'() {
        given: 'NbiCrudPostRequest'
        def xpath = 'NetworkElement=ERBS001FORNBI'
        def body ='{ "CmFunction": [ { "id": "1", "attributes": { }}] }'
        NbiCrudPostRequest nbiCrudPostRequest = new NbiCrudPostRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        CmCreateResponse cmCreateResponse = objUnderTest.executor(nbiCrudPostRequest)
        then:
        with(cmCreateResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            moObjects == null
            errorResponseType.error.errorInfo.contains("Execution prohibited")
            httpCode == 417
        }
    }


    /*
      Managed Mo (parent != null and target != null)
     */
    def 'POST - Execute create EUtranCellFDD return success'() {
        given: 'NbiCrudPostRequest'
        def xpath = 'MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1'
        def body ='{ "EUtranCellFDD": [ { "id": "20", "attributes": { "EUtranCellFDDId":"2022", "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test"' +
                ' }}] }'
        NbiCrudPostRequest nbiCrudPostRequest = new NbiCrudPostRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        CmCreateResponse cmCreateResponse = objUnderTest.executor(nbiCrudPostRequest)
        then:
        with(cmCreateResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 201
            def resource = ((List)moObjects.moObjects.get("EUtranCellFDD")).iterator().next()
            with (resource) {
                id == '20'

                attributes.get("EUtranCellFDDId") == '20'
                attributes.get("userLabel") == '22'
                attributes.get("earfcndl") == 3
                attributes.get("earfcnul") == 18000
            }
        }
    }

    @Unroll
    def 'POST - Execute create NetworkElement (with id null) return fail (checkConstraints fail on NetworkElementId)'() {
        given: 'NbiCrudPostRequest'
        def xpath = null
        def body ='{ "NetworkElement": [ { "id":'+ identifier +', "attributes": { "NetworkElementId":"abc", "ossPREFIX":"22" , "NETYPE": "ERBS" }}] }'
        NbiCrudPostRequest nbiCrudPostRequest = new NbiCrudPostRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        CmCreateResponse cmCreateResponse = objUnderTest.executor(nbiCrudPostRequest)
        then:
        with(cmCreateResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            moObjects == null
           //errorResponseType.error.errorInfo.contains("Constraint violation: value specified for attribute networkElementId is not valid")
            httpCode == 417
        }

        where:
        identifier      | _
        '"null"' |_
        null   |_
    }

    @Unroll
    def 'POST - Execute create NetworkElement return fail when invalid attribute'() {
        given: 'NbiCrudPostRequest'
        def xpath = null
        def body ='{ "NetworkElement": [ { "id": "LTETA0001FDD", "attributes": { "NetworkElementId":"abc", "INVALID_ATTRIBUTE":"22" , "NETYPE": "ERBS" }}] }'
        NbiCrudPostRequest nbiCrudPostRequest = new NbiCrudPostRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        CmCreateResponse cmCreateResponse = objUnderTest.executor(nbiCrudPostRequest)
        then:
        with(cmCreateResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            moObjects == null
            errorResponseType.error.errorInfo.contains("An unknown attribute has been encountered")
            httpCode == 417
        }
    }

    @Unroll
    def 'POST - Execute create EUtranCellFDD (with id null) return return success when EUtranCellFDDId specified'() {
        given: 'NbiCrudPostRequest'
        def xpath = 'MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1'
        def body ='{ "EUtranCellFDD": [ { "id": '+ identifier + ', "attributes": { "EUtranCellFDDId":"2022", "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test"' +
                ' }}] }'
        NbiCrudPostRequest nbiCrudPostRequest = new NbiCrudPostRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        CmCreateResponse cmCreateResponse = objUnderTest.executor(nbiCrudPostRequest)
        then:
        with(cmCreateResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 201
            def resource = ((List)moObjects.moObjects.get("EUtranCellFDD")).iterator().next()
            with (resource) {
                id == '1'

                attributes.get("EUtranCellFDDId") == '2022'
                attributes.get("userLabel") == '22'
                attributes.get("earfcndl") == 3
                attributes.get("earfcnul") == 18000
            }
        }

        where:
        identifier      | _
        '"null"' |_
        null   |_

    }

    /*
    *                 PUT (CREATE) COMMAND REQUEST
    *
    * */

    /*
      Root Mo  (parent = null)
     */
    def 'PUT - Execute create NetworkElement return success'() {
        given: 'NbiCrudPutRequest'
        def xpath = 'NetworkElement=LTETA0001FDD'
        def body ='{ "NetworkElement": [ { "id": "LTETA0001FDD", "attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" }}] }'
        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        CmCreateResponse cmCreateResponse = objUnderTest.executor(nbiCrudPutRequest)
        then:
        with(cmCreateResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 201
            def resource = ((List)moObjects.moObjects.get("NetworkElement")).iterator().next()
            with (resource) {
                id == 'LTETA0001FDD'

                attributes.get("networkElementId") == id
                attributes.get("neType") == 'ERBS'
                attributes.get("ossPrefix") == '22'
                !attributes.containsKey("technologyDomain")
            }
        }
    }

    @Unroll
    def 'PUT - Execute create NetworkElement (with NetworkElementId) return success'() {
        given: 'NbiCrudPutRequest'
        def xpath = 'NetworkElement=LTETA0001FDD'
        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        CmCreateResponse cmCreateResponse = objUnderTest.executor(nbiCrudPutRequest)
        then:
        with(cmCreateResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 201
            def resource = ((List)moObjects.moObjects.get("NetworkElement")).iterator().next()
            with (resource) {
                id == 'LTETA0001FDD'

                attributes.get("networkElementId") == 'LTETA0001FDD'
                attributes.get("neType") == 'ERBS'
                attributes.get("ossPrefix") == '22'
                !attributes.containsKey("technologyDomain")
            }
        }

        where:
        body | _
        '{ "NetworkElement": [ { "id": "LTETA0001FDD", "attributes": { "NetworkElementId": "abc", "ossPREFIX":"22" , "NETYPE": "ERBS" }}] }' | _
        '{ "NetworkElement": { "id": "LTETA0001FDD", "attributes": { "NetworkElementId": "abc", "ossPREFIX":"22" , "NETYPE": "ERBS" }} }' | _
    }

    def 'PUT - Execute create SubNetwork return success'() {
        given: 'NbiCrudPutRequest'
        def xpath = 'SubNetwork=aa'
        def body ='{ "SubNetwork": [ { "id": "aa", "attributes": { "SubNetworkId": "bb" }}] }'
        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        CmCreateResponse cmCreateResponse = objUnderTest.executor(nbiCrudPutRequest)
        then:
        with(cmCreateResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 201
            def resource = ((List)moObjects.moObjects.get("SubNetwork")).iterator().next()
            with (resource) {
                id == 'aa'

                attributes.get("SubNetworkId") == 'aa'
            }
        }
    }


    /*
        Root Mo  (parent != null and target = null)
     */
    def 'PUT - Execute create MeContext under SubNetwork return success'() {
        given: 'NbiCrudPutRequest'
        def xpath = 'SubNetwork=Sample/MeContext=ERBS_node'
        def body ='{ "MeContext": [ { "id": "ERBS_node", "attributes": { "MEContextId": "bb", "neTYpe":"ERBS", "testObsoletePersistent":"test" }}] }'
        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        CmCreateResponse cmCreateResponse = objUnderTest.executor(nbiCrudPutRequest)
        then:
        with(cmCreateResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 201
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                id == 'ERBS_node'

                attributes.get("MeContextId") == 'ERBS_node'
                attributes.get("neType") == 'ERBS'
                attributes.get("testObsoletePersistent") == null
            }
        }
    }

    def 'PUT - Execute create CmFunction under NetworkElement return fail cause System Created'() {
        given: 'NbiCrudPutRequest'
        def xpath = 'NetworkElement=ERBS001FORNBI/CmFunction=1'
        def body ='{ "CmFunction": [ { "id": "1", "attributes": { }}] }'
        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        CmCreateResponse cmCreateResponse = objUnderTest.executor(nbiCrudPutRequest)
        then:
        with(cmCreateResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            moObjects == null
            errorResponseType.error.errorInfo.contains("Execution prohibited")
            httpCode == 417
        }
    }


    /*
      Managed Mo (parent != null and target != null)
     */
    def 'PUT - Execute create EUtranCellFDD return success'() {
        given: 'NbiCrudPutRequest'
        def xpath = 'MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20'
        def body ='{ "EUtranCellFDD": [ { "id": "20", "attributes": { "EUtranCellFDDId":"2022", "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test"' +
                ' }}] }'
        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        CmCreateResponse cmCreateResponse = objUnderTest.executor(nbiCrudPutRequest)
        then:
        with(cmCreateResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 201
            def resource = ((List)moObjects.moObjects.get("EUtranCellFDD")).iterator().next()
            with (resource) {
                id == '20'

                attributes.get("EUtranCellFDDId") == '20'
                attributes.get("userLabel") == '22'
                attributes.get("earfcndl") == 3
                attributes.get("earfcnul") == 18000
            }
        }
    }

    @Unroll
    def 'PUT - Execute create NetworkElement (with id null) return fail (checkConstraints fail on NetworkElementId)'() {
        given: 'NbiCrudPutRequest'
        def xpath = 'NetworkElement=ERBS001FORNBI'
        def body ='{ "NetworkElement": [ { "id":'+ identifier +', "attributes": { "NetworkElementId":"abc", "ossPREFIX":"22" , "NETYPE": "ERBS" }}] }'
        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        CmCreateResponse cmCreateResponse = objUnderTest.executor(nbiCrudPutRequest)
        then:
        with(cmCreateResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            moObjects == null
            //errorResponseType.error.errorInfo.contains("Constraint violation: value specified for attribute networkElementId is not valid")
            httpCode == 400
        }

        where:
        identifier      | _
        '"null"' |_
        null   |_
    }

    @Unroll
    def 'PUT - Execute create NetworkElement return fail when invalid attribute'() {
        given: 'NbiCrudPutRequest'
        def xpath = 'NetworkElement=LTETA0001FDD'
        def body ='{ "NetworkElement": [ { "id": "LTETA0001FDD", "attributes": { "NetworkElementId":"abc", "INVALID_ATTRIBUTE":"22" , "NETYPE": "ERBS" }}] }'
        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        CmCreateResponse cmCreateResponse = objUnderTest.executor(nbiCrudPutRequest)
        then:
        with(cmCreateResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            moObjects == null
            errorResponseType.error.errorInfo.contains("An unknown attribute has been encountered")
            httpCode == 417
        }
    }

    @Unroll
    def 'PUT - Execute create EUtranCellFDD with id null return fail cause id should be not null for POST request'() {
        given: 'NbiCrudPutRequest'
        def xpath = 'MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=1'
        def body ='{ "EUtranCellFDD": [ { "id": '+ identifier + ', "attributes": { "EUtranCellFDDId":"2022", "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test"' +
                ' }}] }'
        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        CmCreateResponse cmCreateResponse = objUnderTest.executor(nbiCrudPutRequest)
        then:
        with(cmCreateResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            moObjects == null
            errorResponseType.error.errorInfo.contains("The supplied parameters id=null is not allowed.")
            httpCode == 400
        }

        where:
        identifier      | _
        '"null"' |_
        null   |_

    }

    @Unroll
    def 'PUT - Execute create NetworkElement return fail when mismatch between xpath and resource'() {
        given: 'NbiCrudPutRequest'
        def body ='{ "NetworkElement": [ { "id": "LTETA0001FDD", "attributes": { "NetworkElementId":"abc", "INVALID_ATTRIBUTE":"22" , "NETYPE": "ERBS" }}] }'
        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        CmCreateResponse cmCreateResponse = objUnderTest.executor(nbiCrudPutRequest)
        then:
        with(cmCreateResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            moObjects == null
            errorResponseType.error.errorInfo.contains("Mismatch between URI Mo")
            httpCode == 400
        }
        where:
        xpath                     | _
        'NetworkElement=aa'     | _
        'Mo=bb'                   | _
        'null'                   | _
         null                   | _
    }


    /*
      TEST HTTP RETURN CODES
      We are using:
      201  OK
      400  for crud validation error
      401  for TBAC invalid fdn
      404  for invalid fdn
      404  for cm-reader/cm-writer response empty (NA in this scenario)
      417 for cm-reader/cm-writer error
      405  for unexpected exception
    */

    def 'POST - Execute nbiCrudPostRequest with objUnderTest.executeOperation return success'() {
        given: 'nbiCrudPostRequest'
        def xpath = 'MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1'
        def body ='{ "EUtranCellFDD": [ { "id": "1", "attributes": { "EUtranCellFDDId":"2022", "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test"' +
                ' }}] }'

        NbiCrudPostRequest nbiCrudPostRequest = new NbiCrudPostRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPostRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 201
            nbiResponse.jsonContent.contains('EUtranCellFDD')
        }
    }

    def 'POST - Execute nbiCrudPostRequest with objUnderTest.executeOperation with NetworkElement return success'() {
        given: 'nbiCrudPostRequest'
        def xpath = null
        def body = '{ "NetworkElement": [ { "id": "LTETA0001FDD", "attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS"}}] }'

        NbiCrudPostRequest nbiCrudPostRequest = new NbiCrudPostRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPostRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 201
            nbiResponse.jsonContent == '{"NetworkElement":{"id":"LTETA0001FDD","attributes":{"networkElementId":"LTETA0001FDD","ossPrefix":"22","neType":"ERBS"}}}'
        }
    }


    def 'POST - Execute nbiCrudPostRequest with objUnderTest.executeOperation with testDate (Date() field) return success'() {
        given: 'nbiCrudPostRequest'
        def xpath = null
        def body = '{ "NetworkElement": [ { "id": "LTETA0001FDD", "attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS", "testDate":"Sat Sep 18 11:00:00 CEST 2021"}}] }'

        NbiCrudPostRequest nbiCrudPostRequest = new NbiCrudPostRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPostRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 201
            nbiResponse.jsonContent.contains('Sep ') //minimal check cause on PCR env different timezone
        }
    }

    def 'POST - Execute nbiCrudPostRequest with objUnderTest.executeOperation return fail'() {
        given: 'nbiCrudPostRequest'
        def xpath = 'MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=xxx'
        def body ='{ "EUtranCellFDD": [ { "id": "1", "attributes": { "EUtranCellFDDId":"2022", "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test"' +
                ' }}] }'

        NbiCrudPostRequest nbiCrudPostRequest = new NbiCrudPostRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPostRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 404
            nbiResponse.jsonContent == '{"error":{"errorInfo":"The supplied FDN MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=xxx does not exist in the database"}}'
        }
    }

    def 'POST - Execute nbiCrudPostRequest with objUnderTest.executeOperation return fail when invalid Json'() {
        given: 'nbiCrudPostRequest'
        def xpath = 'MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1'
        def body ='{ invalidjson }'

        NbiCrudPostRequest nbiCrudPostRequest = new NbiCrudPostRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPostRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 400
            nbiResponse.jsonContent.contains('Invalid json in body:')
        }
    }

    def 'POST - Execute nbiCrudPostRequest with objUnderTest.executeOperation return fail when invalid TBAC node ERBS001'() {
        given: 'nbiCrudPostRequest'
        def xpath = 'MeContext=ERBS001/ManagedElement=1/ENodeBFunction=1'
        def body ='{ "EUtranCellFDD": [ { "id": "20", "attributes": { "EUtranCellFDDId":"2022", "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test"' +
                ' }}] }'

        NbiCrudPostRequest nbiCrudPostRequest = new NbiCrudPostRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPostRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 401
            nbiResponse.jsonContent.contains('Access Denied')
        }
    }

    def 'PUT - Execute nbiCrudPostRequest with objUnderTest.executeOperation return success'() {
        given: 'nbiCrudPostRequest'
        def xpath = 'MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=1'
        def body ='{ "EUtranCellFDD": [ { "id": "1", "attributes": { "EUtranCellFDDId":"2022", "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test"' +
                ' }}] }'

        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPutRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 201
            nbiResponse.jsonContent.contains('EUtranCellFDD')
        }
    }

    def 'PUT - Execute nbiCrudPostRequest with objUnderTest.executeOperation return fail'() {
        given: 'nbiCrudPostRequest'
        def xpath = 'MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=xxx/EUtranCellFDD=1'
        def body ='{ "EUtranCellFDD": [ { "id": "1", "attributes": { "EUtranCellFDDId":"2022", "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test"' +
                ' }}] }'

        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPutRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 404
            nbiResponse.jsonContent == '{"error":{"errorInfo":"The supplied FDN MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=xxx does not exist in the database"}}'
        }
    }

    def 'PUT - Execute nbiCrudPostRequest with objUnderTest.executeOperation return fail when invalid Json'() {
        given: 'nbiCrudPostRequest'
        def xpath = 'MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1'
        def body ='{ invalidjson }'

        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPutRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 400
            nbiResponse.jsonContent.contains('Invalid json in body:')
        }
    }

    def 'PUT - Execute nbiCrudPostRequest with objUnderTest.executeOperation return fail when id = null'() {
        given: 'nbiCrudPostRequest'
        def xpath = 'MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1'
        def body ='{ "EUtranCellFDD": [ { "id": '+ null +', "attributes": { "EUtranCellFDDId":"2022", "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test"' +
                ' }}] }'


        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPutRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 400
            nbiResponse.jsonContent.contains('The supplied parameters id=null is not allowed.')
        }
    }

    def 'PUT - Execute nbiCrudPostRequest with objUnderTest.executeOperation return fail when mismatch between xpath and resource'() {
        given: 'nbiCrudPostRequest'
        def xpath = 'MeContext=ERBS001FORNBI/ManagedElement=1'
        def body ='{ "EUtranCellFDD": [ { "id": "1", "attributes": { "EUtranCellFDDId":"2022", "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test"' +
                ' }}] }'

        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPutRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 400
            nbiResponse.jsonContent.contains('Mismatch between URI Mo')
        }
    }

    def 'PUT - Execute nbiCrudPostRequest with objUnderTest.executeOperation return fail when invalid TBAC node ERBS001'() {
        given: 'nbiCrudPostRequest'
        def xpath = 'MeContext=ERBS001/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20'
        def body ='{ "EUtranCellFDD": [ { "id": "20", "attributes": { "EUtranCellFDDId":"2022", "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test"' +
                ' }}] }'

        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPutRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 401
            nbiResponse.jsonContent.contains('Access Denied')
        }
    }

    /*
        TEST SINGLE METHODS
    */
    def 'CmCreateRequest and CmCreateResponse for_coverage_only'() {
        when:
        CmCreateRequest cmCreateRequest = new CmCreateRequest("fdn", '1' , 'childType', null)
        CmCreateResponse cmCreateSuccessResponse = new CmCreateResponse(ResponseType.SUCCESS, null , null, 0)
        CmCreateResponse cmCreateErroredResponse = new CmCreateResponse(ResponseType.FAIL, null , null, 0)

        then:
        cmCreateRequest.toString()
        cmCreateSuccessResponse.toString()
        cmCreateSuccessResponse.isErrored() == false
        cmCreateErroredResponse.toString()
        cmCreateErroredResponse.isErrored() == true
    }


    def 'formatCmObject when empty response fail (for_coverage_only)'() {
        when:
        CmCreateResponse cmCreateResponse = objUnderTest.formatCmObject(new CmResponse())

        then:
        with(cmCreateResponse) {
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
        CmCreateResponse cmCreateResponse = objUnderTest.formatCmObject(erroredCmResponse)

        then:
        with(cmCreateResponse) {
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