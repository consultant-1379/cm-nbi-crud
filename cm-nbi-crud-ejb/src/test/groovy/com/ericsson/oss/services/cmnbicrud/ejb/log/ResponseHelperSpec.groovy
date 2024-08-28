package com.ericsson.oss.services.cmnbicrud.ejb.log

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification

class ResponseHelperSpec extends CdiSpecification {

    @ObjectUnderTest
    ResponseHelper objUnderTest;

    def 'Verify printStackInfo method for coverage_only'() {
        when: 'printStackInfo is invoked'
            objUnderTest.printStackInfo(new Exception("error"))
        then:
         objUnderTest.toString()
    }
}
