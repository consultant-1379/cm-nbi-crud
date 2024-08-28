package com.ericsson.oss.services.cmnbicrud.ejb.impl.executor

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.rule.custom.node.NodeProperty
import com.ericsson.cds.cdi.support.rule.custom.node.RootNode
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.sdk.security.accesscontrol.EAccessControl
import com.ericsson.oss.itpf.sdk.security.accesscontrol.ESecurityTarget
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudPatchRequest
import com.ericsson.oss.presentation.cmnbirest.api.NbiResponse
import com.ericsson.oss.presentation.cmnbirest.api.PatchContentType
import com.ericsson.oss.services.cm.cmsearch.DynamicBlackListInfoIf
import com.ericsson.oss.services.cm.cmsearch.excessivedata.DynamicBlackListEntry
import com.ericsson.oss.services.cm.cmshared.dto.search.CmSearchCriteria
import com.ericsson.oss.services.cmnbicrud.ejb.common.WriteResponse
import com.ericsson.oss.services.cmnbicrud.ejb.patch.CmPatchRequest
import com.ericsson.oss.services.cmnbicrud.ejb.patch.ErroredWriteResponse
import com.ericsson.oss.services.cmnbicrud.ejb.testutil.BaseForCommandReceiverSpecs
import com.ericsson.oss.services.cmnbicrud.spi.output.ErrorResponseType
import spock.lang.Unroll

import javax.inject.Inject

class CmNbiCrudPatchExecutorSpec extends BaseForCommandReceiverSpecs {
    @ObjectUnderTest
    private CmNbiCrudPatchExecutor objUnderTest

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

        securityTarget = ESecurityTarget.map('ERBS002');
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

    @Unroll
    def 'Check ignore unknown fields'() {
        given: 'NbiCrudPatchRequest'
        def xpath = 'NetworkElement=LTETA0001FDD'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath , body, PatchContentType.THREE_GPP_JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        notThrown(Exception)

        where:
        body | _
        '[ { "op": "add", "path": "", "value": { "id": "LTETA0001FDD", "class": "NetworkElement", "otherField":"aa" ,"attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" } } } ]' | _
    }

    def 'Execute NbiCrudPatchRequest with patchContentType=null return fail'() {
        given: 'NbiCrudPatchRequest'
        def xpath = 'NetworkElement=LTETA0001FDD'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath , '[]', null)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        writeResponse.isErrored()
        writeResponse.moObjects == null
        writeResponse.cmObjects == null
        writeResponse.errorResponseType != null
        writeResponse.errorResponseType.error.errorInfo.contains("Unimplemented Use Case for ContentType=null")
        writeResponse.httpCode == 422
    }

    /*
    *   3gpp Json Patch
    *
    * */

    /*
    Sample of 3gpp Json Patch.

    Note "op": "replace"
    From recommendation, semms possible to specify attribute in 2 formats (with or without / after #)
        "path": "/ENodeBFunction=1#/attributes/userlabel"
        "path": "/ENodeBFunction=1#attributes/userlabel"

    General Note:
    For our own choice we can manage / missing as first char of path
        "path": "/ENodeBFunction=1
        "path": "ENodeBFunction=1

   General Note: for "op":"add" and "op":"remove"  path could be "" (so for our choice we survive to missing or null path)

    https://enmapache.athtem.eei.ericsson.se/enm-nbi/cm/v1/data/SubNetwork=Europe/SubNetwork=Ireland/SubNetwork=NETSimW/ManagedElement=LTE26dg2ERBS00001
    Content-Type: application/3gpp-json-patch+json
    [
    {
    "op": "add",
    "path": "/ENodeBFunction=1,EUtranCellFDD=testmisto1" ,
    "value": {
      "id": "testmisto1",
      "attributes": {
        "cellId":4,
        "earfcndl":1,
        "earfcnul":18001,
        "physicalLayerCellIdGroup":19,
	    "physicalLayerSubCellId":1,
	    "tac":1
      }
    }
   },
  {
    "op": "replace",
    "path": "/ENodeBFunction=1#/attributes/userlabel" ,
    "value": "newUserLabel"
  },
  {
    "op": "remove",
    "path": "/ENodeBFunction=1,EUtranCellFDD=testmisto2"
  }
  ]
     */


    /*
         CHECK INPUT PARAMETERS
     */

    @Unroll
    def '3GPP_JSON_PATCH - Execute NbiCrudPatchRequest with op=null or invalid return fail'() {
        given: 'NbiCrudPatchRequest'
        def xpath = 'NetworkElement=LTETA0001FDD'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath , body, PatchContentType.THREE_GPP_JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        writeResponse.isErrored()
        writeResponse.moObjects == null
        writeResponse.cmObjects == null
        writeResponse.errorResponseType != null
        writeResponse.errorResponseType.error.errorInfo.contains("Unimplemented Use Case")
        writeResponse.httpCode == 422

        where:
        body | _
        '[ { "op": null  , "path": "", "value": { "id": "LTETA0001FDD", "attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" } } } ]' | _
        '[ { "op": ""    , "path": "", "value": { "id": "LTETA0001FDD", "attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" } } } ]' | _
        '[ { "op": "aa"  , "path": "", "value": { "id": "LTETA0001FDD", "attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" } } } ]' | _
    }

    /*
    *                 op=add requests
    *
    * */

    @Unroll
    def '3GPP_JSON_PATCH - Execute add NetworkElement with xpath=null and missing or null path return fail'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath , body, PatchContentType.THREE_GPP_JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        writeResponse.isErrored()
        writeResponse.moObjects == null
        writeResponse.cmObjects == null
        writeResponse.errorResponseType != null
        writeResponse.errorResponseType.error.errorInfo.contains("The supplied parameters")
        writeResponse.httpCode == 400

        where:
        xpath                                  | body | _
        //xpath = null and path missing, null or ""
        null   | '[ { "op": "add", "value": { "id": "LTETA0001FDD", "attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" } } } ]' | _
        null   | '[ { "op": "add", "path": null , "value": { "id": "LTETA0001FDD", "attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" } } } ]' | _
        null   | '[ { "op": "add", "path": "", "value": { "id": "LTETA0001FDD", "attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" } } } ]' | _
    }

    @Unroll
    def '3GPP_JSON_PATCH - Execute add NetworkElement with missing or invalid value return fail'() {
        given: 'NbiCrudPatchRequest'
        def xpath = 'NetworkElement=LTETA0001FDD'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath , body, PatchContentType.THREE_GPP_JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        writeResponse.isErrored()
        writeResponse.moObjects == null
        writeResponse.cmObjects == null
        writeResponse.errorResponseType != null
        writeResponse.errorResponseType.error.errorInfo.contains("Not allowed json value: an invalid value=")
        writeResponse.httpCode == 400

        where:
        body | _
        '[ { "op": "add" , "path": ""} ]' | _
        '[ { "op": "add" , "path": "", "value": null } ]' | _
        '[ { "op": "add" , "path": "", "value": "" } ]' | _
        '[ { "op": "add" , "path": "", "value": 3 } ]' | _
    }

    @Unroll
    def '3GPP_JSON_PATCH - Execute add NetworkElement with missing or invalid attributes return fail'() {
        given: 'NbiCrudPatchRequest'
        def xpath = 'NetworkElement=LTETA0001FDD'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        writeResponse.isErrored()
        writeResponse.moObjects == null
        writeResponse.cmObjects == null
        writeResponse.errorResponseType != null
        writeResponse.errorResponseType.error.errorInfo.contains("Not allowed json value: an invalid attributes")
        writeResponse.httpCode == 400

        where:
        body                                                                                                                         | _
        '[ { "op": "add"  ,"path": "", "value": { "id": "LTETA0001FDD"} } ]' | _
        '[ { "op": "add"  ,"path": "", "value": { "id": "LTETA0001FDD", "attributes": null } } ]' | _
    }

    @Unroll
    def '3GPP_JSON_PATCH - Execute add NetworkElement with missing id return fail'() {
        given: 'NbiCrudPatchRequest'
        def xpath = 'NetworkElement=LTETA0001FDD'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        writeResponse.isErrored()
        writeResponse.moObjects == null
        writeResponse.cmObjects == null
        writeResponse.errorResponseType != null
        writeResponse.errorResponseType.error.errorInfo.contains("Missing json field: id")
        writeResponse.httpCode == 400

        where:
        body                                                                                                                         | _
       '[ { "op": "add", "value": {"attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" } } } ]' | _
    }


    @Unroll
    def '3GPP_JSON_PATCH - Execute add with invalid fdn return fail'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        writeResponse.isErrored()
        writeResponse.moObjects == null
        writeResponse.cmObjects == null
        writeResponse.errorResponseType != null
        writeResponse.errorResponseType.error.errorInfo.contains("The supplied FDN MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=xxx does not exist")
        writeResponse.httpCode == 404

        where:
        xpath | body                                                                                                                         | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=xxx/EUtranCellFDD=20"  | '[ { "op": "add", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } } ]' | _
    }


    @Unroll
    def '3GPP_JSON_PATCH - Execute add NetworkElement return success'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath , body, PatchContentType.THREE_GPP_JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        !writeResponse.isErrored()
        def resource = ((List)writeResponse.cmObjects).get(0)
        with (resource) {
                name == 'LTETA0001FDD'
                type == "NetworkElement"

                attributes.get("networkElementId") == name
                attributes.get("neType") == 'ERBS'
                attributes.get("ossPrefix") == '22'
                !attributes.containsKey("technologyDomain")
        }

        where:
        xpath                                  | body | _
        //xpath null is managed normally
        null                                  | '[ { "op": "add", "path": "/NetworkElement=LTETA0001FDD", "value": { "id": "LTETA0001FDD", "attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" } } } ]' | _
        null                                  | '[ { "op": "add", "path": "NetworkElement=LTETA0001FDD", "value": { "id": "LTETA0001FDD", "attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" } } } ]' | _

        //path missing, null or "" is managed normally
        'NetworkElement=LTETA0001FDD'      | '[ { "op": "add", "value": { "id": "LTETA0001FDD", "attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" } } } ]' | _
        'NetworkElement=LTETA0001FDD'      | '[ { "op": "add", "path": null , "value": { "id": "LTETA0001FDD", "attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" } } } ]' | _
        'NetworkElement=LTETA0001FDD'      | '[ { "op": "add", "path": "", "value": { "id": "LTETA0001FDD", "attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" } } } ]' | _

        //normal behaviour (where id wins)
        'NetworkElement=LTETA0001FDD'      | '[ { "op": "add", "path": "NetworkElement=LTETA0001FDD", "value": { "id": "LTETA0001FDD", "attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" } } } ]' | _
        'NetworkElement=LTETA0001FDD'      | '[ { "op": "add", "path": "NetworkElement=LTETA0001FDDxx", "value": { "id": "LTETA0001FDD", "attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" } } } ]' | _

        //id null is managed normally but we need to test creating EutranCellFDD or similar
        }


    @Unroll
    def '3GPP_JSON_PATCH - Execute add EUtranCellFDD return success'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        !writeResponse.isErrored()
        def resource = ((List)writeResponse.cmObjects).get(0)
        with(resource) {
            name == '20' || name == '1'
            type == "EUtranCellFDD"

            if (name == '1') {
                attributes.get("EUtranCellFDDId") == '2022'
            } else {
                attributes.get("EUtranCellFDDId") == '20'
            }
            attributes.get("userLabel") == '22'
            attributes.get("earfcndl") == 3
            attributes.get("earfcnul") == 18000
        }

        where:
        xpath                                                                        | body                                                                                                                                                                                                                                                                                                                                                         | _
        //xpath null is managed normally
        null      | '[ { "op": "add", "path": "/MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } } ]' | _
        null      | '[ { "op": "add", "path": "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } } ]' | _


        //path missing, null or "" is managed normally
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20"  | '[ { "op": "add", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } } ]' | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20"  | '[ { "op": "add", "path": null, "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } } ]' | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20"  | '[ { "op": "add", "path": "", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } } ]' | _

        //normal behaviour (where id wins)
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20" | '[ { "op": "add", "path": "/MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } } ]' | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1"                  | '[ { "op": "add", "path": "/MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } } ]' | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1"                  | '[ { "op": "add", "path": "/EUtranCellFDD=20", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } } ]'                                                           | _

        //id null is managed normally when EUtranCellFDDId is specified
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1"                  | '[ { "op": "add", "path": "/MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20", "value": { "id": null, "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } } ]' | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1"                  | '[ { "op": "add", "path": "/MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20", "value": { "id": "null", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } } ]' | _
    }


    /*
    *                 op=replace requests
    *
    * */

    @Unroll
    def '3GPP_JSON_PATCH - Execute modify ENodeBFunction with missing or invalid path (null or no attribute) return fail'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        writeResponse.isErrored()
        writeResponse.moObjects == null
        writeResponse.cmObjects == null
        writeResponse.errorResponseType != null
        writeResponse.errorResponseType.error.errorInfo.contains("The supplied parameters")
        writeResponse.httpCode == 400

        where:
        xpath | body                                                                                                                         | _
        'MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1' | '[ { "op": "replace", "value": "newUserLabel" } ] ' | _
        'MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1' | '[ { "op": "replace", "path":null, "value": "newUserLabel" } ] ' | _
        'MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1' | '[ { "op": "replace", "path":"", "value": "newUserLabel" } ] ' | _
        // no attribues (missing #/attributes/attr or #attributes/attr)
        'MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1' | '[ { "op": "replace", "path":"/MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1", "value": "newUserLabel" } ] ' | _

        //both xpath and path invalid
        null | '[ { "op": "replace", "value": "newUserLabel" } ] ' | _
        null | '[ { "op": "replace", "path":null, "value": "newUserLabel" } ] ' | _
        null | '[ { "op": "replace", "path":"", "value": "newUserLabel" } ] ' | _
    }

    @Unroll
    def '3GPP_JSON_PATCH - Execute modify ENodeBFunction with wrong attribute name return fail'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        writeResponse.isErrored()
        writeResponse.moObjects == null
        writeResponse.cmObjects == null
        writeResponse.errorResponseType != null
        writeResponse.errorResponseType.error.errorInfo.contains("An unknown attribute has been encountered")
        writeResponse.httpCode == 417

        where:
        xpath | body                                                                                                                         | _
        "MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1"      | '[ { "op": "replace", "path":"/MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1#attributes/invalidAttr", "value": "newUserLabel" } ] ' | _
    }

    @Unroll
    def '3GPP_JSON_PATCH - Execute modify with invalid fdn return fail'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        writeResponse.isErrored()
        writeResponse.moObjects == null
        writeResponse.cmObjects == null
        writeResponse.errorResponseType != null
        writeResponse.errorResponseType.error.errorInfo.contains("The supplied FDN MeContext=ERBS002,ManagedElement=1,ENodeBFunction=xxx does not exist")
        writeResponse.httpCode == 404

        where:
        xpath | body                                                                                                                         | _
        "MeContext=ERBS002/ManagedElement=1/ENodeBFunction=xxx"      | '[ { "op": "replace", "path":"/MeContext=ERBS002/ManagedElement=1/ENodeBFunction=xxx#attributes/userlabel", "value": "newUserLabel" } ] ' | _
    }

    @Unroll
    def '3GPP_JSON_PATCH - Execute modify ENodeBFunction attribute with success'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        System.out.println("responses=" + responses)
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        !writeResponse.isErrored()
        def resource = ((List)writeResponse.cmObjects).get(0)
        with (resource) {
            type == "ENodeBFunction"
            name == '1'
            attributes.get("userLabel") == 'newUserLabel'
            !attributes.containsKey('eNodeBPlmnId')
        }

        where:
        xpath      |  body                                                                                                                                                                       | _
        //xpath null is managed normally
        null      | '[ { "op": "replace", "path":"/MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1#attributes/userlabel", "value": "newUserLabel" } ] ' | _
        null      | '[ { "op": "replace", "path":"MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1#attributes/userlabel", "value": "newUserLabel" } ] ' | _
        null      | '[ { "op": "replace", "path":"MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1#/attributes/userlabel", "value": "newUserLabel" } ] ' | _

        //path missing, null or "" is errored (see previous test)
        //no tests here

        //normal behaviour
        "MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1"      | '[ { "op": "replace", "path":"/MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1#attributes/userlabel", "value": "newUserLabel" } ] ' | _
        "MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1"      | '[ { "op": "replace", "path":"/MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1#/attributes/userlabel", "value": "newUserLabel" } ] ' | _

        "MeContext=ERBS002/ManagedElement=1"                         | '[ { "op": "replace", "path":"/MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1#attributes/userlabel", "value": "newUserLabel" } ] ' | _
        "MeContext=ERBS002/ManagedElement=1"                         | '[ { "op": "replace", "path":"/MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1#/attributes/userlabel", "value": "newUserLabel" } ] ' | _

        "MeContext=ERBS002/ManagedElement=1"                         | '[ { "op": "replace", "path":"/ENodeBFunction=1#attributes/userlabel", "value": "newUserLabel" } ] ' | _
        "MeContext=ERBS002/ManagedElement=1"                         | '[ { "op": "replace", "path":"/ENodeBFunction=1#/attributes/userlabel", "value": "newUserLabel" } ] ' | _

        "MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1"      | '[ { "op": "replace", "path":"#attributes/userlabel", "value": "newUserLabel" } ] ' | _
        "MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1"      | '[ { "op": "replace", "path":"#/attributes/userlabel", "value": "newUserLabel" } ] ' | _
    }

    @Unroll
    def '3GPP_JSON_PATCH - Execute modify ENodeBFunction complex attribute with success'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        System.out.println("responses=" + responses)
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        !writeResponse.isErrored()
        def resource = ((List)writeResponse.cmObjects).get(0)
        with (resource) {
            name == '1'
            type == "ENodeBFunction"
            attributes.get("eNodeBPlmnId") == [mcc:352, mnc:55, mncLength:2]
            !attributes.containsKey('userLabel')
        }

        where:
        xpath                                                             | body                                                                                                                                                                      | _
        "MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1"      | '[ { "op": "replace", "path":"/MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1#attributes/eNodeBPlmnId", "value": { "mncLength" : 2, "mcc" : 352, "mnc" : 55 } } ] ' | _
    }

    @Unroll
    def '3GPP_JSON_PATCH - Execute modify ENodeBFunction complex attribute (partial) with success'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        System.out.println("responses=" + responses)
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        !writeResponse.isErrored()
        def resource = ((List)writeResponse.cmObjects).get(0)
        with (resource) {
            name == '1'
            type == "ENodeBFunction"
            attributes.get("eNodeBPlmnId") == [mcc: 533, mnc: 77, mncLength:2]
            !attributes.containsKey('userLabel')
        }

        where:
        xpath                                                      | body                                                                                                                                                                      | _
        "MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1"      | '[ { "op": "replace", "path":"/MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1#attributes/eNodeBPlmnId/mnc", "value":77 } ] ' | _
    }

    /*
    *                 op=remove requests
    *
    * */

    @Unroll
    def '3GPP_JSON_PATCH - Execute remove with invalid fdn return fail'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        writeResponse.isErrored()
        writeResponse.moObjects == null
        writeResponse.cmObjects == null
        writeResponse.errorResponseType != null
        writeResponse.errorResponseType.error.errorInfo.contains("The supplied FDN MeContext=ERBS002,ManagedElement=1,ENodeBFunction=xxx does not exist")
        writeResponse.httpCode == 404 || writeResponse.httpCode == 417

        where:
        xpath | body                                                                                                                         | _
        //here check is in crud (httpCode 404)
        "MeContext=ERBS002/ManagedElement=1/ENodeBFunction=xxx"      | '[ { "op": "remove", "path":"/MeContext=ERBS002/ManagedElement=1/ENodeBFunction=xxx" } ] ' | _
        //here check is in writer method deleteManagedObjectForNbi (httpCode 405)
        "MeContext=ERBS002/ManagedElement=1"                         | '[ { "op": "remove", "path": "/MeContext=ERBS002/ManagedElement=1/ENodeBFunction=xxx" } ]' | _
    }

    @Unroll
    def '3GPP_JSON_PATCH - Execute remove EUtranCellFDD return success'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        !writeResponse.isErrored()
        def resource = ((List)writeResponse.cmObjects).get(0)
        with(resource)  {
            name == 'toBeRemoved'
            type == "EUtranCellFDD"
        }

        where:
        xpath                                                                        | body                                                                                                                                                                                                                                                                                                                                                         | _
        //xpath null is managed normally
        null      | '[ { "op": "remove", "path": "/MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=toBeRemoved" } ]' | _
        null      | '[ { "op": "remove", "path": "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=toBeRemoved" } ]' | _

       //path missing, null or "" is managed normally
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=toBeRemoved"    | '[ { "op": "remove"} ]' | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=toBeRemoved"    | '[ { "op": "remove", "path": null } ]' | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=toBeRemoved"    | '[ { "op": "remove", "path": "" } ]' | _

        //normal behaviour
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=toBeRemoved"      | '[ { "op": "remove", "path": "/MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=toBeRemoved" } ]' | _
        "MeContext=ERBS001FORNBI/ManagedElement=1"                         | '[ { "op": "remove", "path": "/MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=toBeRemoved" } ]' | _
    }

    @Unroll
    def '3GPP_JSON_PATCH - Execute remove ENodeBFunction (with child) and returns success'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        !writeResponse.isErrored()
        writeResponse.getHttpCode() == 200
        writeResponse.cmObjects.size() == 2

        where:
        xpath     | body                                                                                                                                                                                                                                                                                                                                                         | _
        //xpath null is managed normally
        null      | '[ { "op": "remove", "path": "/MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1" } ]' | _
        null      | '[ { "op": "remove", "path": "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1" } ]' | _

        //path missing, null or "" is managed normally
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1"    | '[ { "op": "remove"} ]' | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1"    | '[ { "op": "remove", "path": null } ]' | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1"    | '[ { "op": "remove", "path": "" } ]' | _

        //normal behaviour
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1"      | '[ { "op": "remove", "path": "/MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1" } ]' | _
        "MeContext=ERBS001FORNBI/ManagedElement=1"                         | '[ { "op": "remove", "path": "/MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1" } ]' | _
    }

    /*
    *                 mixed requests
    *
    * */
    @Unroll
    def '3GPP_JSON_PATCH - Execute add/replace/remove EUtranCellFDD return success'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 3

        //response 1
        WriteResponse writeResponse = responses.get(0)
        !writeResponse.isErrored()
        def resource = ((List)writeResponse.cmObjects).get(0)
        with(resource) {
            type == "EUtranCellFDD"
            name == '20' || name == '1'
            attributes.get("EUtranCellFDDId") == '20'
            attributes.get("userLabel") == '22'
            attributes.get("earfcndl") == 3
            attributes.get("earfcnul") == 18000
        }

        //response 2
        WriteResponse writeResponse2 = responses.get(1)
        !writeResponse2.isErrored()
        def resource2 = ((List)writeResponse2.cmObjects).get(0)
        with(resource2) {
            type == "ENodeBFunction"
            name == '1'
            attributes.get("userLabel") == 'newUserLabel'
            !attributes.containsKey('eNodeBPlmnId')
        }

        //response 3
        WriteResponse writeResponse3 = responses.get(2)
        !writeResponse3.isErrored()
        def resource3 = ((List)writeResponse3.cmObjects).get(0)
        with(resource3) {
            type == "EUtranCellFDD"
            name == 'toBeRemoved'
        }

        where:
        xpath                                                                        | body                                                                                                                                                                                                                                                                                                                                                         | _
        //xpath null is managed normally
        null | '[ { "op": "add", "path": "/MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } }, { "op": "replace", "path":"/MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1#attributes/userlabel", "value": "newUserLabel" }, { "op": "remove", "path": "/MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=toBeRemoved" } ]' | _
        null | '[ { "op": "add", "path": "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } }, { "op": "replace", "path":"MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1#attributes/userlabel", "value": "newUserLabel" }, { "op": "remove", "path": "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=toBeRemoved" } ]' | _

        //normal behaviour
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1" | '[ { "op": "add", "path": "/MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } }, { "op": "replace", "path":"/MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1#attributes/userlabel", "value": "newUserLabel" }, { "op": "remove", "path": "/MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=toBeRemoved" } ]' | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1" | '[ { "op": "add", "path": "/EUtranCellFDD=20", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } }, { "op": "replace", "path":"#attributes/userlabel", "value": "newUserLabel" }, { "op": "remove", "path": "/EUtranCellFDD=toBeRemoved" } ]' | _
    }

    @Unroll
    def '3GPP_JSON_PATCH - Execute add/replace/remove EUtranCellFDD return fail if error in last remove request'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 3

        //response 1
        WriteResponse writeResponse = responses.get(0)
        !writeResponse.isErrored()
        def resource = ((List)writeResponse.cmObjects).get(0)
        with(resource) {
            type == "EUtranCellFDD"
            name == '20' || name == '1'
            attributes.get("EUtranCellFDDId") == '20'
            attributes.get("userLabel") == '22'
            attributes.get("earfcndl") == 3
            attributes.get("earfcnul") == 18000
        }

        //response 2
        WriteResponse writeResponse2 = responses.get(1)
        !writeResponse2.isErrored()
        def resource2 = ((List)writeResponse2.cmObjects).get(0)
        with(resource2) {
            name == '1'
            attributes.get("userLabel") == 'newUserLabel'
            !attributes.containsKey('eNodeBPlmnId')
        }

        //response 3
        WriteResponse writeResponse3 = responses.get(2)
        writeResponse3.isErrored()
        writeResponse3.moObjects == null
        writeResponse3.errorResponseType != null
        writeResponse3.errorResponseType.error.errorInfo.contains("The supplied FDN MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=xxx does not exist")
        writeResponse3.httpCode == 417

        where:
        xpath                                                                        | body                                                                                                                                                                                                                                                                                                                                                         | _
        //xpath null is managed normally
        null | '[ { "op": "add", "path": "/MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } }, { "op": "replace", "path":"/MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1#attributes/userlabel", "value": "newUserLabel" }, { "op": "remove", "path": "/MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=xxx" } ]' | _
        null | '[ { "op": "add", "path": "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } }, { "op": "replace", "path":"MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1#attributes/userlabel", "value": "newUserLabel" }, { "op": "remove", "path": "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=xxx" } ]' | _

        //normal behaviour
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1" | '[ { "op": "add", "path": "/MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } }, { "op": "replace", "path":"/MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1#attributes/userlabel", "value": "newUserLabel" }, { "op": "remove", "path": "/MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=xxx" } ]' | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1" | '[ { "op": "add", "path": "/EUtranCellFDD=20", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } }, { "op": "replace", "path":"#attributes/userlabel", "value": "newUserLabel" }, { "op": "remove", "path": "/EUtranCellFDD=xxx" } ]' | _
    }


    /*
    *   Json Patch
    *
    * */

    /*
    Sample of Json Patch.

    This patch could only have xpath!=null so we can't specify xpath=null via REST (see cm-nbi-rest).
    In any case we give error in case of xpath=null.

    Note "op": "replace"
    From recommendation, seems possible to specify attributes in 1 format
        "path": "/attributes/userlabel"

    For our own choice we manage also
        "path": "attributes/userlabel"

    General Note: for "op":"add" and "op":"remove"  path should be always "" (so for our choice we survive to missing or null path)

    https://enmapache.athtem.eei.ericsson.se/enm-nbi/cm/v1/data/SubNetwork=Europe/SubNetwork=Ireland/SubNetwork=NETSimW/ManagedElement=LTE26dg2ERBS00001/ENodeBFunction=1,EUtranCellFDD=testmisto1
    Content-Type: application/3gpp-json-patch+json
    [
    {
    "op": "add",
    "path": "" ,
    "value": {
    "id": "testmisto1",
    "attributes": {
        "cellId":4,
        "earfcndl":1,
        "earfcnul":18001,
        "physicalLayerCellIdGroup":19,
        "physicalLayerSubCellId":1,
        "tac":1
      }
    }
    },
    {
    "op": "replace",
    "path": "/attributes/userlabel" ,
    "value": "newUserLabel"
    },
    {
    "op": "remove",
    "path": ""
    }
   ]
  */

    @Unroll
    def 'JSON_PATCH - Execute NbiCrudPatchRequest with op=null or invalid return fail'() {
        given: 'NbiCrudPatchRequest'
        def xpath = 'NetworkElement=LTETA0001FDD'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath , body, PatchContentType.JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        writeResponse.isErrored()
        writeResponse.moObjects == null
        writeResponse.cmObjects == null
        writeResponse.errorResponseType != null
        writeResponse.errorResponseType.error.errorInfo.contains("Unimplemented Use Case")
        writeResponse.httpCode == 422

        where:
        body | _
        '[ { "op": null  , "path": "", "value": { "id": "LTETA0001FDD", "attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" } } } ]' | _
        '[ { "op": ""    , "path": "", "value": { "id": "LTETA0001FDD", "attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" } } } ]' | _
        '[ { "op": "aa"  , "path": "", "value": { "id": "LTETA0001FDD", "attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" } } } ]' | _
    }

    @Unroll
    def 'JSON_PATCH - Execute NbiCrudPatchRequest with xpath=null return fail'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath , body, PatchContentType.JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        writeResponse.isErrored()
        writeResponse.moObjects == null
        writeResponse.cmObjects == null
        writeResponse.errorResponseType != null
        writeResponse.errorResponseType.error.errorInfo.contains("The supplied parameters xpath=null is not allowed.")
        writeResponse.httpCode == 400

        where:
        xpath                                  | body | _
        null   | '[ { "op": "add", "path": "" , "value": { "id": "LTETA0001FDD", "attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" } } } ]' | _
        null   | '[ { "op": "replace", "path":"/MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1#attributes/eNodeBPlmnId", "value": { "mncLength" : 2, "mcc" : 352, "mnc" : 55 } } ] ' | _
        null   | '[ { "op": "remove", "path": "MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1" } ]' | _
    }

    /*
    *           op=add requests
    *
    * */

    @Unroll
    def 'JSON_PATCH - Execute add NetworkElement with missing or invalid value return fail'() {
        given: 'NbiCrudPatchRequest'
        def xpath = 'NetworkElement=LTETA0001FDD'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath , body, PatchContentType.JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        writeResponse.isErrored()
        writeResponse.moObjects == null
        writeResponse.cmObjects == null
        writeResponse.errorResponseType != null
        writeResponse.errorResponseType.error.errorInfo.contains("Not allowed json value: an invalid value=")
        writeResponse.httpCode == 400

        where:
        body | _
        '[ { "op": "add" , "path": ""} ]' | _
        '[ { "op": "add" , "path": "", "value": null } ]' | _
        '[ { "op": "add" , "path": "", "value": "" } ]' | _
        '[ { "op": "add" , "path": "", "value": 3 } ]' | _
    }

    @Unroll
    def 'JSON_PATCH - Execute add NetworkElement with missing or invalid attributes return fail'() {
        given: 'NbiCrudPatchRequest'
        def xpath = 'NetworkElement=LTETA0001FDD'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        writeResponse.isErrored()
        writeResponse.moObjects == null
        writeResponse.cmObjects == null
        writeResponse.errorResponseType != null
        writeResponse.errorResponseType.error.errorInfo.contains("Not allowed json value: an invalid attributes")
        writeResponse.httpCode == 400

        where:
        body                                                                                                                         | _
        '[ { "op": "add"  ,"path": "", "value": { "id": "LTETA0001FDD"} } ]' | _
        '[ { "op": "add"  ,"path": "", "value": { "id": "LTETA0001FDD", "attributes": null } } ]' | _
    }

    @Unroll
    def 'JSON_PATCH - Execute add NetworkElement with missing id return fail'() {
        given: 'NbiCrudPatchRequest'
        def xpath = 'NetworkElement=LTETA0001FDD'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        writeResponse.isErrored()
        writeResponse.moObjects == null
        writeResponse.cmObjects == null
        writeResponse.errorResponseType != null
        writeResponse.errorResponseType.error.errorInfo.contains("Missing json field: id")
        writeResponse.httpCode == 400

        where:
        body                                                                                                                         | _
        '[ { "op": "add", "value": {"attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" } } } ]' | _
    }

    @Unroll
    def 'JSON_PATCH - Execute add NetworkElement with path not null return fail'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath , body, PatchContentType.JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        writeResponse.isErrored()
        writeResponse.moObjects == null
        writeResponse.cmObjects == null
        writeResponse.errorResponseType != null
        writeResponse.errorResponseType.error.errorInfo.contains("The supplied parameters")
        writeResponse.httpCode == 400

        where:
        xpath                                  | body | _
        'NetworkElement=LTETA0001FDD'      | '[ { "op": "add", "path": "NetworkElement=LTETA0001FDD", "value": { "id": "LTETA0001FDD", "attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" } } } ]' | _
        'NetworkElement=LTETA0001FDD'      | '[ { "op": "add", "path": "NetworkElement=LTETA0001FDDxx", "value": { "id": "LTETA0001FDD", "attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" } } } ]' | _
    }

    @Unroll
    def 'JSON_PATCH - Execute add with invalid fdn return fail'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        writeResponse.isErrored()
        writeResponse.moObjects == null
        writeResponse.cmObjects == null
        writeResponse.errorResponseType != null
        writeResponse.errorResponseType.error.errorInfo.contains("The supplied FDN MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=xxx does not exist")
        writeResponse.httpCode == 404

        where:
        xpath | body                                                                                                                         | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=xxx/EUtranCellFDD=20"  | '[ { "op": "add", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } } ]' | _
    }

    @Unroll
    def 'JSON_PATCH - Execute add NetworkElement return success'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath , body, PatchContentType.JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        !writeResponse.isErrored()
        def resource = ((List)writeResponse.cmObjects).get(0)
        with (resource) {
            name == 'LTETA0001FDD'
            type == "NetworkElement"

            attributes.get("networkElementId") == name
            attributes.get("neType") == 'ERBS'
            attributes.get("ossPrefix") == '22'
            !attributes.containsKey("technologyDomain")
        }

        where:
        xpath                                  | body | _
        //path should be ""  (but we managed also if null or missing)
        'NetworkElement=LTETA0001FDD'      | '[ { "op": "add", "value": { "id": "LTETA0001FDD", "attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" } } } ]' | _
        'NetworkElement=LTETA0001FDD'      | '[ { "op": "add", "path": null , "value": { "id": "LTETA0001FDD", "attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" } } } ]' | _
        'NetworkElement=LTETA0001FDD'      | '[ { "op": "add", "path": "", "value": { "id": "LTETA0001FDD", "attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" } } } ]' | _

        //id null is managed normally but we need to test creating EutranCellFDD or similar
    }

    @Unroll
    def 'JSON_PATCH - Execute add EUtranCellFDD return success'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        !writeResponse.isErrored()
        def resource = ((List)writeResponse.cmObjects).get(0)
        with(resource) {
            name == '20' || name == '1'
            type == "EUtranCellFDD"

            if (name == '1') {
                attributes.get("EUtranCellFDDId") == '2022'

            } else {
                attributes.get("EUtranCellFDDId") == '20'
            }
            attributes.get("userLabel") == '22'
            attributes.get("earfcndl") == 3
            attributes.get("earfcnul") == 18000
        }

        where:
        xpath                                                                        | body                                                                                                                                                                                                                                                                                                                                                         | _
        //path should be ""  (but we managed also if null or missing)
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20"  | '[ { "op": "add", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } } ]' | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20"  | '[ { "op": "add", "path": null, "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } } ]' | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20"  | '[ { "op": "add", "path": "", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } } ]' | _

        //id null is managed normally when EUtranCellFDDId is specified
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20"                  | '[ { "op": "add", "path": "", "value": { "id": null, "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } } ]' | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20"                  | '[ { "op": "add", "path": "", "value": { "id": "null", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } } ]' | _
    }

    /*
    *                 op=replace requests
    *
    * */

    @Unroll
    def 'JSON_PATCH - Execute modify ENodeBFunction with missing or invalid path (null or no attribute) return fail'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        writeResponse.isErrored()
        writeResponse.moObjects == null
        writeResponse.cmObjects == null
        writeResponse.errorResponseType != null
        writeResponse.errorResponseType.error.errorInfo.contains("The supplied parameters")
        writeResponse.httpCode == 400

        where:
        xpath | body                                                                                                                         | _
        'MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1' | '[ { "op": "replace", "value": "newUserLabel" } ] ' | _
        'MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1' | '[ { "op": "replace", "path":null, "value": "newUserLabel" } ] ' | _
        'MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1' | '[ { "op": "replace", "path":"", "value": "newUserLabel" } ] ' | _
        // no attribues (missing /attributes/attr or attributes/attr)
        'MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1' | '[ { "op": "replace", "path":"/MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1", "value": "newUserLabel" } ] ' | _
    }

    @Unroll
    def 'JSON_PATCH - Execute modify ENodeBFunction with wrong attribute name return fail'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        writeResponse.isErrored()
        writeResponse.moObjects == null
        writeResponse.cmObjects == null
        writeResponse.errorResponseType != null
        writeResponse.errorResponseType.error.errorInfo.contains("An unknown attribute has been encountered")
        writeResponse.httpCode == 417

        where:
        xpath | body                                                                                                                         | _
        "MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1"      | '[ { "op": "replace", "path":"/attributes/invalidAttr", "value": "newUserLabel" } ] ' | _
        "MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1"      | '[ { "op": "replace", "path":"attributes/invalidAttr", "value": "newUserLabel" } ] ' | _
    }

    @Unroll
    def 'JSON_PATCH - Execute modify with invalid fdn return fail'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        writeResponse.isErrored()
        writeResponse.moObjects == null
        writeResponse.cmObjects == null
        writeResponse.errorResponseType != null
        writeResponse.errorResponseType.error.errorInfo.contains("The supplied FDN MeContext=ERBS002,ManagedElement=1,ENodeBFunction=xxx does not exist")
        writeResponse.httpCode == 404

        where:
        xpath | body                                                                                                                         | _
        "MeContext=ERBS002/ManagedElement=1/ENodeBFunction=xxx"      | '[ { "op": "replace", "path":"/attributes/userlabel", "value": "newUserLabel" } ] ' | _
    }

    @Unroll
    def 'JSON_PATCH - Execute modify ENodeBFunction attribute with success'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        System.out.println("responses=" + responses)
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        !writeResponse.isErrored()
        def resource = ((List)writeResponse.cmObjects).get(0)
        with (resource) {
            name == '1'
            type == "ENodeBFunction"
            attributes.get("userLabel") == 'newUserLabel'
            !attributes.containsKey('eNodeBPlmnId')
        }

        where:
        xpath      |  body                                                                                                                                                                       | _
        "MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1"      | '[ { "op": "replace", "path":"attributes/userlabel", "value": "newUserLabel" } ] ' | _
        "MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1"      | '[ { "op": "replace", "path":"/attributes/userlabel", "value": "newUserLabel" } ] ' | _
    }

    @Unroll
    def 'JSON_PATCH - Execute modify ENodeBFunction complex attribute with success'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        System.out.println("responses=" + responses)
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        !writeResponse.isErrored()
        def resource = ((List)writeResponse.cmObjects).get(0)
///        def resource = ((List)writeResponse.moObjects.moObjects.get("ENodeBFunction")).iterator().next()
        with (resource) {
            name == '1'
            type == "ENodeBFunction"
            attributes.get("eNodeBPlmnId") == [mcc:352, mnc:55, mncLength:2]
            !attributes.containsKey('userLabel')
        }

        where:
        xpath                                                             | body                                                                                                                                                                      | _
        "MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1"      | '[ { "op": "replace", "path":"attributes/eNodeBPlmnId", "value": { "mncLength" : 2, "mcc" : 352, "mnc" : 55 } } ] ' | _
    }

    @Unroll
    def 'JSON_PATCH - Execute modify ENodeBFunction complex attribute (partial) with success'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        System.out.println("responses=" + responses)
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        !writeResponse.isErrored()
        def resource = ((List)writeResponse.cmObjects).get(0)
///        def resource = ((List)writeResponse.moObjects.moObjects.get("ENodeBFunction")).iterator().next()
        with (resource) {
            name == '1'
            type == "ENodeBFunction"
            attributes.get("eNodeBPlmnId") == [mcc:533, mnc:77, mncLength:2]
            !attributes.containsKey('userLabel')
        }

        where:
        xpath                                                      | body                                                                                                                                                                      | _
        "MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1"      | '[ { "op": "replace", "path":"attributes/eNodeBPlmnId/mnc", "value": 77 } ] ' | _
    }

    /*
    *                 op=remove requests
    *
    * */

    @Unroll
    def 'JSON_PATCH - Execute remove NetworkElement with path not null return fail'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath , body, PatchContentType.JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        writeResponse.isErrored()
        writeResponse.moObjects == null
        writeResponse.cmObjects == null
        writeResponse.errorResponseType != null
        writeResponse.errorResponseType.error.errorInfo.contains("The supplied parameters")
        writeResponse.httpCode == 400

        where:
        xpath                                  | body | _
        'NetworkElement=LTETA0001FDD'      | '[ { "op": "remove", "path": "NetworkElement=LTETA0001FDD" } ]' | _
        'NetworkElement=LTETA0001FDD'      | '[ { "op": "remove", "path": "NetworkElement=LTETA0001FDDxx" } ]' | _
    }

    @Unroll
    def 'JSON_PATCH - Execute remove with invalid fdn return fail'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        writeResponse.isErrored()
        writeResponse.moObjects == null
        writeResponse.cmObjects == null
        writeResponse.errorResponseType != null
        writeResponse.errorResponseType.error.errorInfo.contains("The supplied FDN MeContext=ERBS002,ManagedElement=1,ENodeBFunction=xxx does not exist")
        writeResponse.httpCode == 404 || writeResponse.httpCode == 405

        where:
        xpath | body                                                                                                                         | _
        //here check is in crud (httpCode 404)
        "MeContext=ERBS002/ManagedElement=1/ENodeBFunction=xxx"      | '[ { "op": "remove", "path":"" } ] ' | _
    }

    @Unroll
    def 'JSON_PATCH - Execute remove ENodeBFunction return success'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 1
        WriteResponse writeResponse = responses.get(0)
        !writeResponse.isErrored()
        def resource = ((List)writeResponse.cmObjects).get(0)
        with(resource) {
            name == 'toBeRemoved'
            type == "EUtranCellFDD"
        }

        where:
        xpath                                                          | body                                                                                                                                                                                                                                                                                                                                                         | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=toBeRemoved"    | '[ { "op": "remove"} ]' | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=toBeRemoved"    | '[ { "op": "remove", "path": null } ]' | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=toBeRemoved"    | '[ { "op": "remove", "path": "" } ]' | _
    }

    /*
    *                 mixed requests
    *
    * */
    @Unroll
    def 'JSON_PATCH - Execute add/replace EUtranCellFDD return success'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 2

        //response 1
        WriteResponse writeResponse = responses.get(0)
        !writeResponse.isErrored()
        def resource = ((List)writeResponse.cmObjects).get(0)
        with(resource) {
            name == '20'
            type == "EUtranCellFDD"
            attributes.get("EUtranCellFDDId") == '20'
            attributes.get("userLabel") == '22'
            attributes.get("earfcndl") == 3
            attributes.get("earfcnul") == 18000
        }

        //response 2
        WriteResponse writeResponse2 = responses.get(1)
        !writeResponse2.isErrored()
        def resource2 = ((List)writeResponse2.cmObjects).get(0)
        with(resource2) {
            name == '20'
            type == "EUtranCellFDD"
            attributes.get("userLabel") == 'newUserLabel'
            !attributes.containsKey('EUtranCellFDDId')
            !attributes.containsKey('earfcndl')
        }

        where:
        xpath                                                                        | body                                                                                                                                                                                                                                                                                                                                                         | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20" | '[ { "op": "add", "path": "", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } }, { "op": "replace", "path":"attributes/userlabel", "value": "newUserLabel" } ]' | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20" | '[ { "op": "add", "path": "", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } }, { "op": "replace", "path":"/attributes/userlabel", "value": "newUserLabel" } ]' | _
    }

    @Unroll
    def 'JSON_PATCH - Execute add/replace EUtranCellFDD return fail if error in last replace request (invalidAttr)'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.JSON_PATCH)

        when:
        List<WriteResponse> responses = objUnderTest.executor(nbiCrudPatchRequest)
        then:
        responses.size() == 2

        //response 1
        WriteResponse writeResponse = responses.get(0)
        !writeResponse.isErrored()
        def resource = ((List)writeResponse.cmObjects).get(0)
        with(resource) {
            name == '20'
            type == "EUtranCellFDD"
            attributes.get("EUtranCellFDDId") == '20'
            attributes.get("userLabel") == '22'
            attributes.get("earfcndl") == 3
            attributes.get("earfcnul") == 18000
        }

        //response 2
        WriteResponse writeResponse2 = responses.get(1)
        writeResponse2.isErrored()
        writeResponse2.moObjects == null
        writeResponse2.cmObjects == null
        writeResponse2.errorResponseType != null
        writeResponse2.errorResponseType.error.errorInfo.contains("An unknown attribute has been encountered; name invalidAttr in the MO class EUtranCellFDD.")
        writeResponse2.httpCode == 417

        where:
        xpath                                                                        | body                                                                                                                                                                                                                                                                                                                                                         | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20" | '[ { "op": "add", "path": "", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } }, { "op": "replace", "path":"attributes/invalidAttr", "value": "newUserLabel" } ]' | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20" | '[ { "op": "add", "path": "", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } }, { "op": "replace", "path":"/attributes/invalidAttr", "value": "newUserLabel" } ]' | _
    }

    /*
  TEST HTTP RETURN CODES
  We are using:
  200  OK
  400  for crud validation error
  401  for TBAC invalid fdn
  404  for invalid fdn
  404  for cm-reader/cm-writer response empty (NA in this scenario)
  417 for cm-reader/cm-writer error
  405  for unexpected exception
*/

    @Unroll
    def '3GPP_JSON_PATCH - Execute nbiCrudPatchRequest with objUnderTest.executeOperation (mixed operations1) return success'() {
        given: 'NbiCrudPatchRequest'
        //create  MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20
        //replace MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1 userlabel=newUserLabel
        //remove  MeContext=ERBS001FORNBI/ManagedElement=1//EUtranCellFDD=toBeRemoved
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPatchRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 200
            nbiResponse.jsonContent.contains('"EUtranCellFDD":[{"id":"20",')  //add EUtranCellFDD
            nbiResponse.jsonContent.contains('"zzzTemporary43":-2000000000')  //add EUtranCellFDD
 //           nbiResponse.jsonContent.contains('{"EUtranCellFDD":{"id":"toBeRemoved"}}') //remove
            !nbiResponse.jsonContent.contains('"EUtranCellFDD":{"id":"toBeRemoved"}}') //remove
        }

        where:
        xpath                                                                        | body                                                                                                                                                                                                                                                                                                                                                         | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1" | '[ { "op": "add", "path": "/EUtranCellFDD=20", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } }, { "op": "replace", "path":"#attributes/userlabel", "value": "newUserLabel" }, { "op": "remove", "path": "/EUtranCellFDD=toBeRemoved" } ]' | _
    }

    @Unroll
    def '3GPP_JSON_PATCH - Execute nbiCrudPatchRequest with objUnderTest.executeOperation (mixed operations2) return success'() {
        given: 'NbiCrudPatchRequest'
        //create  MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20
        //replace MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20 zzzTemporary43=10
        //replace MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1 userlabel=newUserLabel
        //remove  MeContext=ERBS001FORNBI/ManagedElement=1//EUtranCellFDD=toBeRemoved
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPatchRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 200
            nbiResponse.jsonContent.contains('"EUtranCellFDD":[{"id":"20",')  //add
            nbiResponse.jsonContent.contains('"zzzTemporary43":10')            //modify EUtranCellFDD zzzTemporary43=10
            nbiResponse.jsonContent.contains('{"ENodeBFunction":{"id":"1","attributes":{"userLabel":"newUserLabel"}') //replace
            !nbiResponse.jsonContent.contains('"EUtranCellFDD":{"id":"toBeRemoved"}}') //remove
        }

        where:
        xpath                                                                        | body                                                                                                                                                                                                                                                                                                                                                         | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1" | '[ { "op": "add", "path": "/EUtranCellFDD=20", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } }, { "op": "replace", "path":"/EUtranCellFDD=20#attributes/zzzTemporary43", "value": 10 }, { "op": "replace", "path":"#attributes/userlabel", "value": "newUserLabel" }, { "op": "remove", "path": "/EUtranCellFDD=toBeRemoved" } ]' | _
    }

    @Unroll
    def '3GPP_JSON_PATCH - Execute nbiCrudPatchRequest with objUnderTest.executeOperation (mixed operations3) return success'() {
        given: 'NbiCrudPatchRequest'
        //create  MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20
        //replace MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20 zzzTemporary43=10
        //replace MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1 userlabel=newUserLabel
        //remove  MeContext=ERBS001FORNBI/ManagedElement=1//EUtranCellFDD=1/EUtranCellFDD=20
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPatchRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 200
            !nbiResponse.jsonContent.contains('"EUtranCellFDD":[{"id":"20",')  //add and remove
            !nbiResponse.jsonContent.contains('"zzzTemporary43":10')            //modify EUtranCellFDD zzzTemporary43=10
            nbiResponse.jsonContent.contains('{"ENodeBFunction":{"id":"1","attributes":{"userLabel":"newUserLabel"}') //replace
        }

        where:
        xpath                                                                        | body                                                                                                                                                                                                                                                                                                                                                         | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1" | '[ { "op": "add", "path": "/EUtranCellFDD=20", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } }, { "op": "replace", "path":"/EUtranCellFDD=20#attributes/zzzTemporary43", "value": 10 }, { "op": "replace", "path":"#attributes/userlabel", "value": "newUserLabel" }, { "op": "remove", "path": "/EUtranCellFDD=20" } ]' | _
    }

    @Unroll
    def '3GPP_JSON_PATCH - Execute nbiCrudPatchRequest with objUnderTest.executeOperation (mixed operations4) return success'() {
        given: 'NbiCrudPatchRequest'
        //create  MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20
        //remove  MeContext=ERBS001FORNBI/ManagedElement=1//EUtranCellFDD=1/EUtranCellFDD=20
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPatchRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 200
            nbiResponse.jsonContent == '{}'
        }

        where:
        xpath                                                                        | body                                                                                                                                                                                                                                                                                                                                                         | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1" | '[ { "op": "add", "path": "/EUtranCellFDD=20", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } }, { "op": "remove", "path": "/EUtranCellFDD=20" } ]' | _
    }

    @Unroll
    def '3GPP_JSON_PATCH - Execute nbiCrudPatchRequest with objUnderTest.executeOperation (mixed operations) return fail if error in last remove request'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPatchRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 417
            nbiResponse.jsonContent.contains("The supplied FDN MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=xxx does not exist in the database")
        }

        where:
        xpath                                                                        | body                                                                                                                                                                                                                                                                                                                                                         | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1" | '[ { "op": "add", "path": "/EUtranCellFDD=20", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } }, { "op": "replace", "path":"#attributes/userlabel", "value": "newUserLabel" }, { "op": "remove", "path": "/EUtranCellFDD=xxx" } ]' | _
    }

    def '3GPP_JSON_PATCH - Execute nbiCrudPatchRequest with objUnderTest.executeOperation (mixed operations) return fail when invalid Json'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPatchRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 400
            nbiResponse.jsonContent.contains('Invalid json in body:')
        }

        where:
        xpath                                                       | body                  | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1" | '{ invalidjson }'     | _
    }

    def '3GPP_JSON_PATCH - Execute nbiCrudPatchRequest with objUnderTest.executeOperation (mixed operations) return fail when invalid TBAC node'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.THREE_GPP_JSON_PATCH)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPatchRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 401
            nbiResponse.jsonContent.contains('Access Denied')
        }

        where:
        xpath                                                                        | body                                                                                                                                                                                                                                                                                                                                                         | _
        "MeContext=ERBS001/ManagedElement=1/ENodeBFunction=1" | '[ { "op": "add", "path": "/EUtranCellFDD=20", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } }, { "op": "replace", "path":"#attributes/userlabel", "value": "newUserLabel" }, { "op": "remove", "path": "/EUtranCellFDD=xxx" } ]' | _
    }

    @Unroll
    def 'JSON_PATCH - Execute nbiCrudPatchRequest with objUnderTest.executeOperation (mixed operations) return success'() {
        given: 'NbiCrudPatchRequest'
        //create  MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20
        //replace MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20 userlabel=newUserLabel
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.JSON_PATCH)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPatchRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 200
            nbiResponse.jsonContent.contains('{"ENodeBFunction":{"id":"1","EUtranCellFDD":[{"id":"20","attributes":{')  //add
            nbiResponse.jsonContent.contains(',"userLabel":"newUserLabel",') //replace
        }

        where:
        xpath                                                                        | body                                                                                                                                                                                                                                                                                                                                                         | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20" | '[ { "op": "add", "path": "", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } }, { "op": "replace", "path":"/attributes/userlabel", "value": "newUserLabel" } ]' | _
    }

    @Unroll
    def 'JSON_PATCH - Execute nbiCrudPatchRequest with objUnderTest.executeOperation (mixed operations2) return success'() {
        given: 'NbiCrudPatchRequest'
        //create  MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20
        //remove  MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.JSON_PATCH)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPatchRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 200
            nbiResponse.jsonContent == '{}'
        }

        where:
        xpath                                                                        | body                                                                                                                                                                                                                                                                                                                                                         | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20" | '[ { "op": "add", "path": "", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } }, { "op": "remove", "path":"" } ]' | _
    }

    @Unroll
    def 'JSON_PATCH - Execute nbiCrudPatchRequest with objUnderTest.executeOperation (mixed operations) return fail if error in last replace request (invalidAttr)'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.JSON_PATCH)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPatchRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 417
            nbiResponse.jsonContent == '{"error":{"errorInfo":"An unknown attribute has been encountered; name invalidAttr in the MO class EUtranCellFDD."}}'
        }

        where:
        xpath                                                                        | body                                                                                                                                                                                                                                                                                                                                                         | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20" | '[ { "op": "add", "path": "", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } }, { "op": "replace", "path":"/attributes/invalidAttr", "value": "newUserLabel" } ]' | _
    }

    def 'JSON_PATCH - Execute nbiCrudPatchRequest with objUnderTest.executeOperation (mixed operations) return fail when invalid Json'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.JSON_PATCH)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPatchRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 400
            nbiResponse.jsonContent.contains('Invalid json in body:')
        }

        where:
        xpath                                                                        | body   | _
        "MeContext=ERBS001FORNBI/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20" | '{ invalidjson }'     | _
    }

    def 'JSON_PATCH - Execute nbiCrudPatchRequest with objUnderTest.executeOperation (mixed operations) return fail when invalid TBAC node'() {
        given: 'NbiCrudPatchRequest'
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, body, PatchContentType.JSON_PATCH)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudPatchRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 401
            nbiResponse.jsonContent.contains('Access Denied')
        }

        where:
        xpath                                                                        | body                                                                                                                                                                                                                                                                                                                                                         | _
        "MeContext=ERBS001/ManagedElement=1/ENodeBFunction=1/EUtranCellFDD=20" | '[ { "op": "add", "path": "", "value": { "id": "20", "attributes": { "EUtranCellFDDId":"2022" , "userLaBEL":22, "physicalLayerSubCellId":"1", "earfcndl":3, "earfcnul":18000, "tac":"2", "physicalLayerCellIdGroup":"1", "cellId":"1", "testObsoleteNonPersistent":"test" } } }, { "op": "replace", "path":"/attributes/invalidAttr", "value": "newUserLabel" } ]' | _
    }

    /*
    TEST SINGLE METHODS
    */
    def 'CmPatchRequest for_coverage_only'() {
        when:
        CmPatchRequest cmPatchRequest = new CmPatchRequest("", "", new ArrayList<>())
        ErroredWriteResponse erroredWriteResponse = new ErroredWriteResponse(new ErrorResponseType("error"), 400)

        then:
        cmPatchRequest.toString()
        erroredWriteResponse.getResponseType()
        erroredWriteResponse.toString()
    }

    def 'getFirstErroredResponse with no errored response return null'() {
        when: 'invoke getFirstErroredResponse'
        def erroredResponse = objUnderTest.getFirstErroredResponse(new ArrayList<WriteResponse>())

        then:
        erroredResponse == null
    }

    def 'dummy test to force maven to run test class with all other test methods annotated with @Unroll'() {
        when:
        objUnderTest.toString()
        then:
        1 == 1
    }

}