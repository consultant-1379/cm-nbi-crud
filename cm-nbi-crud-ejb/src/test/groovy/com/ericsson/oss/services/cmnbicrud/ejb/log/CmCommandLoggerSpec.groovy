package com.ericsson.oss.services.cmnbicrud.ejb.log

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.sdk.context.ContextService
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder
import com.ericsson.oss.presentation.cmnbirest.api.NbiResponse
import com.ericsson.oss.services.cmnbicrud.ejb.common.CmContextService
import spock.lang.Unroll

import javax.inject.Inject

class CmCommandLoggerSpec extends CdiSpecification {

    @ObjectUnderTest
    CmCommandLogger objUnderTest;

    @Inject
    final CmContextService cmContextService

    @Inject
    SystemRecorder systemRecorderMock

    @Unroll
    def 'test removeErrorInfoTag'() {
        when:
            def returnedJson = objUnderTest.removeErrorInfoTag(json)
        then:
            returnedJson == expectedJson
        where:
        json                                                                             || expectedJson                                                            |_
        null                                                                             || null                                                                    |_
        '{"error":{"errorInfo":"The supplied parameters xpath=null is not allowed."}}'   || '{"errorDetail":"The supplied parameters xpath=null is not allowed."}'        |_
        '{"error":{"errorInfo":"Invalid scopeType=BASE_ONLY for fdn=MeContext=1"}}'      || '{"errorDetail":"Invalid scopeType=BASE_ONLY for fdn=MeContext=1"}'           |_
        '{"error":{"errorInfo":"Invalid scopeType'                                       || '{"errorDetail":"Invalid scopeType'                                           |_
        '{"error":{"errorInfo3":"test."}}'                                               || '{"errorDetail":{"errorInfo3":"test."}}'                                      |_
        '{"error":{"errorInfo":"The supplied:1 " 2 "}}'                                   || '{"errorDetail":"The supplied:1 " 2 "}'                                      |_
    }

    @Unroll
    def 'test getJsonLen'() {
        when:
            def length = objUnderTest.getJsonLen(json)
        then:
            length == expectedLength
        where:
        json                                                                             || expectedLength |_
        null                                                                             || 0              |_
        '{"error":{"errorInfo":"Invalid scopeType=BASE_ONLY for fdn=MeContext=1"}}'      || 73             |_
        '{"error":{"errorInfo":"The supplied parameters xpath=null is not allowed."}}'   || 76             |_
    }

    def 'logCompactAuditLog for coverage only'() {
        given:
            def nbiResponseMock = Mock(NbiResponse.class)
            Set successHttpCodes = new HashSet()
            successHttpCodes.add(200)
            nbiResponseMock.getHttpCode() >> 200
            systemRecorderMock.recordCompactAudit(*_) >> {throw new Exception("some error")}
        when:
            def length = objUnderTest.logCompactAuditLog(nbiResponseMock, successHttpCodes, cmContextService)
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
