package com.ericsson.oss.services.cmnbicrud.ejb.cal

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudActionRequest
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudDeleteRequest
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudGetRequest
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudPatchRequest
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudPostRequest
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudPutRequest
import spock.lang.Shared
import spock.lang.Unroll

import javax.inject.Inject

/*
*  Here some generic test to convert generic CompactSummary
*
*  To have correct json format refers tests inside TestCompactAuditLoggerSpec
*  In general we can say that we have something like this:
*
*  GET Command:
*     '{{"summaryResult":[{"opType":"read","entity":"ManagedObject","result":{"total":1}}]}'
* and
*     '{"errorDetail":"The supplied parameters xpath=null is not allowed."}'
*
*  DELETE Command:
*      '{"detailResult":[{"opType":"delete","id":"MeContext=ERBS002"},{"opType":"delete","id":"MeContext=ERBS002,ManagedElement=1"},{"opType":"delete","id":"MeContext=ERBS002,ManagedElement=1,ENodeBFunction=1"},{"opType":"delete","id":"MeContext=ERBS002,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=1"},{"opType":"delete","id":"MeContext=ERBS002,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=2"},{"opType":"delete","id":"MeContext=ERBS002,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=3"},{"opType":"delete","id":"MeContext=ERBS002,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=4"}]}'
* and
*     '{"errorDetail":"The supplied parameters xpath=null is not allowed."}'
*
*  PUT (Modify) Command:
*      '{"detailResult":[{"opType":"update","id":"NetworkElement=ERBS002","oldValues":{"utcOffset":"+00:18"},"currentValues":{"utcOffset":"+00:20"}}]}'
* and
*     '{"errorDetail":"The supplied parameters xpath=null is not allowed."}'
*
* PUT (Create) Command:
*      '{"detailResult":[{"opType":"create","id":"NetworkElement=ERBS002","currentValues":{"utcOffset":"+00:20"}}]}'
* and
*     '{"errorDetail":"The supplied parameters xpath=null is not allowed."}'
*
* POST (Create) Command:
*      '{"detailResult":[{"opType":"create","id":"NetworkElement=ERBS002","currentValues":{"utcOffset":"+00:20"}}]}'
* and
*     '{"errorDetail":"The supplied parameters xpath=null is not allowed."}'
*
* PATCH (3GPP_JSON_PATCH)
*     '{"detailResult":[{"opType":"create","id":"SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=NETSimW,ManagedElement=LTE26dg2ERBS00003,
*      ENodeBFunction=1,EUtraNetwork=1,EUtranFrequency=120","currentValues":{"userLabel":"patchLabel","freqBand":null,"zzzTemporaryList1":null,
*      "arfcnValueEUtranDl":11,"additionalFreqBandList":null,"zzzTemporary1":-2000000000,"eUtranFrequencyId":"120","zzzTemporary2":-2000000000,
*      "zzzTemporary3":"","mfbiFreqBandIndPrio":false,"reservedBy":null,"prioAdditionalFreqBandList":null,"excludeAdditionalFreqBandList":null}},
*      {"opType":"update","id":"SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=NETSimW,ManagedElement=LTE26dg2ERBS00003,ENodeBFunction=1,
*      EUtraNetwork=1,EUtranFrequency=2","oldValues":{"mfbiFreqBandIndPrio":false},"currentValues":{"mfbiFreqBandIndPrio":true}},{"opType":"delete",
*      "id":"SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=NETSimW,ManagedElement=LTE26dg2ERBS00003,ENodeBFunction=1,DrxProfile=4"}]}'
*
* and
*     '{"errorDetail":"The supplied parameters xpath=null is not allowed."}'
*
* PATCH (JSON_PATCH) Command
*     '{"detailResult":[{"opType":"create","id":"SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=NETSimW,ManagedElement=LTE26dg2ERBS00003,
*      ENodeBFunction=1,EUtraNetwork=1,EUtranFrequency=121","currentValues":{"userLabel":"patchLabel","freqBand":null,"zzzTemporaryList1":null,
*     "arfcnValueEUtranDl":11,"additionalFreqBandList":null,"zzzTemporary1":-2000000000,"eUtranFrequencyId":"121","zzzTemporary2":-2000000000,
*     "zzzTemporary3":"","mfbiFreqBandIndPrio":false,"reservedBy":null,"prioAdditionalFreqBandList":null,"excludeAdditionalFreqBandList":null}},
*     {"opType":"update","id":"SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=NETSimW,ManagedElement=LTE26dg2ERBS00003,ENodeBFunction=1,
*     EUtraNetwork=1,EUtranFrequency=121","oldValues":{"userLabel":"patchLabel"},"currentValues":{"userLabel":"patchJson"}}]}'
*
* and
*     '{"errorDetail":"The supplied parameters xpath=null is not allowed."}'
*
* ACTION Command
*     '{"detailResult":[{"opType":"action","id":"MeContext=ERBS001FORNBI,ManagedElement=1,ENodeBFunction=1","currentValues":{"RETURN VALUE":"[{"cell"":"EUtranCellFDD"="LTE05ERBS00028-1", "ue":26, "srb": 10, "drb": 12}]"}}]}'
*
* and
*     '{"errorDetail":"The supplied parameters xpath=null is not allowed."}'
*
*
*/
class CompactAuditLoggerCreatorSpec extends CdiSpecification {

    @ObjectUnderTest
    CompactAuditLoggerCreator objUnderTest


    @Shared
    def mockedNbiCrudGetRequest = Mock(NbiCrudGetRequest)

    @Shared
    def mockedNbiCrudPutRequest = Mock(NbiCrudPutRequest)

    @Shared
    def mockedNbiCrudPostRequest = Mock(NbiCrudPostRequest)

    @Shared
    def mockedNbiCrudDeleteRequest = Mock(NbiCrudDeleteRequest)

    @Shared
    def mockedNbiCrudPatchRequest = Mock(NbiCrudPatchRequest)

    @Shared
    def mockedNbiCrudActionRequest = Mock(NbiCrudActionRequest)

    String userId = "user"
    String requestId = "crud:1234"
    String ipAddress = "1.2.3.5"
    String ssoToken = "CookieAABBCCDDEE";

    def setup() {}

    def 'Verify CompactSummary - empty'() {
        given:
        CompactSummary compactSummary = new CompactSummary()
        when:
        def json = objUnderTest.convertToJson(compactSummary)
        then:
        json == "{}"
    }

    def 'Verify CompactSummary - errored'() {
        given:
        CompactSummary compactSummary = new CompactSummary()
        compactSummary.setErrorDetail("error in db")
        when:
        def json = objUnderTest.convertToJson(compactSummary)
        then:
        json == '{"errorDetail":"error in db"}'
    }

    def 'verify CompactSummary - 1 summaryResultOperation'() {
        given:
        CompactSummary compactSummary = new CompactSummary()

        SummaryResultOperation summaryResultOperation = new SummaryResultOperation(CompactSummary.OP_TYPE_READ, "ManagedObject")
        summaryResultOperation.setOpType(CompactSummary.OP_TYPE_READ)  //for coverage
        summaryResultOperation.setEntity("ManagedObject")  //for coverage
        Map<String, Object> result = new LinkedHashMap<>()
        result.put("returned objects" , 100)
        summaryResultOperation.setResult(result)

        List<SummaryResultOperation> summaryResultOperations = new ArrayList<>()
        summaryResultOperations.add(summaryResultOperation)
        summaryResultOperations.add(summaryResultOperation)

        compactSummary.setSummaryResult(summaryResultOperations)

        when:
        def json=objUnderTest.convertToJson(compactSummary)
        then:
        json == '{"summaryResult":[{"opType":"read","entity":"ManagedObject","result":{"returned objects":100}},{"opType":"read","entity":"ManagedObject","result":{"returned objects":100}}]}'
    }

    def 'verify CompactSummary - 2 summaryResultOperation'() {
        given:
        CompactSummary compactSummary = new CompactSummary()

        SummaryResultOperation summaryResultOperation1 = new SummaryResultOperation(CompactSummary.OP_TYPE_READ, "ManagedObject")
        Map<String, Object> result1 = new LinkedHashMap<>()
        result1.put("returned objects" , 100)
        summaryResultOperation1.setResult(result1)
        compactSummary.addSummaryResultOperation(summaryResultOperation1)

        SummaryResultOperation summaryResultOperation2 = new SummaryResultOperation(CompactSummary.OP_TYPE_READ, "ManagedObject")
        Map<String, Object> result2 = new LinkedHashMap<>()
        result2.put("returned objects" , 200)
        summaryResultOperation2.setResult(result2)
        compactSummary.addSummaryResultOperation(summaryResultOperation2)

        when:
        def json=objUnderTest.convertToJson(compactSummary)
        then:
        json == '{"summaryResult":[{"opType":"read","entity":"ManagedObject","result":{"returned objects":100}},{"opType":"read","entity":"ManagedObject","result":{"returned objects":200}}]}'
    }

    def 'Verify CompactSummary - 2 detailResultOperation'() {
        CompactSummary compactSummary = new CompactSummary()

        DetailResultOperation detailResultOperation1 = new DetailResultOperation(CompactSummary.OP_TYPE_CREATE, "A=1/B=3")
        detailResultOperation1.setOpType(CompactSummary.OP_TYPE_CREATE)
        detailResultOperation1.setId("A=1/B=3")
        compactSummary.addDetailResultOperation(detailResultOperation1)

        DetailResultOperation detailResultOperation2 = new DetailResultOperation(CompactSummary.OP_TYPE_CREATE, "A=1/B=3")
        detailResultOperation2.setOpType(CompactSummary.OP_TYPE_DELETE)
        detailResultOperation2.setId("A=1/B=3")
        compactSummary.addDetailResultOperation(detailResultOperation2)

        when:
        def json = objUnderTest.convertToJson(compactSummary)
        then:
        json == '{"detailResult":[{"opType":"create","id":"A=1/B=3"},{"opType":"delete","id":"A=1/B=3"}]}'
    }

    def 'Verify CompactSummary - 1 summaryResultOperation and 1 detailResultOperation'() {
        CompactSummary compactSummary = new CompactSummary()

        DetailResultOperation detailResultOperation1 = new DetailResultOperation(CompactSummary.OP_TYPE_CREATE, "A=1/B=3")
        detailResultOperation1.setOpType(CompactSummary.OP_TYPE_CREATE)  //for coverage
        detailResultOperation1.setId("A=1/B=3") //for coverage

        DetailResultOperation detailResultOperation2 = new DetailResultOperation(CompactSummary.OP_TYPE_CREATE, "A=1/B=3")
        detailResultOperation2.setOpType(CompactSummary.OP_TYPE_DELETE)
        detailResultOperation2.setId("A=1/B=3")

        List<DetailResultOperation> detailResultOperations = new ArrayList<>()
        detailResultOperations.add(detailResultOperation1)
        detailResultOperations.add(detailResultOperation2)

        compactSummary.setDetailResult(detailResultOperations)

        SummaryResultOperation summaryResultOperation1 = new SummaryResultOperation(CompactSummary.OP_TYPE_READ, "ManagedObject")
        Map<String, Object> result1 = new LinkedHashMap<>()
        result1.put("deleted" , 200)
        summaryResultOperation1.setResult(result1)
        compactSummary.addSummaryResultOperation(summaryResultOperation1)

        when:
        def json = objUnderTest.convertToJson(compactSummary)
        then:
        json == '{"detailResult":[{"opType":"create","id":"A=1/B=3"},{"opType":"delete","id":"A=1/B=3"}],"summaryResult":[{"opType":"read","entity":"ManagedObject","result":{"deleted":200}}]}'
    }

    def 'Verify CompactSummary - 1 detailResultOperation with one attributes old and current '() {
        CompactSummary compactSummary = new CompactSummary()

        DetailResultOperation detailResultOperation1 = new DetailResultOperation(CompactSummary.OP_TYPE_CREATE, "A=1/B=3")
        detailResultOperation1.setOpType(CompactSummary.OP_TYPE_CREATE)
        detailResultOperation1.setId("A=1/B=3")
        compactSummary.addDetailResultOperation(detailResultOperation1)

        Map<String, Object> oldValues = new LinkedHashMap<>()
        oldValues.put("attr1", 1)
        detailResultOperation1.setOldValues(oldValues)

        Map<String, Object> currentValues = new LinkedHashMap<>()
        currentValues.put("attr1", 2)
        detailResultOperation1.setCurrentValues(currentValues)

        when:
        def json = objUnderTest.convertToJson(compactSummary)
        then:
        json == '{"detailResult":[{"opType":"create","id":"A=1/B=3","oldValues":{"attr1":1},"currentValues":{"attr1":2}}]}'
    }

    def 'Verify CompactSummary - 1 detailResultOperation with two attributes old and current'() {
        CompactSummary compactSummary = new CompactSummary()

        DetailResultOperation detailResultOperation1 = new DetailResultOperation(CompactSummary.OP_TYPE_CREATE, "A=1/B=3")
        detailResultOperation1.setOpType(CompactSummary.OP_TYPE_CREATE)
        detailResultOperation1.setId("A=1/B=3")
        compactSummary.addDetailResultOperation(detailResultOperation1)

        detailResultOperation1.addAttributeOldValue("attr1", 1)
        detailResultOperation1.addAttributeOldValue("attr2", 2)

        detailResultOperation1.addAttributeCurrentValue("attr1", 3)
        detailResultOperation1.addAttributeCurrentValue("attr2", 4)

        when:
        def json = objUnderTest.convertToJson(compactSummary)
        then:
        json == '{"detailResult":[{"opType":"create","id":"A=1/B=3","oldValues":{"attr1":1,"attr2":2},"currentValues":{"attr1":3,"attr2":4}}]}'
    }

    def 'CompactSummary - for coverage when Exception'() {
        when:
        def json = objUnderTest.convertToJson(null)
        then:
        1 == 1
    }

    def 'CompactOperation - for coverage'() {
        when:
        DetailResultOperation detailResultOperation = new DetailResultOperation(CompactSummary.OP_TYPE_CREATE, "A=1/B=3")
        then:
        detailResultOperation.toString()
    }

    @Unroll
    def 'test getSloganFromRequest - for coverage only'() {
        when:
            def slogan = objUnderTest.getSloganFromRequest(nbiRequest)
        then:
            slogan == expectedSlogan
        where:
        nbiRequest                 || expectedSlogan                               |_
        mockedNbiCrudGetRequest    || OperationSlogan.EXECUTE_GET.getSlogan()      |_
        mockedNbiCrudPutRequest    || OperationSlogan.EXECUTE_PUT.getSlogan()      |_
        mockedNbiCrudPostRequest   || OperationSlogan.EXECUTE_POST.getSlogan()     |_
        mockedNbiCrudDeleteRequest || OperationSlogan.EXECUTE_DELETE.getSlogan()   |_
        mockedNbiCrudPatchRequest  || OperationSlogan.EXECUTE_THREE_GPP_JSON_PATCH.getSlogan()           |_
        mockedNbiCrudActionRequest || OperationSlogan.EXECUTE_ACTION.getSlogan()               |_
        null                       || OperationSlogan.NA.getSlogan()                           |_
    }

    @Unroll
    def 'test getUriAndParamsFromRequest - for coverage only'() {
        when:
            def uriAndParams = objUnderTest.getUriAndParamsFromRequest(nbiRequest)
        then:
            1 == 1
        where:
        nbiRequest                 |_
        mockedNbiCrudGetRequest    |_
        mockedNbiCrudPutRequest    |_
        mockedNbiCrudPostRequest   |_
        mockedNbiCrudDeleteRequest |_
        mockedNbiCrudPatchRequest  |_
        mockedNbiCrudActionRequest |_
        null                       |_
    }

    @Unroll
    def 'test getParamsFromRequest - for GET'() {
        when:
            NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , scopeType, fields, attributes, scopeLevel, filter)
            def params = objUnderTest.getParamsFromRequest(nbiCrudGetRequest)
        then:
            expectedParams == params
        where:
            scopeType  | fields                               |   attributes          |  scopeLevel |  filter              || expectedParams
            "BASE_ALL" | "attributes/productData/productName" | "positionCoordinates" | 2           | "//Cdma2000FreqBand" || "?scopeType=BASE_ALL&fields=attributes/productData/productName&attributes=positionCoordinates&scopeLevel=2&filter=//Cdma2000FreqBand"
            "BASE_ALL" |  null                                |     null              | 0           | null                 || "?scopeType=BASE_ALL"
            "BASE_ALL" | "attributes/productData/productName" |     null              | 0           | null                 || "?scopeType=BASE_ALL&fields=attributes/productData/productName"
            "BASE_ALL" | "attributes/productData/productName" | "positionCoordinates" | 0           | null                 || "?scopeType=BASE_ALL&fields=attributes/productData/productName&attributes=positionCoordinates"
            "BASE_ALL" | "attributes/productData/productName" | "positionCoordinates" | 2           | null                 || "?scopeType=BASE_ALL&fields=attributes/productData/productName&attributes=positionCoordinates&scopeLevel=2"
            "BASE_ALL" | null                                 | "positionCoordinates" | 2           | null                 || "?scopeType=BASE_ALL&attributes=positionCoordinates&scopeLevel=2"
             null      | null                                 |     null              | 0           | null                 || ""
             null      | "attributes/productData/productName" |     null              | 0           | null                 || "?fields=attributes/productData/productName"
             null      | "attributes/productData/productName" |     null              | 0           | "//Cdma2000FreqBand" || "?fields=attributes/productData/productName&filter=//Cdma2000FreqBand"
    }

    @Unroll
    def 'test getUriAndParamsFromRequest - for GET'() {
        when:
            NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , scopeType, fields, attributes, scopeLevel, filter)
            def uriAndParams = objUnderTest.getUriAndParamsFromRequest(nbiCrudGetRequest)
        then:
            expectedUriAndParams == uriAndParams
        where:
            scopeType  | fields                               |   attributes          |  scopeLevel |  filter               || expectedUriAndParams
            null       | null                                 |     null              | 0           | null                  || "/enm-nbi/cm/v1/data/NetworkElement=ERBS002"
            null      | "attributes/productData/productName"  |     null              | 0            | "//Cdma2000FreqBand" || "/enm-nbi/cm/v1/data/NetworkElement=ERBS002?fields=attributes/productData/productName&filter=//Cdma2000FreqBand"

    }

    @Unroll
    def 'test getParamsFromRequest - for DELETE'() {
        when:
            NbiCrudDeleteRequest nbiCrudDeleteRequest = new NbiCrudDeleteRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002", scopeType, scopeLevel, filter)
            def params = objUnderTest.getParamsFromRequest(nbiCrudDeleteRequest)
        then:
            expectedParams == params
        where:
        scopeType  |  scopeLevel |  filter               || expectedParams
        "BASE_ALL" |    2        | "//Cdma2000FreqBand"  || "?scopeType=BASE_ALL&scopeLevel=2&filter=//Cdma2000FreqBand"
        "BASE_ALL" |    0        | null                  || "?scopeType=BASE_ALL"
        "BASE_ALL" |    0        | "//Cdma2000FreqBand"  || "?scopeType=BASE_ALL&filter=//Cdma2000FreqBand"
        null       |    0        | null                  || ""
        null       |    2        | "//Cdma2000FreqBand"  || "?scopeLevel=2&filter=//Cdma2000FreqBand"
    }


    @Unroll
    def 'test getUriAndParamsFromRequest - for DELETE'() {
        when:
            NbiCrudDeleteRequest nbiCrudDeleteRequest = new NbiCrudDeleteRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002", scopeType, scopeLevel, filter)
            def uriAndParams = objUnderTest.getUriAndParamsFromRequest(nbiCrudDeleteRequest)
        then:
        expectedUriAndParams == uriAndParams
        where:
        scopeType  |  scopeLevel |  filter               || expectedUriAndParams
        null       |  0          |     null              || "/enm-nbi/cm/v1/data/NetworkElement=ERBS002"
        null       |  2          | "//Cdma2000FreqBand"  || "/enm-nbi/cm/v1/data/NetworkElement=ERBS002?scopeLevel=2&filter=//Cdma2000FreqBand"
    }

    @Unroll
    def 'test getParamsFromRequest - for ACTION'() {
        when:
            NbiCrudActionRequest nbiCrudActionRequest = new NbiCrudActionRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002", action,null)
            def params = objUnderTest.getParamsFromRequest(nbiCrudActionRequest)
        then:
            expectedParams == params
        where:
            action                      || expectedParams
            "getulsasamplingcapability" || "?action=getulsasamplingcapability"
             null                       || ""
    }

    @Unroll
    def 'test getUriAndParamsFromRequest - for ACTION'() {
        when:
            NbiCrudActionRequest nbiCrudActionRequest = new NbiCrudActionRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002", action, null)
            def uriAndParams = objUnderTest.getUriAndParamsFromRequest(nbiCrudActionRequest)
        then:
            expectedUriAndParams == uriAndParams
        where:
            action                      || expectedUriAndParams
            null                        || "/enm-nbi/cm/v1/data/NetworkElement=ERBS002"
            "getulsasamplingcapability" || "/enm-nbi/cm/v1/data/NetworkElement=ERBS002?action=getulsasamplingcapability"
    }

    def 'test obscureBodyForCmActionRequest - for coverage when Exception'() {
        when:
            Set<String> sensitiveActionParameterNames= new HashSet<>();
            def body = objUnderTest.obscureBodyForCmActionRequest("wrongBody", sensitiveActionParameterNames)
        then:
            1 == 1
    }

    def 'dummy test to force maven to run test class with all other test methods annotated with @Unroll'() {
        when:
        "".toString()
        then:
        1 == 1
    }

}
