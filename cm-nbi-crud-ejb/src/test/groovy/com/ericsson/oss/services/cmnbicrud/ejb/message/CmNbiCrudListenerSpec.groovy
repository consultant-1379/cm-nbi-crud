package com.ericsson.oss.services.cmnbicrud.ejb.message

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.sdk.eventbus.Channel
import com.ericsson.oss.itpf.sdk.eventbus.Event
import com.ericsson.oss.presentation.cmnbirest.api.NbiCrudGetRequest
import com.ericsson.oss.services.cm.cmreader.api.CmReaderService
import com.ericsson.oss.services.cmnbicrud.ejb.impl.executor.CmNbiCommandExecutor
import com.ericsson.oss.services.cmnbicrud.ejb.testutil.JMSHelper
import spock.lang.Unroll

import javax.inject.Inject

class CmNbiCrudListenerSpec extends CdiSpecification {

    @ObjectUnderTest
    private CmNbiCrudListener objUnderTest

    @MockedImplementation
    private CmNbiCommandExecutor mockedCmNbiCommandExecutor

    @Inject
    private CmReaderService mockedCmReaderService

    @Inject
    private Channel mockedCmEditChannel

    def mockedEvent = Mock(Event)

    String userId = "user"
    String requestId = "crud:1234"
    String fields = null
    String attributes = null
    String filter = null
    String ipAddress = "1.2.3.5"
    String ssoToken = "CookieAABBCCDDEE";

    def genericHttpCode = 1234

    def setup() {
        System.setProperty("sdk.eventbus.jms.concurrent.queue.listeners.number.request.CRUD","20")
        System.setProperty("sdk.eventbus.jms.concurrent.queue.listeners.number.request.CRUD_STRONG","3")
    }

    @Unroll
     def 'receiveCommandRequest.receiveRequest(NbiCrudGetRequest) send correct response'() {
        given:
            NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , "BASE_ONLY", fields, attributes, 0, filter)
            mockedCmReaderService.checkIfcommandCanBeProcessed(requestId) >> dbAvailable
            nbiCrudGetRequest.setRetryNumber(retryNumber)

        when: 'the event is consumed'
            objUnderTest.receiveRequest(JMSHelper.createEventForObject(requestId, nbiCrudGetRequest))
        then: ' send back request'
            sendBack * mockedCmEditChannel.createEvent(nbiCrudGetRequest, requestId) >> mockedEvent
            sendBack * mockedCmEditChannel.send(mockedEvent, { eventConfiguration ->
                                eventConfiguration.eventProperties.get('application') == 'CRUD' })
         and: ' process request'
            processRequest * mockedCmNbiCommandExecutor.processCommandRequest(requestId,nbiCrudGetRequest)
         where: ''
            dbAvailable         |   retryNumber     ||  sendBack    |  processRequest
                true            |       0           ||      0       |       1
                true            |       21          ||      0       |       1
                false           |       0           ||      1       |       0
                false           |       20          ||      1       |       0
                false           |       21          ||      0       |       1
   }

    @Unroll
    def 'receiveCommandRequest.receiveStrongRequest(NbiCrudGetRequest) send correct response'() {
        given:
            NbiCrudGetRequest nbiCrudGetRequest = new NbiCrudGetRequest(userId, requestId, ipAddress, ssoToken, "NetworkElement=ERBS002" , "BASE_ONLY", fields, attributes, 0, filter)
            mockedCmReaderService.checkIfcommandCanBeProcessed(requestId) >> dbAvailable
            nbiCrudGetRequest.setRetryNumber(retryNumber)

        when: 'the event is consumed'
            objUnderTest.receiveStrongRequest(JMSHelper.createEventForObject(requestId, nbiCrudGetRequest))
        then: ' send back request'
            sendBack * mockedCmEditChannel.createEvent(nbiCrudGetRequest, requestId) >> mockedEvent
            sendBack * mockedCmEditChannel.send(mockedEvent, { eventConfiguration ->
                    eventConfiguration.eventProperties.get('application') == 'CRUD_STRONG' })
        and: ' process request'
            processRequest * mockedCmNbiCommandExecutor.processCommandRequest(requestId,nbiCrudGetRequest)
        where: ''
            dbAvailable         |   retryNumber     ||  sendBack    |  processRequest
            true            |       0           ||      0       |       1
            true            |       21          ||      0       |       1
            false           |       0           ||      1       |       0
            false           |       3           ||      1       |       0
            false           |       4           ||      0       |       1
}

    def 'dummy test to force maven to run test class with all other test methods annotated with @Unroll'() {
        when:
        objUnderTest.toString()
        then:
        true
    }
}

