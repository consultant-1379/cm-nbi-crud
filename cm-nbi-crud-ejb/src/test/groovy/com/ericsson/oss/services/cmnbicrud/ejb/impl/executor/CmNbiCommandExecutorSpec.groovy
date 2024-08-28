package com.ericsson.oss.services.cmnbicrud.ejb.impl.executor

import com.ericsson.oss.itpf.sdk.security.accesscontrol.SecurityViolationException
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudDeleteRequest
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudGetRequest
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudActionRequest
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudPatchRequest
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudPostRequest
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudPutRequest
import com.ericsson.oss.presentation.cmnbirest.api.NbiRequest
import com.ericsson.oss.presentation.cmnbirest.api.NbiResponse
import com.ericsson.oss.presentation.cmnbirest.api.OperationType
import com.ericsson.oss.presentation.cmnbirest.api.PatchContentType

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.sdk.eventbus.Channel
import com.ericsson.oss.itpf.sdk.eventbus.Event
import spock.lang.Unroll

import javax.inject.Inject


class CmNbiCommandExecutorSpec extends CdiSpecification {

    @ObjectUnderTest
    private CmNbiCommandExecutor objUnderTest

    @Inject
    private Channel mockedScriptEngineOutputChannel

    def mockedEvent = Mock(Event)

    @MockedImplementation
    private CmNbiCrudGetExecutor mockedCmNbiCrudGetExecutor;

    @MockedImplementation
    private CmNbiCrudPutExecutor mockedCmNbiPutCreateExecutor;

    @MockedImplementation
    private CmNbiCrudCreateExecutor mockedCmNbiCrudCreateExecutor;

    @MockedImplementation
    private CmNbiCrudDeleteExecutor mockedCmNbiCrudDeleteExecutor;

    @MockedImplementation
    private CmNbiCrudPatchExecutor mockedCmNbiCrudPatchExecutor;

    @MockedImplementation
    private CmNbiCrudActionExecutor mockedCmNbiCrudActionExecutor;

    String userId = "user"
    String requestId = "crud:1234"
    String fields = null
    String attributes = null
    String filter = null
    static def ipAddress = "1.2.3.5"
    static def ssoToken = "CookieAABBCCDDEE";

    static def mockedNbiCrudGetRequest = new NbiCrudGetRequest('userId', 'requestId', ipAddress, ssoToken, '' , '', '', null, 0, '')
    static def mockedNbiCrudPutRequestModify = new NbiCrudPutRequest('userId', 'requestId', ipAddress, ssoToken, 'xpath' , 'body')
    static def mockedNbiCrudPutRequestCreate = new NbiCrudPutRequest('userId', 'requestId', ipAddress, ssoToken, 'xpath' , 'body')

    def setup() {
    }

    def 'receiveCommandRequest.receiveCommandRequest(NbiCrudGetRequest) throws Exception'() {
        given:
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , "BASE_ONLY", fields, attributes, 0, filter)

        and: 'Exception is raised'
        mockedCmNbiCrudGetExecutor.executeOperation(_) >> { throw new Exception("generic exception") }

        when: 'the event is consumed'
        objUnderTest.processCommandRequest(requestId, nbiCrudGetRequest)
        then: 'exception is thrown'
        def exception = thrown(Exception)
    }

    def 'receiveCommandRequest.receiveCommandRequest(NbiCrudGetRequest) throws SecurityViolationException'() {
        given:
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , null, fields, attributes, 0, filter)

        and: 'Exception is raised'
        mockedCmNbiCrudGetExecutor.executeOperation(_) >> { throw new SecurityViolationException("Insufficient access rights to perform the operation") }

        when: 'the event is consumed'
        objUnderTest.processCommandRequest(requestId, nbiCrudGetRequest)
        then: 'exception is thrown'
        1 * mockedScriptEngineOutputChannel.createEvent( {
            NbiResponse nbiResponse = (NbiResponse)it
            assert nbiResponse.httpCode == 401
            assert nbiResponse.jsonContent.contains("Insufficient access rights to perform the operation")
            return true
        }, requestId) >> mockedEvent
    }

    def 'receiveCommandRequest.receiveCommandRequest(NbiCrudDeleteRequest) throws SecurityViolationException'() {
        given:
        NbiCrudDeleteRequest nbiCrudDeleteRequest = new NbiCrudDeleteRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , null, 0, null)

        and: 'Exception is raised'
        mockedCmNbiCrudDeleteExecutor.executeOperation(_) >> { throw new SecurityViolationException("Insufficient access rights to perform the operation") }

        when: 'the event is consumed'
        objUnderTest.processCommandRequest(requestId, nbiCrudDeleteRequest)
        then: 'exception is thrown'
        1 * mockedScriptEngineOutputChannel.createEvent( {
            NbiResponse nbiResponse = (NbiResponse)it
            assert nbiResponse.httpCode == 401
            assert nbiResponse.jsonContent.contains("Insufficient access rights to perform the operation")
            return true
        }, requestId) >> mockedEvent
    }

     def 'receiveCommandRequest.receiveCommandRequest(NbiCrudGetRequest) send correct response'() {
        given:
        NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , "BASE_ONLY", fields, attributes, 0, filter)

        and: 'NbiResponse is returned'
        mockedCmNbiCrudGetExecutor.executeOperation(_) >> new NbiResponse(200, "{a:b}")

        when: 'the event is consumed'
        objUnderTest.processCommandRequest(requestId, nbiCrudGetRequest)

        then: 'the correct response for the command is dispatched to the script-engine output queue'
        1 * mockedScriptEngineOutputChannel.createEvent( {
             NbiResponse nbiResponse = (NbiResponse)it
             assert nbiResponse.httpCode == 200
             assert nbiResponse.jsonContent == "{a:b}"
            return true
        }, requestId) >> mockedEvent
        1 * mockedScriptEngineOutputChannel.send(mockedEvent, { eventConfiguration ->
            eventConfiguration.eventProperties.get('skipCache') == 'true'
        })
   }

    @Unroll
    def 'receiveCommandRequest.receiveCommandRequest(NbiCrudPutRequest MODIFY) send correct response'() {
        given:
        def xpath = 'MeContext=ERBS001FORNBI,ManagedElement=1'
        def body = '{ "ManagedElement": [{ "id": "1", "attributes": {"logicalName": "newLogicalName"} }] }'
        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        and: 'NbiResponse is returned'
        mockedCmNbiPutCreateExecutor.isAnUpdateOperation(nbiCrudPutRequest) >> true
        mockedCmNbiPutCreateExecutor.executeOperation(_) >> new NbiResponse(200, "{a:b}")

        when: 'the event is consumed'
        objUnderTest.processCommandRequest(requestId, nbiCrudPutRequest)

        then: 'the correct response for the command is dispatched to the script-engine output queue'
        1 * mockedScriptEngineOutputChannel.createEvent( {
            NbiResponse nbiResponse = (NbiResponse)it
            assert nbiResponse.httpCode == 200
            assert nbiResponse.jsonContent == "{a:b}"
            return true
        }, requestId) >> mockedEvent
        1 * mockedScriptEngineOutputChannel.send(mockedEvent, { eventConfiguration ->
            eventConfiguration.eventProperties.get('skipCache') == 'true'
        })
    }

    @Unroll
    def 'receiveCommandRequest.receiveCommandRequest(NbiCrudPutRequest CREATE) send correct response'() {
        given:
        def xpath = null
        def body ='{ "NetworkElement": [ { "id": "LTETA0001FDD", "attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" }}] }'
        NbiCrudPutRequest nbiCrudPutRequest = new NbiCrudPutRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        and: 'NbiResponse is returned'
        mockedCmNbiCrudCreateExecutor.executeOperation(_) >> new NbiResponse(201, "{a:b}")

        when: 'the event is consumed'
        objUnderTest.processCommandRequest(requestId, nbiCrudPutRequest)

        then: 'the correct response for the command is dispatched to the script-engine output queue'
        1 * mockedScriptEngineOutputChannel.createEvent( {
            NbiResponse nbiResponse = (NbiResponse)it
            assert nbiResponse.httpCode == 201
            assert nbiResponse.jsonContent == "{a:b}"
            return true
        }, requestId) >> mockedEvent
        1 * mockedScriptEngineOutputChannel.send(mockedEvent, { eventConfiguration ->
            eventConfiguration.eventProperties.get('skipCache') == 'true'
        })
    }

    @Unroll
    def 'receiveCommandRequest.receiveCommandRequest(NbiCrudPostRequest) send correct response'() {
        given:
        def xpath = null
        def body ='{ "NetworkElement": [ { "id": "LTETA0001FDD", "attributes": { "ossPREFIX":"22" , "NETYPE": "ERBS" }}] }'
        NbiCrudPostRequest nbiCrudPostRequest = new NbiCrudPostRequest(userId, requestId, ipAddress, ssoToken, xpath , body)

        and: 'NbiResponse is returned'
        mockedCmNbiCrudCreateExecutor.executeOperation(_) >> new NbiResponse(201, "{a:b}")

        when: 'the event is consumed'
        objUnderTest.processCommandRequest(requestId, nbiCrudPostRequest)

        then: 'the correct response for the command is dispatched to the script-engine output queue'
        1 * mockedScriptEngineOutputChannel.createEvent( {
            NbiResponse nbiResponse = (NbiResponse)it
            assert nbiResponse.httpCode == 201
            assert nbiResponse.jsonContent == "{a:b}"
            return true
        }, requestId) >> mockedEvent
        1 * mockedScriptEngineOutputChannel.send(mockedEvent, { eventConfiguration ->
            eventConfiguration.eventProperties.get('skipCache') == 'true'
        })
    }

    @Unroll
    def 'receiveCommandRequest.receiveCommandRequest(NbiCrudDeleteRequest) send correct response'() {
        given:
        def xpath = null
        NbiCrudDeleteRequest nbiCrudDeleteRequest = new NbiCrudDeleteRequest(userId, requestId, ipAddress, ssoToken, xpath , "BASE_ONLY", 0, filter)

        and: 'NbiResponse is returned'
        mockedCmNbiCrudDeleteExecutor.executeOperation(_) >> new NbiResponse(200, "{a:b}")

        when: 'the event is consumed'
        objUnderTest.processCommandRequest(requestId, nbiCrudDeleteRequest)

        then: 'the correct response for the command is dispatched to the script-engine output queue'
        1 * mockedScriptEngineOutputChannel.createEvent( {
            NbiResponse nbiResponse = (NbiResponse)it
            assert nbiResponse.httpCode == 200
            assert nbiResponse.jsonContent == "{a:b}"
            return true
        }, requestId) >> mockedEvent
        1 * mockedScriptEngineOutputChannel.send(mockedEvent, { eventConfiguration ->
            eventConfiguration.eventProperties.get('skipCache') == 'true'
        })
    }


    @Unroll
    def 'receiveCommandRequest.receiveCommandRequest(NbiCrudPatchRequest) send correct response'() {
        given:
        def xpath = null
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, xpath, '[some operations]', PatchContentType.THREE_GPP_JSON_PATCH)

        and: 'NbiResponse is returned'
        mockedCmNbiCrudPatchExecutor.executeOperation(_) >> new NbiResponse(200, "{[a:b]}")

        when: 'the event is consumed'
        objUnderTest.processCommandRequest(requestId, nbiCrudPatchRequest)

        then: 'the correct response for the command is dispatched to the script-engine output queue'
        1 * mockedScriptEngineOutputChannel.createEvent( {
            NbiResponse nbiResponse = (NbiResponse)it
            assert nbiResponse.httpCode == 200
            assert nbiResponse.jsonContent == "{[a:b]}"
            return true
        }, requestId) >> mockedEvent
        1 * mockedScriptEngineOutputChannel.send(mockedEvent, { eventConfiguration ->
            eventConfiguration.eventProperties.get('skipCache') == 'true'
        })
    }

    def 'receiveCommandRequest.receiveCommandRequest(NbiCrudPatchRequest) throws SecurityViolationException'() {
        given:
        NbiCrudPatchRequest nbiCrudPatchRequest = new NbiCrudPatchRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , '[some operations]', PatchContentType.JSON_PATCH)

        and: 'Exception is raised'
        mockedCmNbiCrudPatchExecutor.executeOperation(_) >> { throw new SecurityViolationException("Insufficient access rights to perform the operation") }

        when: 'the event is consumed'
        objUnderTest.processCommandRequest(requestId, nbiCrudPatchRequest)
        then: 'exception is thrown'
        1 * mockedScriptEngineOutputChannel.createEvent( {
            NbiResponse nbiResponse = (NbiResponse)it
            assert nbiResponse.httpCode == 401
            assert nbiResponse.jsonContent.contains("Insufficient access rights to perform the operation")
            return true
        }, requestId) >> mockedEvent
    }

    @Unroll
    def 'receiveCommandRequest.receiveCommandRequest(NbiCrudActionRequest) send correct response'() {
        given:
        def xpath = null
        NbiCrudActionRequest nbiCrudActionRequest = new NbiCrudActionRequest(userId, requestId, ipAddress, ssoToken, xpath, 'someAction', '[some operations]')

        and: 'NbiResponse is returned'
        mockedCmNbiCrudActionExecutor.executeOperation(_ as NbiCrudActionRequest) >> new NbiResponse(200, "{\"output\":\"{a:b}}")

        when: 'the event is consumed'
        objUnderTest.processCommandRequest(requestId, nbiCrudActionRequest)

        then: 'the correct response for the command is dispatched to the script-engine output queue'
        1 * mockedScriptEngineOutputChannel.createEvent( {
            NbiResponse nbiResponse = (NbiResponse)it
            assert nbiResponse.httpCode == 200
            assert nbiResponse.jsonContent == "{\"output\":\"{a:b}}"
            return true
        }, requestId) >> mockedEvent
        1 * mockedScriptEngineOutputChannel.send(mockedEvent, { eventConfiguration ->
            eventConfiguration.eventProperties.get('skipCache') == 'true'
        })
    }

    def 'dummy test to force maven to run test class with all other test methods annotated with @Unroll'() {
        when:
        objUnderTest.toString()
        then:
        true
    }
}
