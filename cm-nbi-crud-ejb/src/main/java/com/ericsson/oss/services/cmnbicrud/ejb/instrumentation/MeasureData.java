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
package com.ericsson.oss.services.cmnbicrud.ejb.instrumentation;

/**
 * Created by enmadmin on 11/24/21.
 */
public class MeasureData {
    private long intervalTotalTime;
    private long intervalOperationNumber;
    private long intervalMaxTime;
    private long savedIntervalAvgTime;
    private long savedIntervalMaxTime;

    public MeasureData() {
        intervalTotalTime = 0;
        intervalOperationNumber = 0;
        intervalMaxTime = 0;
        savedIntervalAvgTime = 0;
        savedIntervalMaxTime = 0;
    }

    public void incrementIntervalTotalTime(final long incrementValue) {
        intervalTotalTime += incrementValue;
        intervalOperationNumber+=1;
        if (incrementValue > intervalMaxTime) {
            intervalMaxTime = incrementValue;
        }
    }

    public void freezeIntervalAvgTime() {
        long value = 0;
        if (intervalOperationNumber != 0) {
            value = intervalTotalTime / intervalOperationNumber;
        }
        intervalOperationNumber = 0;
        intervalTotalTime = 0;

        savedIntervalAvgTime = value;
    }

    public void freezeIntervalMaxTime() {
        savedIntervalMaxTime = intervalMaxTime;
        intervalMaxTime = 0;
    }

    public long getSavedIntervalAvgTime() {
        return savedIntervalAvgTime;
    }

    public long getSavedIntervalMaxTime() {
        return savedIntervalMaxTime;
    }
}
