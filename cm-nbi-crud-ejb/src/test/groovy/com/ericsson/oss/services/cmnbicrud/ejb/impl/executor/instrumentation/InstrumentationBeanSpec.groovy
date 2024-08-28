package com.ericsson.oss.services.cmnbicrud.ejb.impl.executor.instrumentation

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.cmnbicrud.ejb.instrumentation.InstrumentationBean
import com.ericsson.oss.services.cmnbicrud.ejb.instrumentation.InstrumentationTimerBean

import javax.inject.Inject

class InstrumentationBeanSpec extends CdiSpecification {

    @ObjectUnderTest
    InstrumentationBean instrumentationBean;

    @Inject
    InstrumentationTimerBean instrumentationTimerBean;

    def 'measures have a consistent value after start and stop'() {
        given:
        def measureType = "GET_BASE_ONLY"
        def startTime = instrumentationBean.startInstrumentationMeasure()
        and: 'some time elapses'
        Thread.sleep(100L);
        when: 'measure time is stopped'
        instrumentationBean.stopInstrumentationMeasure(measureType, startTime)
        instrumentationTimerBean.freezeMeasures()
        def avg = instrumentationBean.getGetBaseOnlyExecAvgTime()
        def max = instrumentationBean.getGetBaseOnlyExecMaxTime()
        then:
        avg > 0
        max > 0
    }

    def 'measures have a consistent value after double start and stop'() {
        given:
        def measureType = "GET_BASE_OTHER_ALL"
        def startTime = instrumentationBean.startInstrumentationMeasure()
        and: 'some time elapses'
        Thread.sleep(100L);
        when: 'measure time is stopped'
        instrumentationBean.stopInstrumentationMeasure(measureType, startTime)
        instrumentationTimerBean.freezeMeasures()
        then:
        def max1 = instrumentationBean.getGetBaseOtherAllExecMaxTime()
        max1 > 0
        and: 'measure time is stopped and started again'
        def startTime2 = instrumentationBean.startInstrumentationMeasure()
        and: 'some time elapses again'
        Thread.sleep(200L);
        when: 'measure time is stopped again'
        instrumentationBean.stopInstrumentationMeasure(measureType, startTime2)
        instrumentationTimerBean.freezeMeasures()
        def avg2 = instrumentationBean.getGetBaseOtherAllExecAvgTime()
        def max2 = instrumentationBean.getGetBaseOtherAllExecMaxTime()
        then:
        avg2 > 0
        max2 > 0
    }
}
