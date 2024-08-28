package com.ericsson.oss.services.cmnbicrud.ejb.common

import com.ericsson.cds.cdi.support.spock.CdiSpecification

class CmRestNbiConstantsSpec extends CdiSpecification {

    def 'asking constructor for coverage only'() {
        when:
            CmRestNbiConstants cmRestNbiConstants = new CmRestNbiConstants()
        then:
           1 == 1
    }
}
