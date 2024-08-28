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

import com.ericsson.oss.itpf.sdk.instrument.annotation.InstrumentedBean;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute;
import com.ericsson.oss.itpf.sdk.instrument.annotation.MonitoredAttribute.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@InstrumentedBean(description = "CM NBI CRUD Operation", displayName = "CM NBI CRUD")
@ApplicationScoped
public class InstrumentationBean {

    public static final String GET_BASE_ONLY = "GET_BASE_ONLY";
    public static final String GET_BASE_OTHER_ALL = "GET_BASE_OTHER_ALL";
    public static final String PUT_MODIFY = "PUT_MODIFY";
    public static final String PUT_CREATE = "PUT_CREATE";
    public static final String POST = "POST";
    public static final String DELETE = "DELETE";
    public static final String PATCH_JSON_PATCH = "PATCH_JSON_PATCH";
    public static final String PATCH_3GPP_JSON_PATCH = "PATCH_3GPP_JSON_PATCH";
    public static final String UNKNOWN = "UNKNOWN";

    @Inject
    private MeasureInstrumentation measureInstrumentation;

    public long startInstrumentationMeasure() {
        return System.currentTimeMillis();
    }

    public void stopInstrumentationMeasure(final String requestType, final long startTimeMeasure) {
        measureInstrumentation.stopInstrumentationMeasure(requestType,startTimeMeasure);
    }

    /*
     * GET_BASE_ONLY
     */
    @MonitoredAttribute(visibility = Visibility.ALL,
            units = Units.MILLISECONDS,
            category = Category.PERFORMANCE,
            interval = Interval.ONE_MIN,
            collectionType = CollectionType.DYNAMIC)
    public long getGetBaseOnlyExecAvgTime() {
        return measureInstrumentation.getSavedAverageExecutionTime(GET_BASE_ONLY);
    }

    @MonitoredAttribute(visibility = Visibility.ALL,
            units = Units.MILLISECONDS,
            category = Category.PERFORMANCE,
            interval = Interval.ONE_MIN,
            collectionType = CollectionType.DYNAMIC)
    public long getGetBaseOnlyExecMaxTime() {
        return measureInstrumentation.getSavedMaxExecutionTime(GET_BASE_ONLY);
    }

    /*
     * GET_BASE_OTHER_ALL
     */
    @MonitoredAttribute(visibility = Visibility.ALL,
            units = Units.MILLISECONDS,
            category = Category.PERFORMANCE,
            interval = Interval.ONE_MIN,
            collectionType = CollectionType.DYNAMIC)
    public long getGetBaseOtherAllExecAvgTime() {
        return measureInstrumentation.getSavedAverageExecutionTime(GET_BASE_OTHER_ALL);
    }

    @MonitoredAttribute(visibility = Visibility.ALL,
            units = Units.MILLISECONDS,
            category = Category.PERFORMANCE,
            interval = Interval.ONE_MIN,
            collectionType = CollectionType.DYNAMIC)
    public long getGetBaseOtherAllExecMaxTime() {
        return measureInstrumentation.getSavedMaxExecutionTime(GET_BASE_OTHER_ALL);
    }

    /*
     * PUT_MODIFY
     */
    @MonitoredAttribute(visibility = Visibility.ALL,
            units = Units.MILLISECONDS,
            category = Category.PERFORMANCE,
            interval = Interval.ONE_MIN,
            collectionType = CollectionType.DYNAMIC)
    public long getPutModifyExecAvgTime() {
        return measureInstrumentation.getSavedAverageExecutionTime(PUT_MODIFY);
    }

    @MonitoredAttribute(visibility = Visibility.ALL,
            units = Units.MILLISECONDS,
            category = Category.PERFORMANCE,
            interval = Interval.ONE_MIN,
            collectionType = CollectionType.DYNAMIC)
    public long getPutModifyExecMaxTime() {
        return measureInstrumentation.getSavedMaxExecutionTime(PUT_MODIFY);
    }

    /*
     * PUT_CREATE
     */
    @MonitoredAttribute(visibility = Visibility.ALL,
            units = Units.MILLISECONDS,
            category = Category.PERFORMANCE,
            interval = Interval.ONE_MIN,
            collectionType = CollectionType.DYNAMIC)
    public long getPutCreateExecAvgTime() {
        return measureInstrumentation.getSavedAverageExecutionTime(PUT_CREATE);
    }

    @MonitoredAttribute(visibility = Visibility.ALL,
            units = Units.MILLISECONDS,
            category = Category.PERFORMANCE,
            interval = Interval.ONE_MIN,
            collectionType = CollectionType.DYNAMIC)
    public long getPutCreateExecMaxTime() {
        return measureInstrumentation.getSavedMaxExecutionTime(PUT_CREATE);
    }

    /*
     * POST
     */
    @MonitoredAttribute(visibility = Visibility.ALL,
            units = Units.MILLISECONDS,
            category = Category.PERFORMANCE,
            interval = Interval.ONE_MIN,
            collectionType = CollectionType.DYNAMIC)
    public long getPostExecAvgTime() {
        return measureInstrumentation.getSavedAverageExecutionTime(POST);
    }

    @MonitoredAttribute(visibility = Visibility.ALL,
            units = Units.MILLISECONDS,
            category = Category.PERFORMANCE,
            interval = Interval.ONE_MIN,
            collectionType = CollectionType.DYNAMIC)
    public long getPostExecMaxTime() {
        return measureInstrumentation.getSavedMaxExecutionTime(POST);
    }

    /*
     * DELETE
     */
    @MonitoredAttribute(visibility = Visibility.ALL,
            units = Units.MILLISECONDS,
            category = Category.PERFORMANCE,
            interval = Interval.ONE_MIN,
            collectionType = CollectionType.DYNAMIC)
    public long getDeleteExecAvgTime() {
        return measureInstrumentation.getSavedAverageExecutionTime(DELETE);
    }

    @MonitoredAttribute(visibility = Visibility.ALL,
            units = Units.MILLISECONDS,
            category = Category.PERFORMANCE,
            interval = Interval.ONE_MIN,
            collectionType = CollectionType.DYNAMIC)
    public long getDeleteExecMaxTime() {
        return measureInstrumentation.getSavedMaxExecutionTime(DELETE);
    }

    /*
     * PATCH_JSON_PATCH
     */
    @MonitoredAttribute(visibility = Visibility.ALL,
            units = Units.MILLISECONDS,
            category = Category.PERFORMANCE,
            interval = Interval.ONE_MIN,
            collectionType = CollectionType.DYNAMIC)
    public long getPatchJPatchExecAvgTime() {
        return measureInstrumentation.getSavedAverageExecutionTime(PATCH_JSON_PATCH);
    }

    @MonitoredAttribute(visibility = Visibility.ALL,
            units = Units.MILLISECONDS,
            category = Category.PERFORMANCE,
            interval = Interval.ONE_MIN,
            collectionType = CollectionType.DYNAMIC)
    public long getPatchJPatchExecMaxTime() {
        return measureInstrumentation.getSavedMaxExecutionTime(PATCH_JSON_PATCH);
    }

    /*
     * PATCH_3GPP_JSON_PATCH
     */
    @MonitoredAttribute(visibility = Visibility.ALL,
            units = Units.MILLISECONDS,
            category = Category.PERFORMANCE,
            interval = Interval.ONE_MIN,
            collectionType = CollectionType.DYNAMIC)
    public long getPatch3gppJPatchExecMaxTime() {
        return measureInstrumentation.getSavedMaxExecutionTime(PATCH_3GPP_JSON_PATCH);
    }

    @MonitoredAttribute(visibility = Visibility.ALL,
            units = Units.MILLISECONDS,
            category = Category.PERFORMANCE,
            interval = Interval.ONE_MIN,
            collectionType = CollectionType.DYNAMIC)
    public long getPatch3gppJPatchExecAvgTime() {
        return measureInstrumentation.getSavedAverageExecutionTime(PATCH_3GPP_JSON_PATCH);
    }

}
