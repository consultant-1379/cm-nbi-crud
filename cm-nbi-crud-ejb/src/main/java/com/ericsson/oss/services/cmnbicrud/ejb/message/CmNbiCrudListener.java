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

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.presentation.cmnbirest.api.NbiRequest;
import com.ericsson.oss.presentation.cmnbirest.api.NbiRestApplication;

import com.ericsson.oss.services.cm.cmreader.api.CmReaderService;
import com.ericsson.oss.services.cmnbicrud.ejb.impl.executor.CmNbiCommandExecutor;
import org.slf4j.Logger;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.eventbus.Event;
import com.ericsson.oss.itpf.sdk.eventbus.annotation.Consumes;


@ApplicationScoped
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class CmNbiCrudListener {

    private static final int MAX_UNDEFINED_RETRY_NUMBER = 10;
    private static final String SDK_EVENTBUS_JMS_PATH = "sdk.eventbus.jms.concurrent.queue.listeners.number.request.";
    @Inject
    private Logger logger;

    @Inject
    CmNbiCommandExecutor cmNbiCommandExecutor;

    @Inject
    private CmNbiSendBackMessage cmNbiSendBackMessage;

    @EServiceRef
    private CmReaderService cmReaderService;

    public void receiveRequest(@Observes @Consumes(endpoint = "jms:queue/scriptengine/request",
            filter="application ='CRUD'") final Event event ) {
        requestProcess(event,NbiRestApplication.CRUD);
        }

    public void receiveStrongRequest(@Observes @Consumes(endpoint = "jms:queue/scriptengine/request",
            filter="application ='CRUD_STRONG'") final Event event ) {
        requestProcess(event,NbiRestApplication.CRUD_STRONG);
    }

    public void receiveMediumRequest(@Observes @Consumes(endpoint = "jms:queue/scriptengine/request",
            filter="application ='CRUD_MEDIUM'") final Event event ) {
        requestProcess(event,NbiRestApplication.CRUD_MEDIUM);
    }

    private void requestProcess(final Event event, final NbiRestApplication nbiRestApplication) {
        final String requestId = event.getCorrelationId();
        final NbiRequest nbiRequest = (NbiRequest) event.getPayload();
        if (checkIfRequestCanBeProcessed(requestId, nbiRequest.getRetryNumber(), nbiRestApplication)) {
            logger.debug("RequestListener:: selector={}  received operation requestId={} nbiRequest={}",nbiRestApplication.getName(),requestId, nbiRequest);
            cmNbiCommandExecutor.processCommandRequest(requestId, nbiRequest);
        } else {
            cmNbiSendBackMessage.sendBackMessageAndIncrementRetryNumber(nbiRequest,nbiRestApplication);
        }

    }

    private boolean checkIfRequestCanBeProcessed(final String requestId, final int retryNumber, final NbiRestApplication nbiRestApplication ) {
        if (!cmReaderService.checkIfcommandCanBeProcessed(requestId) && retryNumber < getMaxRetryNumber(nbiRestApplication)) {
            logger.warn("cm-nbi-crud, request with requestID {} sent back due to internal resources are not yet available retryNumber={} maxRetry = {}",requestId, retryNumber, getMaxRetryNumber(nbiRestApplication));
            return false;
        }
        return true;
    }

    private int getMaxRetryNumber(final NbiRestApplication nbiRestApplication) {
        final String propertyName = SDK_EVENTBUS_JMS_PATH + nbiRestApplication.getName();
        final String propertyValue = System.getProperty(propertyName);
        int maxRetryNumber = MAX_UNDEFINED_RETRY_NUMBER;
        if (propertyValue != null) {
            maxRetryNumber = Integer.valueOf(propertyValue);
        }
        return maxRetryNumber + 1;
    }
}

