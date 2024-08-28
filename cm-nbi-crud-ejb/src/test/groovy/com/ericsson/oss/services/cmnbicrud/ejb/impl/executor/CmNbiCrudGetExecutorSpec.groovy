/*
    This class, and model class are copied from SetCommandSpec.groovy
 */
package com.ericsson.oss.services.cmnbicrud.ejb.impl.executor
import com.ericsson.cds.cdi.support.providers.custom.sfwk.PropertiesForTest
import com.ericsson.cds.cdi.support.providers.custom.sfwk.SuppliedProperty
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.itpf.sdk.security.accesscontrol.EAccessControl
import com.ericsson.oss.presentation.cmnbirest.api.NbiResponse
import com.ericsson.oss.services.cm.cmshared.dto.CmResponse
import com.ericsson.oss.services.cm.cmshared.dto.search.CmSearchCriteria
import com.ericsson.oss.services.cmnbicrud.ejb.common.ResponseType
import com.ericsson.oss.services.cmnbicrud.ejb.get.CmGetRequest
import com.ericsson.oss.services.cmnbicrud.ejb.get.CmGetResponse
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudGetRequest
import spock.lang.Unroll

import com.ericsson.oss.services.cmnbicrud.ejb.testutil.BaseForCommandReceiverSpecs
import com.ericsson.oss.services.cmnbicrud.ejb.testutil.TestUtil
import com.ericsson.oss.services.cm.cmsearch.DynamicBlackListInfoIf
import com.ericsson.oss.services.cm.cmsearch.excessivedata.DynamicBlackListEntry

import javax.inject.Inject

class CmNbiCrudGetExecutorSpec extends BaseForCommandReceiverSpecs {
    @ObjectUnderTest
    private CmNbiCrudGetExecutor objUnderTest

    @Inject
    private TestUtil testUtil

    @Inject
    DynamicBlackListInfoIf mockedDynamicBlackListInfoIf;

    @Inject
    private EAccessControl mockedAccessControl;

    final CmSearchCriteria cmSearchCriteria = new CmSearchCriteria()

    String userId = "user"
    String requestId = "crud:1234"
    String fields = null
    String attributes = null
    String filter = null
    String ipAddress = "1.2.3.5"
    String ssoToken = "CookieAABBCCDDEE";

    static def ROOT_MO = null

    def setup() {
        mockedDynamicBlackListInfoIf.isAvailable() >> true
        mockedDynamicBlackListInfoIf.getDynamicBlackListMap() >> new HashMap<String, DynamicBlackListEntry>()
        mockedDynamicBlackListInfoIf.getDynamicBlackListMapForApi() >> new HashMap<String, DynamicBlackListEntry>()
        mockedDynamicBlackListInfoIf.getDynamicBlackListMapForNbi() >> new HashMap<String, DynamicBlackListEntry>()
        runtimeDps.withTransactionBoundaries()
    }

    /*
         CHECK INPUT PARAMETERS
     */

    def 'Execute nbiCrudGetRequest with xpath=null return fail'() {
        given: 'nbiCrudGetRequest'
        def xpath = null
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, xpath , "BASE_ONLY", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            moObjects == null
            errorResponseType.error.errorInfo.contains("The supplied parameters xpath=null is not allowed.")
            httpCode == 400
        }
    }

    def 'Execute nbiCrudGetRequest with invalid scopeType return fail'() {
        given: 'nbiCrudGetRequest'
        String scopeType = "INVALID"
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , scopeType, fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            moObjects == null
            errorResponseType.error.errorInfo.contains("The supplied parameters scopeType=INVALID is not allowed.")
            httpCode == 400
        }
    }

    def 'Execute nbiCrudGetRequest with scopeLevel < 0 return fail'() {
        given: 'nbiCrudGetRequest'
        int scopeLevel = -1
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , "BASE_ONLY", fields, attributes, scopeLevel, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            moObjects == null
            errorResponseType.error.errorInfo.contains("The supplied parameters scopeLevel=-1 is not allowed.")
            httpCode == 400
        }
    }

    def 'Execute nbiCrudGetRequest with invalid fields return fail'() {
        given: 'nbiCrudGetRequest'
        String fields = "userLabel" //valid parameter should be attributes/aa or attributes/aa/bb
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , "BASE_ONLY", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            moObjects == null
            errorResponseType.error.errorInfo.contains("The supplied parameters fields=userLabel is not allowed.")
            httpCode == 400
        }
    }

    /*
      DB CHECKS
     */
    @Unroll
    def 'Execute nbiCrudGetRequest with everyScope and invalid Fdn return fail'() {
        given: 'nbiCrudGetRequest'
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=xxx" , scopeType, fields, attributes, 0, getfilter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            moObjects == null
            errorResponseType.error.errorInfo.contains("does not exist in the database")
            httpCode == 404
        }

        where:
        scopeType      | getfilter |_
        "BASE_ONLY"  |  null | _
        "BASE_ALL"   | null | _
        "BASE_SUBTREE"  | null | _
        "BASE_NTH_LEVEL"  | null | _
        "BASE_ALL"   | "some filter " | _
    }


    /*
        BASE_ONLY
     */

    def 'Execute nbiCrudGetRequest with BASE_ONLY and filter!=null return fail'() {
        given: 'nbiCrudGetRequest'
        String filter = "/MeContext/ManagedElement/ENodeBFunction[attributes/USERLABEL=\"value\"]";
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , "BASE_ONLY", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            moObjects == null
            errorResponseType.error.errorInfo.contains("Unimplemented Use Case for scopeType=BASE_ONLY")
            httpCode == 422
        }
    }

    def 'Execute nbiCrudGetRequest for NetworkElement with BASE_ONLY and attributes=null return success'() {
        given: 'nbiCrudGetRequest'
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , "BASE_ONLY", fields, attributes, 0, filter)

        when:
            CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
            with(cmGetResponse) {
                responseType == ResponseType.SUCCESS
                errorResponseType == null
                moObjects.moObjects.size() == 1
                httpCode == 200
                def resource = ((List)moObjects.moObjects.get("NetworkElement")).iterator().next()
                with (resource) {
                    id == 'ERBS002'
                    resource.attributes.get("neType") == 'ERBS'
                    resource.attributes.get("technologyDomain") == ['3G', '4G', '5G']

                    resource.toString().contains("neType")
                    resource.toString().contains("technologyDomain")
                }
            }
    }

    def 'Execute nbiCrudGetRequest for NetworkElement with BASE_ONLY and attributes empty return success'() {
        given: 'nbiCrudGetRequest'
        attributes = ""
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , "BASE_ONLY", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 200
            def resource = ((List)moObjects.moObjects.get("NetworkElement")).iterator().next()
            with (resource) {
                id == 'ERBS002'

                !resource.toString().contains("neType")
                !resource.toString().contains("technologyDomain")
            }
        }
    }

    def 'Execute nbiCrudGetRequest for NetworkElement with BASE_ONLY and fields empty return success'() {
        given: 'nbiCrudGetRequest'
        fields = ""
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , "BASE_ONLY", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 200
            def resource = ((List)moObjects.moObjects.get("NetworkElement")).iterator().next()
            with (resource) {
                id == 'ERBS002'

                !resource.toString().contains("neType")
                !resource.toString().contains("technologyDomain")
            }
        }
    }


    def 'Execute nbiCrudGetRequest for NetworkElement with BASE_ONLY and attributes specified return success'() {
        given: 'nbiCrudGetRequest'
        attributes = "NETYPE"
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , "BASE_ONLY", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 200
            def resource = ((List)moObjects.moObjects.get("NetworkElement")).iterator().next()
            with (resource) {
                id == 'ERBS002'

                resource.toString().contains("neType")
                !resource.toString().contains("technologyDomain")
            }
        }
    }

    def 'Execute nbiCrudGetRequest for NetworkElement with BASE_ONLY and invalid attribute return fail'() {
        given: 'nbiCrudGetRequest'
        attributes = "invalid"
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , "BASE_ONLY", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            moObjects == null
            errorResponseType.error.errorInfo.contains("An unknown attribute has been encountered")
            httpCode == 417
        }
    }

    def 'Execute nbiCrudGetRequest for NetworkElement with BASE_ONLY and invalid attribute (no members) return fail'() {
        given: 'nbiCrudGetRequest'
        fields = "attributes/NETYPE/AAA"
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , "BASE_ONLY", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            moObjects == null
            errorResponseType.error.errorInfo.contains('An incorrect attribute data type has been encountered for NETYPE expected type ENUM_REF, but received COMPLEX_REF')
            httpCode == 417
        }
    }

    def 'Execute nbiCrudGetRequest for ENodeBFunction with BASE_ONLY and complex attribute specified return success'() {
        given: 'nbiCrudGetRequest'
        attributes = "ENODEBPlmnId"
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1" , "BASE_ONLY", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 200
            def resource = ((List)moObjects.moObjects.get("ENodeBFunction")).iterator().next()
            with (resource) {
                id == '1'

                resource.toString().contains("eNodeBPlmnId") //EnodebFunction
                resource.toString().contains("mcc") //EnodebFunction eNodeBPlmnId member
                resource.toString().contains("mnc") //EnodebFunction eNodeBPlmnId member
                resource.toString().contains("mncLength") //EnodebFunction eNodeBPlmnId member
            }
        }
    }

    def 'Execute nbiCrudGetRequest for ENodeBFunction with BASE_ONLY and complex member attribute specified return success'() {
        given: 'nbiCrudGetRequest'
        fields = "attributes/ENODEBPlmnId/MCC"
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1" , "BASE_ONLY", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 200
            def resource = ((List)moObjects.moObjects.get("ENodeBFunction")).iterator().next()
            with (resource) {
                id == '1'

                resource.toString().contains("eNodeBPlmnId") //EnodebFunction
                resource.toString().contains("mcc") //EnodebFunction eNodeBPlmnId member
                !resource.toString().contains("mnc") //EnodebFunction eNodeBPlmnId member
                !resource.toString().contains("mncLength") //EnodebFunction eNodeBPlmnId member
            }
        }
    }

    def 'Execute nbiCrudGetRequest for ENodeBFunction with BASE_ONLY and invalid complex member attribute specified return fail'() {
        given: 'nbiCrudGetRequest'
        fields = "attributes/ENODEBPlmnId/AAA"
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1" , "BASE_ONLY", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            moObjects == null
            errorResponseType.error.errorInfo.contains("An unknown struct member has been encountered; name AAA in the struct with dataType PlmnIdentity and name ENODEBPlmnId.")
            httpCode == 417
        }
    }


    /*
        BASE_ALL         WITH FILTER = null
        BASE_SUBTREE     WITH FILTER = null
        BASE_NTH_LEVEL   WITH FILTER = null
    */

    def 'Execute nbiCrudGetRequest for NetworkElement with BASE_ALL and attributes=null return success'() {
        given: 'nbiCrudGetRequest'
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , "BASE_ALL", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 200
            def resource = ((List)moObjects.moObjects.get("NetworkElement")).iterator().next()
            with (resource) {
                id == 'ERBS002'
                resource.attributes.get("neType") == 'ERBS'
                resource.attributes.get("technologyDomain") == ['3G', '4G', '5G']

                resource.toString().contains("neType")
                resource.toString().contains("technologyDomain")
            }
        }
    }

    def 'Execute nbiCrudGetRequest for ENodeBFunction with BASE_ALL and attributes=null return success'() {
        given: 'nbiCrudGetRequest'
            NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002/ManagedElement=1/ENodeBFunction=1" , "BASE_ALL", fields, attributes, 0, filter)

        when:
            CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 200
            def resource = ((List)moObjects.moObjects.get("ENodeBFunction")).iterator().next()
            with (resource) {
                id == '1'
                resource.toString().contains("EUtranCellFDD")

                //check some persistent attributes in some Mo
                !resource.toString().contains("MeContextId") //MeContext
                !resource.toString().contains("productName") //ManagedElement
                resource.toString().contains("timePhaseMaxDeviationOtdoa") //EnodebFunction
                resource.toString().contains("zzzTemporary43") //EutranCellFDD
            }
        }
    }

    def 'Execute nbiCrudGetRequest for MeContext with BASE_ALL and attributes=null return success'() {
        given: 'nbiCrudGetRequest'
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_ALL", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            moObjects.moObjects.size() == 1
            httpCode == 200
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                resource.id == 'ERBS002'
                resource.toString().contains("ManagedElement")
                resource.toString().contains("ENodeBFunction")
                resource.toString().contains("EUtranCellFDD")

                //check some persistent attributes in some Mo
                resource.toString().contains("MeContextId") //MeContext
                resource.toString().contains("productName") //ManagedElement
                resource.toString().contains("timePhaseMaxDeviationOtdoa") //EnodebFunction
                resource.toString().contains("zzzTemporary43") //EutranCellFDD
            }
        }
    }

    def 'Execute nbiCrudGetRequest for MeContext with BASE_ALL and 1 attributes specified return success'() {
        given: 'nbiCrudGetRequest'
        attributes = "NETYPE"
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_ALL", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            moObjects.moObjects.size() == 1
            httpCode == 200
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                resource.id == 'ERBS002'
                resource.toString().contains("ManagedElement")
                !resource.toString().contains("ENodeBFunction")
                !resource.toString().contains("EUtranCellFDD")

                //check some persistent attributes in some Mo
                resource.toString().contains("neType") //MeContext and ManagedElement
                !resource.toString().contains("testObsoletePersistent") //MeContext
                !resource.toString().contains("MeContextId") //MeContext
                !resource.toString().contains("productName") //ManagedElement
                !resource.toString().contains("timePhaseMaxDeviationOtdoa") //EnodebFunction
                !resource.toString().contains("zzzTemporary43") //EutranCellFDD
            }
        }
    }

    def 'Execute nbiCrudGetRequest for MeContext with BASE_ALL and 2 attributes specified return success'() {
        given: 'nbiCrudGetRequest'
        attributes = "NETYPE,TESTobsoletePersistenT"
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_ALL", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            moObjects.moObjects.size() == 1
            httpCode == 200
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                resource.id == 'ERBS002'
                !resource.toString().contains("ManagedElement")
                !resource.toString().contains("ENodeBFunction")
                !resource.toString().contains("EUtranCellFDD")

                //check some persistent attributes in some Mo
                resource.toString().contains("neType") //MeContext and ManagedElement
                resource.toString().contains("testObsoletePersistent") //MeContext
                !resource.toString().contains("MeContextId") //MeContext
                !resource.toString().contains("productName") //ManagedElement
                !resource.toString().contains("timePhaseMaxDeviationOtdoa") //EnodebFunction
                !resource.toString().contains("zzzTemporary43") //EutranCellFDD
            }
        }
    }

    @Unroll
    def 'Execute nbiCrudGetRequest for MeContext with BASE_ALL and invalid attribute return fail'() {
        given: 'nbiCrudGetRequest'
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_ALL", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            moObjects == null
            errorResponseType.error.errorInfo.contains("Invalid attributes or fields for all entries")
            httpCode == 417
        }

        where:
        attributes               | fields                      | _
        'invalid'                | null                       | _
        'netype,invalid'        | null                       | _
        'mecontextid,invalid'  | null                       | _
        'mecontextid'           | 'attributes/NETYPE/AAA'   | _
        null                     | 'attributes/NETYPE/AAA'   | _
        null                     | 'attributes/ENODEBPlmnId/AAA'   | _
    }

    def 'Execute nbiCrudGetRequest for MeContext with BASE_ALL and complex attribute specified return success'() {
        given: 'nbiCrudGetRequest'
        attributes = "ENODEBPlmnId"
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_ALL", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            moObjects.moObjects.size() == 1
            httpCode == 200
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                resource.id == 'ERBS002'
                resource.toString().contains("ManagedElement")
                resource.toString().contains("ENodeBFunction")
                !resource.toString().contains("EUtranCellFDD")

                //check some persistent attributes in some Mo
                resource.toString().contains("eNodeBPlmnId") //EnodebFunction
                resource.toString().contains("mcc") //EnodebFunction eNodeBPlmnId member
                resource.toString().contains("mnc") //EnodebFunction eNodeBPlmnId member
                resource.toString().contains("mncLength") //EnodebFunction eNodeBPlmnId member

                !resource.toString().contains("neType") //MeContext and ManagedElement
                !resource.toString().contains("testObsoletePersistent") //MeContext
                !resource.toString().contains("MeContextId") //MeContext
                !resource.toString().contains("productName") //ManagedElement
                !resource.toString().contains("timePhaseMaxDeviationOtdoa") //EnodebFunction
                !resource.toString().contains("zzzTemporary43") //EutranCellFDD
            }
        }
    }

    def 'Execute nbiCrudGetRequest for MeContext with BASE_ALL and complex member attribute specified return success'() {
        given: 'nbiCrudGetRequest'
        fields = "attributes/ENODEBPlmnId/MCC"
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_ALL", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            moObjects.moObjects.size() == 1
            httpCode == 200
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                resource.id == 'ERBS002'
                resource.toString().contains("ManagedElement")
                resource.toString().contains("ENodeBFunction")
                !resource.toString().contains("EUtranCellFDD")

                //check some persistent attributes in some Mo
                resource.toString().contains("eNodeBPlmnId") //EnodebFunction
                resource.toString().contains("mcc") //EnodebFunction eNodeBPlmnId member
                !resource.toString().contains("mnc") //EnodebFunction eNodeBPlmnId member
                !resource.toString().contains("mncLength") //EnodebFunction eNodeBPlmnId member

                !resource.toString().contains("neType") //MeContext and ManagedElement
                !resource.toString().contains("testObsoletePersistent") //MeContext
                !resource.toString().contains("MeContextId") //MeContext
                !resource.toString().contains("productName") //ManagedElement
                !resource.toString().contains("timePhaseMaxDeviationOtdoa") //EnodebFunction
                !resource.toString().contains("zzzTemporary43") //EutranCellFDD
            }
        }
    }

    def 'Execute nbiCrudGetRequest for MeContext with BASE_ALL and attributes empty return success'() {
        given: 'nbiCrudGetRequest'
        attributes = ""
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_ALL", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 200
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                resource.id == 'ERBS002'
                resource.attributes == null
                resource.toString().contains("ManagedElement")
                resource.toString().contains("ENodeBFunction")
                resource.toString().contains("EUtranCellFDD")

                //check some persistent attributes in some Mo
                !resource.toString().contains("MeContextId") //MeContext
                !resource.toString().contains("productName") //ManagedElement
                !resource.toString().contains("timePhaseMaxDeviationOtdoa") //EnodebFunction
                !resource.toString().contains("zzzTemporary43") //EutranCellFDD
            }
        }
    }

    def 'Execute nbiCrudGetRequest for MeContext with BASE_SUBTREE and attributes=null and scopeLevel=0 return success'() {
        given: 'nbiCrudGetRequest'
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_SUBTREE", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 200
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                id == 'ERBS002'
                !resource.toString().contains("ManagedElement")
                !resource.toString().contains("ENodeBFunction")
                !resource.toString().contains("EUtranCellFDD")

                //check some persistent attributes in some Mo
                resource.toString().contains("MeContextId") //MeContext
                !resource.toString().contains("productName") //ManagedElement
                !resource.toString().contains("timePhaseMaxDeviationOtdoa") //EnodebFunction
                !resource.toString().contains("zzzTemporary43") //EutranCellFDD

            }
        }
    }

    def 'Execute nbiCrudGetRequest for MeContext with BASE_SUBTREE and attributes=null and scopeLevel=1 return success'() {
        given: 'nbiCrudGetRequest'
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_SUBTREE", fields, attributes, 1, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 200
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                resource.id == 'ERBS002'
                resource.toString().contains("ManagedElement")
                !resource.toString().contains("ENodeBFunction")
                !resource.toString().contains("EUtranCellFDD")

                //check some persistent attributes in some Mo
                resource.toString().contains("MeContextId") //MeContext
                resource.toString().contains("productName") //ManagedElement
                !resource.toString().contains("timePhaseMaxDeviationOtdoa") //EnodebFunction
                !resource.toString().contains("zzzTemporary43") //EutranCellFDD
            }
        }
    }

    def 'Execute nbiCrudGetRequest for MeContext with BASE_SUBTREE and 1 attributes specified and scopeLevel=3 return success'() {
        given: 'nbiCrudGetRequest'
        attributes = "NETYPE"
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_SUBTREE", fields, attributes, 3, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            moObjects.moObjects.size() == 1
            httpCode == 200
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                resource.id == 'ERBS002'
                resource.toString().contains("ManagedElement")
                !resource.toString().contains("ENodeBFunction")
                !resource.toString().contains("EUtranCellFDD")

                //check some persistent attributes in some Mo
                resource.toString().contains("neType") //MeContext and ManagedElement
                !resource.toString().contains("testObsoletePersistent") //MeContext
                !resource.toString().contains("MeContextId") //MeContext
                !resource.toString().contains("productName") //ManagedElement
                !resource.toString().contains("timePhaseMaxDeviationOtdoa") //EnodebFunction
                !resource.toString().contains("zzzTemporary43") //EutranCellFDD
            }
        }
    }

    def 'Execute nbiCrudGetRequest for MeContext with BASE_SUBTREE and 2 attributes specified and scopeLevel=3 return success'() {
        given: 'nbiCrudGetRequest'
        attributes = "NETYPE,TESTobsoletePersistenT"
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_SUBTREE", fields, attributes, 3, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            moObjects.moObjects.size() == 1
            httpCode == 200
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                resource.id == 'ERBS002'
                !resource.toString().contains("ManagedElement")
                !resource.toString().contains("ENodeBFunction")
                !resource.toString().contains("EUtranCellFDD")

                //check some persistent attributes in some Mo
                resource.toString().contains("neType") //MeContext and ManagedElement
                resource.toString().contains("testObsoletePersistent") //MeContext
                !resource.toString().contains("MeContextId") //MeContext
                !resource.toString().contains("productName") //ManagedElement
                !resource.toString().contains("timePhaseMaxDeviationOtdoa") //EnodebFunction
                !resource.toString().contains("zzzTemporary43") //EutranCellFDD
            }
        }
    }

    @Unroll
    def 'Execute nbiCrudGetRequest for MeContext with BASE_SUBTREE and invalid attribute and scopeLevel=3 return fail'() {
        given: 'nbiCrudGetRequest'
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_SUBTREE", fields, attributes, 3, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            moObjects == null
            errorResponseType.error.errorInfo.contains("Invalid attributes or fields for all entries")
            httpCode == 417
        }

        where:
        attributes               | fields                      | _
        'invalid'                | null                       | _
        'netype,invalid'        | null                       | _
        'mecontextid,invalid'  | null                       | _
        'mecontextid'           | 'attributes/NETYPE/AAA'   | _
        null                     | 'attributes/NETYPE/AAA'   | _
        null                     | 'attributes/ENODEBPlmnId/AAA'   | _
    }

    def 'Execute nbiCrudGetRequest for MeContext with BASE_SUBTREE and complex attribute specified and scopeLevel=3 return success'() {
        given: 'nbiCrudGetRequest'
        attributes = "ENODEBPlmnId"
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_SUBTREE", fields, attributes, 3, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            moObjects.moObjects.size() == 1
            httpCode == 200
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                resource.id == 'ERBS002'
                resource.toString().contains("ManagedElement")
                resource.toString().contains("ENodeBFunction")
                !resource.toString().contains("EUtranCellFDD")

                //check some persistent attributes in some Mo
                resource.toString().contains("eNodeBPlmnId") //EnodebFunction
                resource.toString().contains("mcc") //EnodebFunction eNodeBPlmnId member
                resource.toString().contains("mnc") //EnodebFunction eNodeBPlmnId member
                resource.toString().contains("mncLength") //EnodebFunction eNodeBPlmnId member

                !resource.toString().contains("neType") //MeContext and ManagedElement
                !resource.toString().contains("testObsoletePersistent") //MeContext
                !resource.toString().contains("MeContextId") //MeContext
                !resource.toString().contains("productName") //ManagedElement
                !resource.toString().contains("timePhaseMaxDeviationOtdoa") //EnodebFunction
                !resource.toString().contains("zzzTemporary43") //EutranCellFDD
            }
        }
    }

    def 'Execute nbiCrudGetRequest for MeContext with BASE_SUBTREE and complex member attribute specified and scopeLevel=3 return success'() {
        given: 'nbiCrudGetRequest'
        fields = "attributes/ENODEBPlmnId/MCC"
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_SUBTREE", fields, attributes, 3, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            moObjects.moObjects.size() == 1
            httpCode == 200
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                resource.id == 'ERBS002'
                resource.toString().contains("ManagedElement")
                resource.toString().contains("ENodeBFunction")
                !resource.toString().contains("EUtranCellFDD")

                //check some persistent attributes in some Mo
                resource.toString().contains("eNodeBPlmnId") //EnodebFunction
                resource.toString().contains("mcc") //EnodebFunction eNodeBPlmnId member
                !resource.toString().contains("mnc") //EnodebFunction eNodeBPlmnId member
                !resource.toString().contains("mncLength") //EnodebFunction eNodeBPlmnId member

                !resource.toString().contains("neType") //MeContext and ManagedElement
                !resource.toString().contains("testObsoletePersistent") //MeContext
                !resource.toString().contains("MeContextId") //MeContext
                !resource.toString().contains("productName") //ManagedElement
                !resource.toString().contains("timePhaseMaxDeviationOtdoa") //EnodebFunction
                !resource.toString().contains("zzzTemporary43") //EutranCellFDD
            }
        }
    }

    def 'Execute nbiCrudGetRequest for MeContext with BASE_SUBTREE and attributes empty and scopeLevel=1 return success'() {
        given: 'nbiCrudGetRequest'
        attributes = ""
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_SUBTREE", fields, attributes, 1, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 200
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                resource.id == 'ERBS002'
                resource.attributes == null
                resource.toString().contains("ManagedElement")
                !resource.toString().contains("ENodeBFunction")
                !resource.toString().contains("EUtranCellFDD")

                //check some persistent attributes in some Mo
                !resource.toString().contains("MeContextId") //MeContext
                !resource.toString().contains("productName") //ManagedElement
                !resource.toString().contains("timePhaseMaxDeviationOtdoa") //EnodebFunction
                !resource.toString().contains("zzzTemporary43") //EutranCellFDD
            }
        }
    }

    def 'Execute nbiCrudGetRequest for MeContext with BASE_NTH_LEVEL and attributes=null and scopeLevel=0 return success'() {
        given: 'nbiCrudGetRequest'
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_NTH_LEVEL", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 200
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                id == 'ERBS002'
                !resource.toString().contains("ManagedElement")
                !resource.toString().contains("ENodeBFunction")
                !resource.toString().contains("EUtranCellFDD")

                //check some persistent attributes in some Mo
                resource.toString().contains("MeContextId") //MeContext
                !resource.toString().contains("productName") //ManagedElement
                !resource.toString().contains("timePhaseMaxDeviationOtdoa") //EnodebFunction
                !resource.toString().contains("zzzTemporary43") //EutranCellFDD
            }
        }
    }

    def 'Execute nbiCrudGetRequest for MeContext with BASE_NTH_LEVEL and attributes=null and scopeLevel=2 return success'() {
        given: 'nbiCrudGetRequest'
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_NTH_LEVEL", fields, attributes, 2, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 200
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                resource.id == 'ERBS002'
                resource.toString().contains("ManagedElement")
                resource.toString().contains("ENodeBFunction")
                !resource.toString().contains("EUtranCellFDD")

                //check some persistent attributes in some Mo
                !resource.toString().contains("MeContextId") //MeContext
                !resource.toString().contains("productName") //ManagedElement
                resource.toString().contains("timePhaseMaxDeviationOtdoa") //EnodebFunction
                !resource.toString().contains("zzzTemporary43") //EutranCellFDD
            }
        }
    }

    def 'Execute nbiCrudGetRequest for MeContext with BASE_NTH_LEVEL and 1 attributes specified and scopeLevel=0 return success'() {
        given: 'nbiCrudGetRequest'
        attributes = "NETYPE"
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_NTH_LEVEL", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            moObjects.moObjects.size() == 1
            httpCode == 200
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                resource.id == 'ERBS002'
                !resource.toString().contains("ManagedElement")
                !resource.toString().contains("ENodeBFunction")
                !resource.toString().contains("EUtranCellFDD")

                //check some persistent attributes in some Mo
                resource.toString().contains("neType") //MeContext and ManagedElement
                !resource.toString().contains("testObsoletePersistent") //MeContext
                !resource.toString().contains("MeContextId") //MeContext
                !resource.toString().contains("productName") //ManagedElement
                !resource.toString().contains("timePhaseMaxDeviationOtdoa") //EnodebFunction
                !resource.toString().contains("zzzTemporary43") //EutranCellFDD
            }
        }
    }

    def 'Execute nbiCrudGetRequest for MeContext with BASE_NTH_LEVEL and 2 attributes specified and scopeLevel=0 return success'() {
        given: 'nbiCrudGetRequest'
        attributes = "NETYPE,TESTobsoletePersistenT"
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_NTH_LEVEL", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            moObjects.moObjects.size() == 1
            httpCode == 200
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                resource.id == 'ERBS002'
                !resource.toString().contains("ManagedElement")
                !resource.toString().contains("ENodeBFunction")
                !resource.toString().contains("EUtranCellFDD")

                //check some persistent attributes in some Mo
                resource.toString().contains("neType") //MeContext and ManagedElement
                resource.toString().contains("testObsoletePersistent") //MeContext
                !resource.toString().contains("MeContextId") //MeContext
                !resource.toString().contains("productName") //ManagedElement
                !resource.toString().contains("timePhaseMaxDeviationOtdoa") //EnodebFunction
                !resource.toString().contains("zzzTemporary43") //EutranCellFDD
            }
        }
    }

    @Unroll
    def 'Execute nbiCrudGetRequest for MeContext with BASE_NTH_LEVEL and invalid attribute and scopeLevel=0 return fail'() {
        given: 'nbiCrudGetRequest'
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_NTH_LEVEL", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            moObjects == null
            errorResponseType.error.errorInfo.contains("Invalid attributes or fields for all entries")
            httpCode == 417
        }

        where:
        attributes               | fields                      | _
        'invalid'                | null                       | _
        'netype,invalid'        | null                       | _
        'mecontextid,invalid'  | null                       | _
        'mecontextid'           | 'attributes/NETYPE/AAA'   | _
        null                     | 'attributes/NETYPE/AAA'   | _
        null                     | 'attributes/ENODEBPlmnId/AAA'   | _
    }

    def 'Execute nbiCrudGetRequest for MeContext with BASE_NTH_LEVEL and complex attribute specified and scopeLevel=2 return success'() {
        given: 'nbiCrudGetRequest'
        attributes = "ENODEBPlmnId"
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_NTH_LEVEL", fields, attributes, 2, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            moObjects.moObjects.size() == 1
            httpCode == 200
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                resource.id == 'ERBS002'
                resource.toString().contains("ManagedElement")
                resource.toString().contains("ENodeBFunction")
                !resource.toString().contains("EUtranCellFDD")

                //check some persistent attributes in some Mo
                resource.toString().contains("eNodeBPlmnId") //EnodebFunction
                resource.toString().contains("mcc") //EnodebFunction eNodeBPlmnId member
                resource.toString().contains("mnc") //EnodebFunction eNodeBPlmnId member
                resource.toString().contains("mncLength") //EnodebFunction eNodeBPlmnId member

                !resource.toString().contains("neType") //MeContext and ManagedElement
                !resource.toString().contains("testObsoletePersistent") //MeContext
                !resource.toString().contains("MeContextId") //MeContext
                !resource.toString().contains("productName") //ManagedElement
                !resource.toString().contains("timePhaseMaxDeviationOtdoa") //EnodebFunction
                !resource.toString().contains("zzzTemporary43") //EutranCellFDD
            }
        }
    }

    def 'Execute nbiCrudGetRequest for MeContext with BASE_NTH_LEVEL and complex member attribute specified and scopeLevel=2 return success'() {
        given: 'nbiCrudGetRequest'
        fields = "attributes/ENODEBPlmnId/MCC"
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_NTH_LEVEL", fields, attributes, 2, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            moObjects.moObjects.size() == 1
            httpCode == 200
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                resource.id == 'ERBS002'
                resource.toString().contains("ManagedElement")
                resource.toString().contains("ENodeBFunction")
                !resource.toString().contains("EUtranCellFDD")

                //check some persistent attributes in some Mo
                resource.toString().contains("eNodeBPlmnId") //EnodebFunction
                resource.toString().contains("mcc") //EnodebFunction eNodeBPlmnId member
                !resource.toString().contains("mnc") //EnodebFunction eNodeBPlmnId member
                !resource.toString().contains("mncLength") //EnodebFunction eNodeBPlmnId member

                !resource.toString().contains("neType") //MeContext and ManagedElement
                !resource.toString().contains("testObsoletePersistent") //MeContext
                !resource.toString().contains("MeContextId") //MeContext
                !resource.toString().contains("productName") //ManagedElement
                !resource.toString().contains("timePhaseMaxDeviationOtdoa") //EnodebFunction
                !resource.toString().contains("zzzTemporary43") //EutranCellFDD
            }
        }
    }

    def 'Execute nbiCrudGetRequest for MeContext with BASE_NTH_LEVEL and attributes empty and scopeLevel=2 return success'() {
        given: 'nbiCrudGetRequest'
        attributes = ""
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_NTH_LEVEL", fields, attributes, 2, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 200
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                resource.id == 'ERBS002'
                resource.attributes == null
                resource.toString().contains("ManagedElement")
                resource.toString().contains("ENodeBFunction")
                !resource.toString().contains("EUtranCellFDD")

                //check some persistent attributes in some Mo
                !resource.toString().contains("MeContextId") //MeContext
                !resource.toString().contains("productName") //ManagedElement
                !resource.toString().contains("timePhaseMaxDeviationOtdoa") //EnodebFunction
                !resource.toString().contains("zzzTemporary43") //EutranCellFDD
            }
        }
    }

    @PropertiesForTest(properties = [
            @SuppliedProperty(name = "maxNumberOfObjectsForContainmentQueriesCmCommon", value = '10' )
    ])
    def 'Execute nbiCrudGetRequest for MeContext with BASE_ALL and attributes=null and maxNumberOfObjectsForContainmentQueriesCmCommon=10 return fail'() {
        given: 'nbiCrudGetRequest'
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_ALL", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            moObjects == null
            errorResponseType.error.errorInfo.contains("Execution Error Unable to perform this operation as the maximum number of supported objects (5) has been reached while fetching/processing EUtranCellFDD")
            httpCode == 417
        }
    }


    /*
            BASE_ALL WITH FILTER!=null
     */
    @Unroll
    def 'Execute nbiCrudGetRequest for ENodeBFunction with BASE_ALL,filter!=null and attributes=null return success'() {
        given: 'nbiCrudGetRequest'
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_ALL", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 200
            ((List)moObjects.moObjects.get("MeContext")).size() == 1
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                resource.id == 'ERBS002'
                resource.attributes == null
                resource.toString().contains("ManagedElement")
                resource.toString().contains("ENodeBFunction")
                !resource.toString().contains("EUtranCellFDD")

                //check some persistent attributes in some Mo
                !resource.toString().contains("MeContextId") //MeContext
                !resource.toString().contains("productName") //ManagedElement
                resource.toString().contains("userLabel") //EnodebFunction
                resource.toString().contains("ENodeBFunctionId") //EnodebFunction
                resource.toString().contains("eNodeBPlmnId") //EnodebFunction
                resource.toString().contains("timePhaseMaxDeviationOtdoa") //EnodebFunction
                !resource.toString().contains("zzzTemporary43") //EutranCellFDD
            }
        }

        where:
        filter                                                                                 | _
        '/MeContext/ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'      | _
        '//MeContext/ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]'     | _
        'MeContext//ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]'     | _
        '//ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'      | _
        '//ENodeBFunction[attributes/USERLABEL="value"]'      | _
        '/MeContext//ENodeBFunction[attributes/USERLABEL="value"]'      | _
        '/MECONTEXT//ENODEBFUNCTION[attributes/USERLABEL="value"]'      | _
    }

    @Unroll
    def 'Execute nbiCrudGetRequest for ENodeBFunction with BASE_ALL,filter!=null and attributes=null return fail cause filtered by fiilterCmResponseIfNecessary (regex pattern)'() {
        given: 'nbiCrudGetRequest'
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_ALL", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            httpCode == 200
        }

        where:
        filter                                                                                 | _
        '/MeContext/ENodeBFunction[attributes/USERLABEL="value"]'      | _
       '//MeContext/ENodeBFunction[attributes/USERLABEL="value"]'     | _

        '/ENodeBFunction[attributes/USERLABEL="value"]'      | _
        'Subnetwork/ENodeBFunction[attributes/USERLABEL="value"]'      | _
        '//Subnetwork//ENodeBFunction[attributes/USERLABEL="value"]'      | _

        '/MeContext//ENodeBFunction[attributes/USERLABEL="anothervalue"]'      | _

    }

    @Unroll
    def 'Execute nbiCrudGetRequest for ENodeBFunction with BASE_ALL,filter!=null and attributes empty return success'() {
        given: 'nbiCrudGetRequest'
        attributes = ""
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_ALL", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 200
            ((List)moObjects.moObjects.get("MeContext")).size() == 1
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                resource.id == 'ERBS002'
                resource.attributes == null
                resource.toString().contains("ManagedElement")
                resource.toString().contains("ENodeBFunction")
                !resource.toString().contains("EUtranCellFDD")

                //check some persistent attributes in some Mo
                !resource.toString().contains("MeContextId") //MeContext
                !resource.toString().contains("productName") //ManagedElement
                !resource.toString().contains("userLabel") //EnodebFunction
                !resource.toString().contains("ENodeBFunctionId") //EnodebFunction
                !resource.toString().contains("eNodeBPlmnId") //EnodebFunction
                !resource.toString().contains("timePhaseMaxDeviationOtdoa") //EnodebFunction
                !resource.toString().contains("zzzTemporary43") //EutranCellFDD
            }
        }

        where:
        filter                                                                       | _
        '/MeContext/ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'     | _
        '/MeContext/ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'     | _
        '//MeContext/ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]'   | _
        'MeContext//ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]'    | _
    }


    @Unroll
    def 'Execute nbiCrudGetRequest for ENodeBFunction with BASE_ALL, filter!=null with complex value and attributes empty return success'() {
        given: 'nbiCrudGetRequest'
        attributes = ""
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_ALL", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 200
            ((List)moObjects.moObjects.get("MeContext")).size() == 1
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                resource.id == 'ERBS002'
                resource.attributes == null
                resource.toString().contains("ManagedElement")
                resource.toString().contains("ENodeBFunction")
                !resource.toString().contains("EUtranCellFDD")

                //check some persistent attributes in some Mo
                !resource.toString().contains("MeContextId") //MeContext
                !resource.toString().contains("productName") //ManagedElement
                !resource.toString().contains("timePhaseMaxDeviationOtdoa") //EnodebFunction
                !resource.toString().contains("zzzTemporary43") //EutranCellFDD
            }
        }

        where:
        filter                                                                             | _
        '/MeContext/ManagedElement/ENodeBFunction[attributes/eNodeBPlmnId/mcc>=1]'         | _
        '/MeContext/ManagedElement/ENodeBFunction[attributes/eNodeBPlmnId/mcc!=1]'         | _
        '//ENodeBFunction[attributes/eNodeBPlmnId/mcc>=1]'                                 | _
        '/MeContext/ManagedElement//ENodeBFunction[attributes/eNodeBPlmnId/mcc=533]'       | _
        'MeContext/ManagedElement//ENodeBFunction[attributes/eNodeBPlmnId/mcc=533]'        | _
        '//MeContext/ManagedElement/ENodeBFunction[attributes/eNodeBPlmnId/mcc=533]'       | _
        '//MeContext/ManagedElement/ENodeBFunction[attributes/eNodeBPlmnId/mcc>532]'       | _
        '//MeContext/ManagedElement/ENodeBFunction[attributes/eNodeBPlmnId/mcc<534]'       | _
        '/MeContext/ManagedElement//ENodeBFunction[attributes/eNodeBPlmnId/mncLength>=2]'  | _
        '//ManagedElement//ENodeBFunction[attributes/eNodeBPlmnId/mncLength>=2]'           | _
        '/MeContext/ManagedElement//ENodeBFunction[attributes/eNodeBPlmnId/mncLength<=3]'  | _
        'MeContext/ManagedElement/ENodeBFunction[attributes/eNodeBPlmnId/mcc=533]'         | _
        '/MeContext/ManagedElement/ENodeBFunction[attributes/eNodeBPlmnId/mcc=533 and attributes/eNodeBPlmnId/mcc>=1]'                                          | _
        '/MeContext/ManagedElement/ENodeBFunction[attributes/eNodeBPlmnId/mcc=533 and attributes/eNodeBPlmnId/mncLength>=2 and ]'                               | _
        'MeContext/ManagedElement/ENodeBFunction[attributes/eNodeBPlmnId/mcc=533 and attributes/eNodeBPlmnId/mnc=57 and attributes/eNodeBPlmnId/mncLength>=2]'  | _
    }

    @Unroll
    def 'Execute nbiCrudGetRequest for ENodeBFunction with BASE_ALL, filter!=null with complex value and attributes null return success'() {
        given: 'nbiCrudGetRequest'

        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_ALL", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 200
            ((List)moObjects.moObjects.get("MeContext")).size() == 1
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                resource.id == 'ERBS002'
                resource.attributes == null
                resource.toString().contains("ManagedElement")
                resource.toString().contains("ENodeBFunction")
                !resource.toString().contains("EUtranCellFDD")
                resource.toString().contains("eNodeBPlmnId={mcc=533, mnc=57, mncLength=2}")

                //check some persistent attributes in some Mo
                !resource.toString().contains("MeContextId") //MeContext
                !resource.toString().contains("productName") //ManagedElement
                resource.toString().contains("timePhaseMaxDeviationOtdoa") //EnodebFunction
                !resource.toString().contains("zzzTemporary43") //EutranCellFDD
            }
        }

        where:
        filter                                                                             | _
        '/MeContext/ManagedElement/ENodeBFunction[attributes/eNodeBPlmnId/mcc>=1]'         | _
        '/MeContext/ManagedElement/ENodeBFunction[attributes/eNodeBPlmnId/mcc!=1]'         | _
        '//ENodeBFunction[attributes/eNodeBPlmnId/mcc>=1]'                                 | _
        '/MeContext/ManagedElement//ENodeBFunction[attributes/eNodeBPlmnId/mcc=533]'       | _
        'MeContext/ManagedElement//ENodeBFunction[attributes/eNodeBPlmnId/mcc=533]'        | _
        '//MeContext/ManagedElement/ENodeBFunction[attributes/eNodeBPlmnId/mcc=533]'       | _
        '//MeContext/ManagedElement/ENodeBFunction[attributes/eNodeBPlmnId/mcc>532]'       | _
        '//MeContext/ManagedElement/ENodeBFunction[attributes/eNodeBPlmnId/mcc<534]'       | _
        '/MeContext/ManagedElement//ENodeBFunction[attributes/eNodeBPlmnId/mncLength>=2]'  | _
        '//ManagedElement//ENodeBFunction[attributes/eNodeBPlmnId/mncLength>=2]'           | _
        '/MeContext/ManagedElement//ENodeBFunction[attributes/eNodeBPlmnId/mncLength<=3]'  | _
        'MeContext/ManagedElement/ENodeBFunction[attributes/eNodeBPlmnId/mcc=533]'         | _
        '/MeContext/ManagedElement/ENodeBFunction[attributes/eNodeBPlmnId/mcc=533 and attributes/eNodeBPlmnId/mcc>=1]'                                          | _
        '/MeContext/ManagedElement/ENodeBFunction[attributes/eNodeBPlmnId/mcc=533 and attributes/eNodeBPlmnId/mncLength>=2 and ]'                               | _
        'MeContext/ManagedElement/ENodeBFunction[attributes/eNodeBPlmnId/mcc=533 and attributes/eNodeBPlmnId/mnc=57 and attributes/eNodeBPlmnId/mncLength>=2]'  | _
    }

    @Unroll
    def 'Execute nbiCrudGetRequest for ENodeBFunction with BASE_ALL,filter!=null and attributes=userlabel return success'() {
        given: 'nbiCrudGetRequest'
        attributes = "userlabel"
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_ALL", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 200
            ((List)moObjects.moObjects.get("MeContext")).size() == 1
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                resource.id == 'ERBS002'
                resource.attributes == null
                resource.toString().contains("ManagedElement")
                resource.toString().contains("ENodeBFunction")
                !resource.toString().contains("EUtranCellFDD")

                //check some persistent attributes in some Mo
                !resource.toString().contains("MeContextId") //MeContext
                !resource.toString().contains("productName") //ManagedElement
                resource.toString().contains("userLabel") //EnodebFunction
                !resource.toString().contains("ENodeBFunctionId") //EnodebFunction
                !resource.toString().contains("eNodeBPlmnId") //EnodebFunction
                !resource.toString().contains("timePhaseMaxDeviationOtdoa") //EnodebFunction
                !resource.toString().contains("zzzTemporary43") //EutranCellFDD
            }
        }

        where:
        filter                                                                                 | _
        '/MeContext/ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'      | _
        '//MeContext/ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]'     | _
        'MeContext//ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]'     | _
        '//ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'      | _
        '//ENodeBFunction[attributes/USERLABEL="value"]'      | _
        '/MeContext//ENodeBFunction[attributes/USERLABEL="value"]'      | _
        '/MeContext//ENodeBFunction[attributes/enodebfunctionID="1"]'      | _
        '/MeContext//ENodeBFunction[attributes/enodebfunctionID!="2"]'      | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>1]'         | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc=533]'       | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>=533]'      | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>=533 and attributes/enODEBPlmnId/mcc<=533]'   | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>532 and attributes/enODEBPlmnId/mcc<534]'      | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>532 and attributes/enODEBPlmnId/mCC<534]'    | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>532 and attributes/ENODEBPlmnID/mcc<534]'      | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>532 and attributes/ENODEBPlmnID/mCC<534]'      | _
    }

    @Unroll
    def 'Execute nbiCrudGetRequest for ENodeBFunction with BASE_ALL,filter!=null and complex attributes=ENODEBPlmnId return success'() {
        given: 'nbiCrudGetRequest'
        attributes = "ENODEBPlmnId"
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_ALL", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 200
            ((List)moObjects.moObjects.get("MeContext")).size() == 1
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                resource.id == 'ERBS002'
                resource.attributes == null
                resource.toString().contains("ManagedElement")
                resource.toString().contains("ENodeBFunction")
                !resource.toString().contains("EUtranCellFDD")

                //check some persistent attributes in some Mo
                !resource.toString().contains("MeContextId") //MeContext
                !resource.toString().contains("productName") //ManagedElement
                !resource.toString().contains("userLabel") //EnodebFunction
                !resource.toString().contains("ENodeBFunctionId") //EnodebFunction
                resource.toString().contains("eNodeBPlmnId") //EnodebFunction
                resource.toString().contains("mcc") //EnodebFunction eNodeBPlmnId member
                resource.toString().contains("mnc") //EnodebFunction eNodeBPlmnId member
                resource.toString().contains("mncLength") //EnodebFunction eNodeBPlmnId member
                !resource.toString().contains("timePhaseMaxDeviationOtdoa") //EnodebFunction
                !resource.toString().contains("zzzTemporary43") //EutranCellFDD
            }
        }

        where:
        filter                                                                                 | _
        '/MeContext/ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'      | _
        '//MeContext/ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]'     | _
        'MeContext//ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]'     | _
        '//ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'      | _
        '//ENodeBFunction[attributes/USERLABEL="value"]'      | _
        '/MeContext//ENodeBFunction[attributes/USERLABEL="value"]'      | _
        '/MeContext//ENodeBFunction[attributes/enodebfunctionID="1"]'      | _
        '/MeContext//ENodeBFunction[attributes/enodebfunctionID!="2"]'      | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>1]'         | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc=533]'       | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>=533]'      | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>=533 and attributes/enODEBPlmnId/mcc<=533]'   | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>532 and attributes/enODEBPlmnId/mcc<534]'      | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>532 and attributes/enODEBPlmnId/mCC<534]'    | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>532 and attributes/ENODEBPlmnID/mcc<534]'      | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>532 and attributes/ENODEBPlmnID/mCC<534]'      | _
    }

    @Unroll
    def 'Execute nbiCrudGetRequest for ENodeBFunction with BASE_ALL,filter!=null and complex member fields=attributes/ENODEBPlmnId/MCC return success'() {
        given: 'nbiCrudGetRequest'
        fields = "attributes/ENODEBPlmnId/MCC"
        attributes = "timePhaseMaxDeviationOtdoa"
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_ALL", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 200
            ((List)moObjects.moObjects.get("MeContext")).size() == 1
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                resource.id == 'ERBS002'
                resource.attributes == null
                resource.toString().contains("ManagedElement")
                resource.toString().contains("ENodeBFunction")
                !resource.toString().contains("EUtranCellFDD")

                //check some persistent attributes in some Mo
                !resource.toString().contains("MeContextId") //MeContext
                !resource.toString().contains("productName") //ManagedElement
                !resource.toString().contains("userLabel") //EnodebFunction
                !resource.toString().contains("ENodeBFunctionId") //EnodebFunction
                resource.toString().contains("eNodeBPlmnId") //EnodebFunction
                resource.toString().contains("mcc") //EnodebFunction eNodeBPlmnId member
                !resource.toString().contains("mnc") //EnodebFunction eNodeBPlmnId member
                !resource.toString().contains("mncLength") //EnodebFunction eNodeBPlmnId member
                resource.toString().contains("timePhaseMaxDeviationOtdoa") //EnodebFunction
                !resource.toString().contains("zzzTemporary43") //EutranCellFDD
            }
        }

        where:
        filter                                                                                 | _
        '/MeContext/ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'      | _
        '//MeContext/ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]'     | _
        'MeContext//ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]'     | _
        '//ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'      | _
        '//ENodeBFunction[attributes/USERLABEL="value"]'      | _
        '/MeContext//ENodeBFunction[attributes/USERLABEL="value"]'      | _
        '/MeContext//ENodeBFunction[attributes/enodebfunctionID="1"]'      | _
        '/MeContext//ENodeBFunction[attributes/enodebfunctionID!="2"]'      | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>1]'         | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc=533]'       | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>=533]'      | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>=533 and attributes/enODEBPlmnId/mcc<=533]'   | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>532 and attributes/enODEBPlmnId/mcc<534]'      | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>532 and attributes/enODEBPlmnId/mCC<534]'    | _
         '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>532 and attributes/ENODEBPlmnID/mcc<534]'      | _
         '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>532 and attributes/ENODEBPlmnID/mCC<534]'      | _
    }

    def 'Execute nbiCrudGetRequest for ENodeBFunction with BASE_ALL,filter!=null with invalid Mo return fail'() {
        given: 'nbiCrudGetRequest'
        String filter = "/InvalidMo/ManagedElement/ENodeBFunction";
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_ALL", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            moObjects == null
            errorResponseType.error.errorInfo.contains("Invalid MO type")
            httpCode == 417
        }
    }

    def 'Execute nbiCrudGetRequest for ENodeBFunction with BASE_ALL,filter!=null and invalid filter return fail'() {
        given: 'nbiCrudGetRequest'
        String filter = "/MeContext/[attributes/NETYPE=\"ERBS\"]";
        attributes = ""
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_ALL", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            moObjects == null
            errorResponseType.error.errorInfo.contains("Invalid filter=/MeContext/ for fdn=MeContext=ERBS002")
            httpCode == 422
        }
    }

    @Unroll
    def 'Execute nbiCrudGetRequest for ENodeBFunction with BASE_ALL,filter!=null and invalid filter (too many slashes) return fail'() {
        given: 'nbiCrudGetRequest'
        attributes = ""
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_ALL", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            moObjects == null
            errorResponseType.error.errorInfo.contains("It should contain only / or //")
            httpCode == 422
        }

        where:
        filter                                                                                 | _
        '///MeContext/ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'     | _
        '/MeContext/ManagedElement////ENodeBFunction[attributes/USERLABEL="value"]'     | _
        '//MeContext/ManagedElement///ENodeBFunction[attributes/USERLABEL="value"]'     | _
        'MeContext///ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]'     | _
    }

    @Unroll
    def 'Execute nbiCrudGetRequest for ENodeBFunction with BASE_ALL,filter!=null and invalid filter (it contains |) return fail'() {
        given: 'nbiCrudGetRequest'
        attributes = ""
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_ALL", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            moObjects == null
            errorResponseType.error.errorInfo.contains("It should not contain |")
            httpCode == 422
        }

        where:
        filter                                                                                 | _
        '//MeContext/ManagedElement/ENodeBFunction[attributes/USERLABEL="value"] | //MeContext'     | _
        '/MeContext/ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]|//MeContext'     | _
        '//MeContext/ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]|//MeContext'     | _
        'MeContext//ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]|//MeContext'     | _
    }


    @Unroll
    def 'Execute nbiCrudGetRequest for ENodeBFunction with BASE_ALL,filter!=null and invalid attributes return fail'() {
        given: 'nbiCrudGetRequest'
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_ALL", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            moObjects == null
            errorResponseType.error.errorInfo.contains("An unknown attribute has been encountered; name invalidAttribute")
            httpCode == 417
        }

        where:
        filter                                                                                 | attributes | _
        '/MeContext/ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'      | 'invalidAttribute' | _
        '//MeContext/ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]'    |  'USERLABEL,invalidAttribute' | _
        'MeContext//ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]'     |  'invalidAttribute,USERLABEL,USERLABEL' | _
        '//ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'      |  'invalidAttribute' | _
        '//ENodeBFunction[attributes/USERLABEL="value"]'      |  'invalidAttribute' | _
        '/MeContext//ENodeBFunction[attributes/USERLABEL="value"]'      |  'invalidAttribute' | _
        '/MeContext//ENodeBFunction[attributes/enodebfunctionID="1"]'      |  'invalidAttribute' | _
        '/MeContext//ENodeBFunction[attributes/enodebfunctionID!="2"]'      |  'invalidAttribute' | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>1]'         |  'invalidAttribute' | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc=533]'       |  'invalidAttribute' | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>=533]'      |  'invalidAttribute' | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>=533 and attributes/enODEBPlmnId/mcc<=533]'   |  'invalidAttribute' | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>532 and attributes/enODEBPlmnId/mcc<534]'      |  'invalidAttribute' | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>532 and attributes/enODEBPlmnId/mCC<534]'    |  'invalidAttribute' | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>532 and attributes/ENODEBPlmnID/mcc<534]'      |  'invalidAttribute' | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>532 and attributes/ENODEBPlmnID/mCC<534]'      |  'invalidAttribute' | _
    }

    @Unroll
    def 'Execute nbiCrudGetRequest for ENodeBFunction with BASE_ALL,filter!=null and invalid fields return fail'() {
        given: 'nbiCrudGetRequest'
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_ALL", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.FAIL
            errorResponseType != null
            moObjects == null
            errorResponseType.error.errorInfo.contains("An unknown attribute has been encountered; name invalidAttribute") || errorResponseType.error.errorInfo.contains("An unknown struct member has been encountered; name invalidMember") || errorResponseType.error.errorInfo.contains("An incorrect attribute data type has been encountered")
            httpCode == 417
        }

        where:
        filter                                                                                 | fields | _
        '/MeContext/ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'      | 'attributes/invalidAttribute' | _
        '//MeContext/ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]'    |  'attributes/invalidAttribute/mcc' | _
        'MeContext//ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]'     |  'attributes/invalidAttribute,attributes/userlabel' | _
        '//ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'      |  'attributes/ENODEBPlmnId/invalidMember' | _
        '//ENodeBFunction[attributes/USERLABEL="value"]'      |  'attributes/userlabel/mcc' | _
        '/MeContext//ENodeBFunction[attributes/USERLABEL="value"]'      |  'attributes/invalidAttribute' | _
        '/MeContext//ENodeBFunction[attributes/enodebfunctionID="1"]'      |  'attributes/invalidAttribute' | _
        '/MeContext//ENodeBFunction[attributes/enodebfunctionID!="2"]'      |  'attributes/invalidAttribute' | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>1]'         |  'attributes/ENODEBPlmnId/invalidMember' | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc=533]'       |  'attributes/invalidAttribute' | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>=533]'      |  'attributes/invalidAttribute' | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>=533 and attributes/enODEBPlmnId/mcc<=533]'   |  'attributes/invalidAttribute' | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>532 and attributes/enODEBPlmnId/mcc<534]'      |  'attributes/invalidAttribute' | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>532 and attributes/enODEBPlmnId/mCC<534]'    |  'attributes/invalidAttribute' | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>532 and attributes/ENODEBPlmnID/mcc<534]'      |  'attributes/invalidAttribute' | _
        '/MeContext//ENodeBFunction[attributes/enODEBPlmnId/mcc>532 and attributes/ENODEBPlmnID/mCC<534]'      |  'attributes/ENODEBPlmnId/invalidMember' | _
    }

    @Unroll
    def 'Execute nbiCrudGetRequest for ENodeBFunction with ROOT_MO,BASE_ALL,filter!=null and attributes=null return success'() {
        given: 'nbiCrudGetRequest'
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, ROOT_MO , "BASE_ALL", fields, attributes, 0, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 200
            ((List)moObjects.moObjects.get("MeContext")).size() == 2
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                resource.id == 'ERBS001'
                resource.attributes == null
                resource.toString().contains("ManagedElement")
                resource.toString().contains("ENodeBFunction")
                !resource.toString().contains("EUtranCellFDD")

                //check some persistent attributes in some Mo
                !resource.toString().contains("MeContextId") //MeContext
                !resource.toString().contains("productName") //ManagedElement
                resource.toString().contains("timePhaseMaxDeviationOtdoa") //EnodebFunction
                !resource.toString().contains("zzzTemporary43") //EutranCellFDD
            }
        }

        where:
        filter                                                                                 | _
        '/MeContext/ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'     | _
    }

    /*
        BASE_SUBTREE WITH FILTER!=null
   */
    @Unroll
    def 'Execute nbiCrudGetRequest for ENodeBFunction with BASE_SUBTREE,filter!=null and attributes=null and scopeLevel>=2 return success'() {
        given: 'nbiCrudGetRequest'
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_SUBTREE", fields, attributes, scopeLevel, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 200
            ((List)moObjects.moObjects.get("MeContext")).size() == 1
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                resource.id == 'ERBS002'
                resource.attributes == null
                resource.toString().contains("ManagedElement")
                resource.toString().contains("ENodeBFunction")
                !resource.toString().contains("EUtranCellFDD")

                //check some persistent attributes in some Mo
                !resource.toString().contains("MeContextId") //MeContext
                !resource.toString().contains("productName") //ManagedElement
                resource.toString().contains("timePhaseMaxDeviationOtdoa") //EnodebFunction
                !resource.toString().contains("zzzTemporary43") //EutranCellFDD
            }
        }

        where:
        filter                                                                                 | scopeLevel | _
        '/MeContext/ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'      | 2          | _
        '//MeContext/ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]'    | 3          | _
        'MeContext//ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]'     |  4         | _
        '//ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'                 | 5         |_
        '//ENodeBFunction[attributes/USERLABEL="value"]'                                  | 10         | _
        '/MeContext//ENodeBFunction[attributes/USERLABEL="value"]'                       | 100       | _
    }

    @Unroll
    def 'Execute nbiCrudGetRequest for ENodeBFunction with BASE_SUBTREE,filter!=null and attributes=null and scopeLevel<2 return fail cause filtered by fiilterCmResponseIfNecessary (scopeLevel filter)'() {
        given: 'nbiCrudGetRequest'
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_SUBTREE", fields, attributes, scopeLevel, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects == null
            httpCode == 200
        }

        where:
        filter                                                                                 | scopeLevel | _
        '/MeContext/ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'      | 0          | _
        '//MeContext/ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]'    | 1          | _
        'MeContext//ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]'     |  1         | _
    }

    @Unroll
    def 'Execute nbiCrudGetRequest for ENodeBFunction with ROOT_MO,BASE_SUBTREE,filter!=null and attributes=null and scopeLevel>=3 return success'() {
        given: 'nbiCrudGetRequest'
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, ROOT_MO , "BASE_SUBTREE", fields, attributes, scopeLevel, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 200
            ((List)moObjects.moObjects.get("MeContext")).size() == 2
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                resource.id == 'ERBS001'
                resource.attributes == null
                resource.toString().contains("ManagedElement")
                resource.toString().contains("ENodeBFunction")
                !resource.toString().contains("EUtranCellFDD")

                //check some persistent attributes in some Mo
                !resource.toString().contains("MeContextId") //MeContext
                !resource.toString().contains("productName") //ManagedElement
                resource.toString().contains("timePhaseMaxDeviationOtdoa") //EnodebFunction
                !resource.toString().contains("zzzTemporary43") //EutranCellFDD
            }
        }

        where:
        filter                                                                                 | scopeLevel | _
        '/MeContext/ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'      | 3          | _
        '//MeContext/ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]'    | 3          | _
        'MeContext//ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]'     |  4         | _
        '//ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'                 | 5         |_
        '//ENodeBFunction[attributes/USERLABEL="value"]'                                  | 10         | _
        '/MeContext//ENodeBFunction[attributes/USERLABEL="value"]'                       | 100       | _
    }

    /*
    BASE_NTH_LEVEL WITH FILTER!=null
    */
    @Unroll
    def 'Execute nbiCrudGetRequest for ENodeBFunction with BASE_NTH_LEVEL,filter!=null and attributes=null and scopeLevel=2 return success'() {
        given: 'nbiCrudGetRequest'
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_NTH_LEVEL", fields, attributes, scopeLevel, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 200
            ((List)moObjects.moObjects.get("MeContext")).size() == 1
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                resource.id == 'ERBS002'
                resource.attributes == null
                resource.toString().contains("ManagedElement")
                resource.toString().contains("ENodeBFunction")
                !resource.toString().contains("EUtranCellFDD")

                //check some persistent attributes in some Mo
                !resource.toString().contains("MeContextId") //MeContext
                !resource.toString().contains("productName") //ManagedElement
                resource.toString().contains("timePhaseMaxDeviationOtdoa") //EnodebFunction
                !resource.toString().contains("zzzTemporary43") //EutranCellFDD
            }
        }

        where:
        filter                                                                                 | scopeLevel | _
        '/MeContext/ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'      | 2          | _
        '//MeContext/ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]'    | 2          | _
        'MeContext//ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]'     | 2         | _
    }

    @Unroll
    def 'Execute nbiCrudGetRequest for ENodeBFunction with BASE_NTH_LEVEL,filter!=null and attributes=null and scopeLevel!=2 return fail cause filtered by fiilterCmResponseIfNecessary (scopeLevel filter)'() {
        given: 'nbiCrudGetRequest'
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "MeContext=ERBS002" , "BASE_NTH_LEVEL", fields, attributes, scopeLevel, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            moObjects.moObjects == null
            errorResponseType == null
            httpCode == 200
        }

        where:
        filter                                                                                 | scopeLevel | _
        '/MeContext/ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'      | 0          | _
        '//MeContext/ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]'    | 1          | _
        'MeContext//ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]'     |  1         | _
        '/MeContext/ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'      | 3          | _
        '//MeContext/ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]'    | 3          | _
        'MeContext//ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]'     |  4         | _
        '//ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'                 | 5         |_
        '//ENodeBFunction[attributes/USERLABEL="value"]'                                  | 10         | _
        '/MeContext//ENodeBFunction[attributes/USERLABEL="value"]'                       | 100       | _
    }

    @Unroll
    def 'Execute nbiCrudGetRequest for ENodeBFunction with ROOT_MO,BASE_NTH_LEVEL,filter!=null and attributes=null and scopeLevel=3 return success'() {
        given: 'nbiCrudGetRequest'
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, ROOT_MO , "BASE_NTH_LEVEL", fields, attributes, scopeLevel, filter)

        when:
        CmGetResponse cmGetResponse = objUnderTest.executor(nbiCrudGetRequest)
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            moObjects.moObjects.size() == 1
            httpCode == 200
            ((List)moObjects.moObjects.get("MeContext")).size() == 2
            def resource = ((List)moObjects.moObjects.get("MeContext")).iterator().next()
            with (resource) {
                resource.id == 'ERBS001'
                resource.attributes == null
                resource.toString().contains("ManagedElement")
                resource.toString().contains("ENodeBFunction")
                !resource.toString().contains("EUtranCellFDD")

                //check some persistent attributes in some Mo
                !resource.toString().contains("MeContextId") //MeContext
                !resource.toString().contains("productName") //ManagedElement
                resource.toString().contains("timePhaseMaxDeviationOtdoa") //EnodebFunction
                !resource.toString().contains("zzzTemporary43") //EutranCellFDD
            }
        }

        where:
        filter                                                                                 | scopeLevel | _
        '/MeContext/ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'      | 3          | _
        '//MeContext/ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]'    | 3          | _
        'MeContext//ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]'     |  3         | _
        '//ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'                 | 3         |_
    }



    /*
        TEST HTTP RETURN CODES
   */
    def 'Execute nbiCrudGetRequest with objUnderTest.executeOperation return success'() {
        given: 'nbiCrudGetRequest'
        attributes = "NETYPE"
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , "BASE_ONLY", fields, attributes, 0, filter)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudGetRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 200
            // here example of one instance at first level ([] not used cause of WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
            nbiResponse.jsonContent == '{"NetworkElement":{"id":"ERBS002","attributes":{"neType":"ERBS"}}}'
        }
    }

    def 'Execute nbiCrudGetRequest with objUnderTest.executeOperation with ROOT_MO,BASE_ALL,filter!=null and attributes return success'() {
        given: 'nbiCrudGetRequest'
        attributes = ""
        String filter = "/NetworkElement";
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, ROOT_MO , "BASE_ALL", fields, attributes, 0, filter)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudGetRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 200
            // here example of multiple instances at first level ([] used)
            nbiResponse.jsonContent == '{"NetworkElement":[{"id":"TESTNODE"},{"id":"BSC011"},{"id":"ERBS001"},{"id":"ERBS002"},{"id":"ANOTHERNODE"}]}'
        }
    }

    def 'Execute nbiCrudGetRequest with objUnderTest.executeOperation with testDate (Date() field) return success'() {
        given: 'nbiCrudGetRequest'
        attributes = "testDate"
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , "BASE_ONLY", fields, attributes, 0, filter)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudGetRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 200
            nbiResponse.jsonContent.contains('Sep ') //minimal check cause on PCR env different timezone
        }
    }

    def 'Execute nbiCrudGetRequest with objUnderTest.executeOperation return fail'() {
        given: 'nbiCrudGetRequest'
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=xxx" , "BASE_ONLY", fields, attributes, 0, filter)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudGetRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 404
            nbiResponse.jsonContent == '{"error":{"errorInfo":"The supplied FDN NetworkElement=xxx does not exist in the database"}}'
        }
    }

    def 'Execute nbiCrudGetRequest with objUnderTest.executeOperation with invalid scopeType return fail'() {
        given: 'nbiCrudGetRequest'
        String scopeType = "INVALID"
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , scopeType, fields, attributes, 0, filter)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudGetRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 400
            nbiResponse.jsonContent == '{"error":{"errorInfo":"The supplied parameters scopeType=INVALID is not allowed."}}'
        }
    }

    /*
        TEST SINGLE METHODS
    */

    @Unroll
    def 'Verify formatSingleMo when no cmObjects'() {
        when: 'formatSingleMo is invoked'
        CmGetResponse cmGetResponse = objUnderTest.formatSingleMo(new CmResponse())
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            httpCode == 200
        }
    }

    @Unroll
    def 'Verify formatAllMos when no cmObjects'() {
        when: 'formatAllMos is invoked'
        CmGetResponse cmGetResponse = objUnderTest.formatAllMos(new CmResponse(), "")
        then:
        with(cmGetResponse) {
            responseType == ResponseType.SUCCESS
            errorResponseType == null
            httpCode == 200
        }
    }

    def 'CmGetRequest and CmGetResponse for_coverage_only'() {
        when:
        CmGetRequest cmGetRequest = new CmGetRequest("fdn", null , 0, null, null)
        cmGetRequest.hasEmptyAttributes()

        def attributesMap = new HashMap<String, Set<String>>()
        attributesMap.put("attribute", new HashSet<String>())
        CmGetRequest cmGetRequest1 = new CmGetRequest("fdn", null , 0, attributesMap, null)

        CmGetResponse cmGetResponse = new CmGetResponse(null, null , null, 0)

        then:
        cmGetRequest.hasEmptyAttributes() == false
        cmGetRequest1.hasEmptyAttributes() == false
        cmGetRequest.toString()
        cmGetResponse.toString()
    }

    @Unroll
    def 'Execute nbiCrudGetRequest with objUnderTest.executeOperation with xpath #xpath, scopeType NOT BASE_ONLY and filter null return fail'(xpath,scopeType) {
        given: 'nbiCrudGetRequest'
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, xpath , scopeType, fields, attributes, 0, null)

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudGetRequest)
        then:
        with(nbiResponse) {
            nbiResponse.httpCode == 422
            nbiResponse.jsonContent == '{"error":{"errorInfo":"Request not supported"}}'
        }

        where: ""
        xpath                    |  scopeType         |_
        "subnetwork=test"       | "BASE_ALL"       |_
        "SUBNETWORK=test"       | "BASE_SUBTREE"   |_
        "SUBnetwork=test"       | "BASE_NTH_LEVEL" |_
    }

    @Unroll
    def 'Execute nbiCrudGetRequest with objUnderTest.executeOperation with xpath #xpath, scopeType NOT BASE_ONLY and filter!=null and BlackListed MO (es ENodeBFunction_BlackListed) return fail'(xpath, scopeType,filter, attributes, httpCode) {
        given: 'nbiCrudGetRequest'
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, xpath , scopeType, fields, attributes, 0, filter)

        and:
        fillNotEmptyDynamicBlackListForNbi() //to check use case when ask root with filter!=null and blacklisted mo

        when:
        NbiResponse nbiResponse = objUnderTest.executeOperation(nbiCrudGetRequest)
        then:
        System.out.println("nbiResponse="+nbiResponse);
        with(nbiResponse) {
            nbiResponse.httpCode == httpCode
            if (httpCode != 200) {
                nbiResponse.jsonContent.contains('Overload Protection Mechanism: Unsupported MO Class in command.  A Network Wide query for MO Class')
                nbiResponse.jsonContent.contains('<namespace>')  //'Overload Protection Mechanism: Unsupported MO Class in command.  A Network Wide query for MO Class (<namespace>/ENodeBFunction) is performed with the executed query, but is not supported, as doing so will return an excessive amount of data."}}'
            }
        }

        where: ""
        xpath                     |  scopeType     | filter                                                                               | attributes | httpCode | _
        // attributes=null
        ROOT_MO                    | "BASE_ALL"    | '/MeContext/ManagedElement/ENodeBFunction'                                    | null | 417 | _
        ROOT_MO                    | "BASE_ALL"    | '/MeContext/ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'   | null |  417 | _
        ROOT_MO                    | "BASE_ALL"    | 'MeContext/ManagedElement/ENODEBFUNCTION[attributes/USERLABEL="value"]'   | null |  417 | _
        ROOT_MO                    | "BASE_ALL"    | '//ENODEBFUNCTION[attributes/USERLABEL="value"]'   | null |  417 | _
        ROOT_MO                    | "BASE_SUBTREE"    | '/MeContext/ManagedElement/ENodeBFunction'                                    | null | 417 | _
        ROOT_MO                     | "BASE_NTH_LEVEL" | '/MeContext/ManagedElement/ENodeBFunction'                                    | null | 417 | _

        'SubNetwork=Sample'       | "BASE_ALL"    | '/MeContext/ManagedElement/ENodeBFunction'   | null |  417 | _
        'SubNetwork=Sample'       | "BASE_ALL"    | '/MeContext/ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'   | null |  417 | _
        'SubNetwork=Sample'       | "BASE_ALL"    | 'MeContext/ManagedElement/ENODEBFUNCTION[attributes/USERLABEL="value"]'   | null |  417 | _
        'SubNetwork=Sample'       | "BASE_ALL"    | '//ENODEBFUNCTION[attributes/USERLABEL="value"]'   | null |  417 | _
        'SubNetwork=Sample'       | "BASE_SUBTREE"      | '/MeContext/ManagedElement/ENodeBFunction'   | null |  417 | _
        'SubNetwork=Sample'       | "BASE_NTH_LEVEL"    | '/MeContext/ManagedElement/ENodeBFunction'   | null |  417 | _

        ROOT_MO                    | "BASE_ALL"    | '/MeContext/ManagedElement/ENodeBFunction_BlackListed'   | null |  417 | _
        ROOT_MO                    | "BASE_ALL"    | '/MeContext/ManagedElement/ENodeBFunction_BlackListed[attributes/USERLABEL="value"]'   | null |  417 | _
        ROOT_MO                    | "BASE_ALL"    | 'MeContext/ManagedElement/ENodeBFunction_BlackListed[attributes/USERLABEL="value"]'   | null |  417 | _
        ROOT_MO                    | "BASE_ALL"    | '//ENodeBFunction_BlackListed[attributes/USERLABEL="value"]'   | null |  417 | _
        ROOT_MO                    | "BASE_SUBTREE"    | '/MeContext/ManagedElement/ENodeBFunction_BlackListed'   | null |  417 | _
        ROOT_MO                    | "BASE_NTH_LEVEL"    | '/MeContext/ManagedElement/ENodeBFunction_BlackListed'   | null |  417 | _

        'SubNetwork=Sample'       | "BASE_ALL"    | '/MeContext/ManagedElement/ENodeBFunction_BlackListed'   | null |   417 | _
        'SubNetwork=Sample'       | "BASE_ALL"    | '/MeContext/ManagedElement/ENodeBFunction_BlackListed[attributes/USERLABEL="value"]'   | null |  417 | _
        'SubNetwork=Sample'       | "BASE_ALL"    | 'MeContext/ManagedElement/ENodeBFunction_BlackListed[attributes/USERLABEL="value"]'   | null |  417 | _
        'SubNetwork=Sample'       | "BASE_ALL"    | '//ENodeBFunction_BlackListed[attributes/USERLABEL="value"]'   | null |  417 | _
        'SubNetwork=Sample'       | "BASE_SUBTREE"    | '/MeContext/ManagedElement/ENodeBFunction_BlackListed'   | null |   417 | _
        'SubNetwork=Sample'       | "BASE_NTH_LEVEL"    | '/MeContext/ManagedElement/ENodeBFunction_BlackListed'   | null |   417 | _

        // empty attributes
        ROOT_MO                    | "BASE_ALL"    | '/MeContext/ManagedElement/ENodeBFunction'                                    | '' |  417 | _
        ROOT_MO                    | "BASE_ALL"    | '/MeContext/ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'   | '' |  417 | _
        ROOT_MO                    | "BASE_ALL"    | 'MeContext/ManagedElement/ENODEBFUNCTION[attributes/USERLABEL="value"]'   | '' |  417 | _
        ROOT_MO                    | "BASE_ALL"    | '//ENODEBFUNCTION[attributes/USERLABEL="value"]'   | '' |  417 | _
        ROOT_MO                    | "BASE_SUBTREE"    | '/MeContext/ManagedElement/ENodeBFunction'                                    | '' |  417 | _
        ROOT_MO                    | "BASE_NTH_LEVEL"    | '/MeContext/ManagedElement/ENodeBFunction'                                    | '' |  417 | _

        'SubNetwork=Sample'       | "BASE_ALL"    | '/MeContext/ManagedElement/ENodeBFunction'   | '' |  417 | _
        'SubNetwork=Sample'       | "BASE_ALL"    | '/MeContext/ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'   | '' |  417 | _
        'SubNetwork=Sample'       | "BASE_ALL"    | 'MeContext/ManagedElement/ENODEBFUNCTION[attributes/USERLABEL="value"]'   | '' |  417 | _
        'SubNetwork=Sample'       | "BASE_ALL"    | '//ENODEBFUNCTION[attributes/USERLABEL="value"]'   | '' |  417 | _
        'SubNetwork=Sample'       | "BASE_SUBTREE"    | '/MeContext/ManagedElement/ENodeBFunction'   | '' |  417 | _
        'SubNetwork=Sample'       | "BASE_NTH_LEVEL"    | '/MeContext/ManagedElement/ENodeBFunction'   | '' |  417 | _

        ROOT_MO                    | "BASE_ALL"    | '/MeContext/ManagedElement/ENodeBFunction_BlackListed'   | '' |  200 | _
        ROOT_MO                    | "BASE_ALL"    | '/MeContext/ManagedElement/ENodeBFunction_BlackListed[attributes/USERLABEL="value"]'   | '' |  200 | _
        ROOT_MO                    | "BASE_ALL"    | 'MeContext/ManagedElement/ENodeBFunction_BlackListed[attributes/USERLABEL="value"]'   | '' |  200 | _
        ROOT_MO                    | "BASE_ALL"    | '//ENodeBFunction_BlackListed[attributes/USERLABEL="value"]'   | '' |  200 | _
        ROOT_MO                    | "BASE_SUBTREE"    | '/MeContext/ManagedElement/ENodeBFunction_BlackListed'   | '' |  200 | _
        ROOT_MO                    | "BASE_NTH_LEVEL"    | '/MeContext/ManagedElement/ENodeBFunction_BlackListed'   | '' |  200 | _

        'SubNetwork=Sample'       | "BASE_ALL"    | '/MeContext/ManagedElement/ENodeBFunction_BlackListed'   | '' |  200 | _
        'SubNetwork=Sample'       | "BASE_ALL"    | '/MeContext/ManagedElement/ENodeBFunction_BlackListed[attributes/USERLABEL="value"]'   | '' |  200 | _
        'SubNetwork=Sample'       | "BASE_ALL"    | 'MeContext/ManagedElement/ENodeBFunction_BlackListed[attributes/USERLABEL="value"]'   | '' |  200 | _
        'SubNetwork=Sample'       | "BASE_ALL"    | '//ENodeBFunction_BlackListed[attributes/USERLABEL="value"]'   | '' |  200 | _
        'SubNetwork=Sample'       | "BASE_SUBTREE"    | '/MeContext/ManagedElement/ENodeBFunction_BlackListed'   | '' |  200 | _
        'SubNetwork=Sample'       | "BASE_NTH_LEVEL"    | '/MeContext/ManagedElement/ENodeBFunction_BlackListed'   | '' |  200 | _

        // attributes > 5
        ROOT_MO                    | "BASE_ALL"    | '/MeContext/ManagedElement/ENodeBFunction_BlackListed'                          | 'userLabel,dnsLookupOnTai,dnsLookupTimer,dscpLabel,eNodeBPlmnId,nnsfMode' |  417 | _
        ROOT_MO                    | "BASE_ALL"    | '//ENodeBFunction_BlackListed'                                                     | 'userLabel,dnsLookupOnTai,dnsLookupTimer,dscpLabel,eNodeBPlmnId,nnsfMode' |  417 | _
        ROOT_MO                    | "BASE_SUBTREE"    | '//ENodeBFunction_BlackListed'                                                     | 'userLabel,dnsLookupOnTai,dnsLookupTimer,dscpLabel,eNodeBPlmnId,nnsfMode' |  417 | _
        ROOT_MO                    | "BASE_NTH_LEVEL"    | '//ENodeBFunction_BlackListed'                                                     | 'userLabel,dnsLookupOnTai,dnsLookupTimer,dscpLabel,eNodeBPlmnId,nnsfMode' |  417 | _

        'SubNetwork=Sample'       | "BASE_ALL"    | '/MeContext/ManagedElement/ENodeBFunction_BlackListed'                         | 'userLabel,dnsLookupOnTai,dnsLookupTimer,dscpLabel,eNodeBPlmnId,nnsfMode' |  417 | _
        'SubNetwork=Sample'       | "BASE_ALL"    | '//ENodeBFunction_BlackListed'                                                   | 'userLabel,dnsLookupOnTai,dnsLookupTimer,dscpLabel,eNodeBPlmnId,nnsfMode' |  417 | _
        'SubNetwork=Sample'       | "BASE_SUBTREE"    | '//ENodeBFunction_BlackListed'                                                   | 'userLabel,dnsLookupOnTai,dnsLookupTimer,dscpLabel,eNodeBPlmnId,nnsfMode' |  417 | _
        'SubNetwork=Sample'       | "BASE_NTH_LEVEL"    | '//ENodeBFunction_BlackListed'                                                   | 'userLabel,dnsLookupOnTai,dnsLookupTimer,dscpLabel,eNodeBPlmnId,nnsfMode' |  417 | _

        // attributes <=5
        ROOT_MO                    | "BASE_ALL"    | '/MeContext/ManagedElement/ENodeBFunction_BlackListed'        | 'userLabel,dnsLookupOnTai,dnsLookupTimer,dscpLabel,eNodeBPlmnId' |  200 | _
        ROOT_MO                    | "BASE_ALL"    | '//ENodeBFunction_BlackListed'                                    | 'userLabel,dnsLookupOnTai,dnsLookupTimer,dscpLabel,eNodeBPlmnId' |  200 | _
        ROOT_MO                    | "BASE_SUBTREE"    | '//ENodeBFunction_BlackListed'                                    | 'userLabel,dnsLookupOnTai,dnsLookupTimer,dscpLabel,eNodeBPlmnId' |  200 | _
        ROOT_MO                    | "BASE_NTH_LEVEL"    | '//ENodeBFunction_BlackListed'                                    | 'userLabel,dnsLookupOnTai,dnsLookupTimer,dscpLabel,eNodeBPlmnId' |  200 | _

        'SubNetwork=Sample'      | "BASE_ALL"    | '/MeContext/ManagedElement/ENodeBFunction_BlackListed'         | 'userLabel,dnsLookupOnTai,dnsLookupTimer,dscpLabel,eNodeBPlmnId,nnsfMode' |  417 | _
        'SubNetwork=Sample'      | "BASE_ALL"    | '//ENodeBFunction_BlackListed'                                    | 'userLabel,dnsLookupOnTai,dnsLookupTimer,dscpLabel,eNodeBPlmnId,nnsfMode' |  417 | _
        'SubNetwork=Sample'      | "BASE_SUBTREE"    | '//ENodeBFunction_BlackListed'                                    | 'userLabel,dnsLookupOnTai,dnsLookupTimer,dscpLabel,eNodeBPlmnId,nnsfMode' |  417 | _
        'SubNetwork=Sample'      | "BASE_NTH_LEVEL"    | '//ENodeBFunction_BlackListed'                                    | 'userLabel,dnsLookupOnTai,dnsLookupTimer,dscpLabel,eNodeBPlmnId,nnsfMode' |  417 | _

        //with FDN and attributes=null
        'MeContext=ERBS001'                    | "BASE_ALL"    | '/MeContext/ManagedElement/ENodeBFunction'                                    | null | 200 | _
        'MeContext=ERBS001'                    | "BASE_ALL"    | '/MeContext/ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'   | null |  200 | _
        'MeContext=ERBS001'                   | "BASE_ALL"    | 'MeContext/ManagedElement/ENODEBFUNCTION[attributes/USERLABEL="value"]'   | null |  200 | _
        'MeContext=ERBS001'                   | "BASE_ALL"    | '//ENODEBFUNCTION[attributes/USERLABEL="value"]'   | null |  200 | _
        'MeContext=ERBS001'                    | "BASE_SUBTREE"    | '/MeContext/ManagedElement/ENodeBFunction'                                    | null | 200 | _
        'MeContext=ERBS001'                    | "BASE_NTH_LEVEL"    | '/MeContext/ManagedElement/ENodeBFunction'                                    | null | 200 | _

    }

    def fillNotEmptyDynamicBlackListForNbi() {
        HashMap<String, DynamicBlackListEntry> map = mockedDynamicBlackListInfoIf.getDynamicBlackListMapForNbi()
        map.put("ERBS_NODE_MODEL:ENodeBFunction", new DynamicBlackListEntry("ERBS_NODE_MODEL:ENodeBFunction",
                DynamicBlackListEntry.SOURCE_DYNAMIC, DynamicBlackListEntry.STATUS_ENABLED, 110000));

        map.put("blacklisted1:ENodeBFunction_BlackListed", new DynamicBlackListEntry("blacklisted1:ENodeBFunction_BlackListed",
                DynamicBlackListEntry.SOURCE_DYNAMIC, DynamicBlackListEntry.STATUS_ENABLED, 25000));

        map.put("blacklisted2:ENodeBFunction_BlackListed", new DynamicBlackListEntry("blacklisted2:ENodeBFunction_BlackListed",
                DynamicBlackListEntry.SOURCE_DYNAMIC, DynamicBlackListEntry.STATUS_ENABLED, 6000));
    }

    def 'dummy test to force maven to run test class with all other test methods annotated with @Unroll'() {
        when:
        objUnderTest.toString()
        then:
        1 == 1
    }
}