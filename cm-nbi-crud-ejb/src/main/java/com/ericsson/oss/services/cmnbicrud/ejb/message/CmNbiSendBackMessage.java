/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.cmnbicrud.ejb.message;

import com.ericsson.oss.itpf.sdk.eventbus.Channel;
import com.ericsson.oss.itpf.sdk.eventbus.Event;
import com.ericsson.oss.itpf.sdk.eventbus.EventConfiguration;
import com.ericsson.oss.itpf.sdk.eventbus.EventConfigurationBuilder;
import com.ericsson.oss.itpf.sdk.eventbus.annotation.Endpoint;
import com.ericsson.oss.presentation.cmnbirest.api.NbiRequest;
import com.ericsson.oss.presentation.cmnbirest.api.NbiRestApplication;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

/**
 * Created by enmadmin on 9/23/21.
 */

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class CmNbiSendBackMessage {


    @Inject
    @Endpoint(value = "jms:/queue/scriptengine/request", timeToLive = 60000)
    private Channel scriptengineRequestChannel;

    @Inject
    Logger logger;

    public void sendBackMessageAndIncrementRetryNumber(final NbiRequest nbiRequest, final NbiRestApplication applicationId) {
        final int retryNumber = nbiRequest.getRetryNumber();
        nbiRequest.setRetryNumber(retryNumber+1);
        waitBeforeSendBackCommand();
        final EventConfigurationBuilder eventConfigurationBuilder = new EventConfigurationBuilder();
        eventConfigurationBuilder.addEventProperty("application", applicationId.getName());
        final EventConfiguration additionalMessageProperties = eventConfigurationBuilder.build();

        final Event event = scriptengineRequestChannel.createEvent(nbiRequest, nbiRequest.getRequestId());
        scriptengineRequestChannel.send(event,additionalMessageProperties);
    }

    private void waitBeforeSendBackCommand() {
        try {
            Thread.sleep(100L);
        } catch (final Exception e) {
            logger.error("sendBackMessageAndIncrementRetryNumber unpected exception on sleep message={}",e.getMessage());
        }
    }
}
