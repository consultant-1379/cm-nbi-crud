/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.cmnbicrud.ejb.instrumentation;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.LocalBean;
import javax.ejb.Startup;
import javax.ejb.TimerService;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.inject.Inject;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.TimerConfig;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

@Singleton
@LocalBean
@Startup
public class InstrumentationTimerBean {

    @Resource
    private TimerService timerService;

    @Inject
    private MeasureInstrumentation measureInstrumentation;

    @Inject
    private Logger logger;

    @PostConstruct
    public void setupTimer() {
        logger.debug("InstrumentationTimerBean CRUD STARTED");
        setFreezeTimer();
    }

    @Timeout
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @Lock(LockType.READ)
    public void freezeMeasures(final Timer timer) {
        final String GET_BASE_ONLY = "GET_BASE_ONLY";
        final String GET_BASE_OTHER_ALL = "GET_BASE_OTHER_ALL";
        final String PUT_MODIFY = "PUT_MODIFY";
        final String PUT_CREATE = "PUT_CREATE";
        final String POST = "POST";
        final String DELETE = "DELETE";
        final String PATCH_JSON_PATCH = "PATCH_JSON_PATCH";
        final String PATCH_3GPP_JSON_PATCH = "PATCH_3GPP_JSON_PATCH";

        measureInstrumentation.freezeMeasureData(GET_BASE_ONLY);
        measureInstrumentation.freezeMeasureData(GET_BASE_OTHER_ALL);
        measureInstrumentation.freezeMeasureData(PUT_MODIFY);
        measureInstrumentation.freezeMeasureData(PUT_CREATE);
        measureInstrumentation.freezeMeasureData(POST);
        measureInstrumentation.freezeMeasureData(DELETE);
        measureInstrumentation.freezeMeasureData(PATCH_JSON_PATCH);
        measureInstrumentation.freezeMeasureData(PATCH_3GPP_JSON_PATCH);

        if (logger.isDebugEnabled()) {
            printFrozenCounters(GET_BASE_ONLY);
            printFrozenCounters(GET_BASE_OTHER_ALL);
            printFrozenCounters(PUT_MODIFY);
            printFrozenCounters(PUT_CREATE);
            printFrozenCounters(POST);
            printFrozenCounters(DELETE);
            printFrozenCounters(PATCH_JSON_PATCH);
            printFrozenCounters(PATCH_3GPP_JSON_PATCH);
        }
        setFreezeTimer();
    }

    private void printFrozenCounters(final String requestType) {
        logger.debug("Freezing CRUD requestType={}  MaxExecutionTime={}  AverageExecutionTime={}",
                requestType,
                measureInstrumentation.getSavedMaxExecutionTime(requestType),
                measureInstrumentation.getSavedAverageExecutionTime(requestType));
    }

    private void setFreezeTimer() {
        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(false);
        timerService.createSingleActionTimer(TimeUnit.MINUTES.toMillis(1), timerConfig);
    }
}
