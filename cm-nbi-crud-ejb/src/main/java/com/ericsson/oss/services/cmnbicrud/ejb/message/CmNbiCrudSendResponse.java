/*
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 */
package com.ericsson.oss.services.cmnbicrud.ejb.message;

import com.ericsson.oss.itpf.sdk.eventbus.Channel;
import com.ericsson.oss.itpf.sdk.eventbus.Event;
import com.ericsson.oss.itpf.sdk.eventbus.EventConfiguration;
import com.ericsson.oss.itpf.sdk.eventbus.EventConfigurationBuilder;
import com.ericsson.oss.itpf.sdk.eventbus.annotation.Endpoint;
import com.ericsson.oss.presentation.cmnbirest.api.NbiResponse;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

/**
 * Created by enmadmin on 9/23/21.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class CmNbiCrudSendResponse {

        @Inject
        @Endpoint(value = "jms:/queue/scriptengine/output", timeToLive = 900000)
        private Channel scriptEngineOutputChannel;

        public void sendMessage(final String requestId, final NbiResponse nbiResponse) {

            final EventConfigurationBuilder eventConfigurationBuilder = new EventConfigurationBuilder();
            EventConfiguration eventConfiguration = eventConfigurationBuilder.addEventProperty("skipCache", "true").build();
            final Event event = scriptEngineOutputChannel.createEvent(nbiResponse, requestId);
            scriptEngineOutputChannel.send(event, eventConfiguration);
        }
}
