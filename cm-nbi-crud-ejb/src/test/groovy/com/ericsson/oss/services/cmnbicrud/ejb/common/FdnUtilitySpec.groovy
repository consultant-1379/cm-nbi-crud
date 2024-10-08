package com.ericsson.oss.services.cmnbicrud.ejb.common

import com.ericsson.cds.cdi.support.spock.CdiSpecification
import spock.lang.Unroll

class FdnUtilitySpec extends CdiSpecification {

    @Unroll
    def 'Verify extractTypeFromFdn method'() {
        when: 'extractTypeFromFdn is invoked'
        def type = FdnUtility.extractTypeFromFdn(fdn)
        then:
        type == expectedType

        where: 'The given parameters'
        fdn                                        | expectedType        | _
        'MeContext=ERBS002,ManagedElement=1'   | 'ManagedElement'  | _
        'MeContext=ERBS002'                      | 'MeContext'        | _
    }

    @Unroll
    def 'Verify extractTypeFromFdn method throws exception'() {
        when: 'extractTypeFromFdn is invoked'
        def type = FdnUtility.extractTypeFromFdn(fdn)

        then: 'exception is thrown'
        def exception = thrown(Exception)

        where: 'The given parameters'
        fdn   |_
        null | _
        ''   |_
    }

    @Unroll
    def 'Verify extractNameFromFdn method'() {
        when: 'extractNameFromFdn is invoked'
        def name = FdnUtility.extractNameFromFdn(fdn)
        then:
        name == expectedName

        where: 'The given parameters'
        fdn                                        | expectedName       | _
        null                                      | null               | _
        ''                                         | ''                | _
        'MeContext=ERBS002,ManagedElement=1'   | '1'               | _
        'MeContext=ERBS002'                      | 'ERBS002'        | _
    }

    @Unroll
    def 'Verify getParentFdn method'() {
        when: 'getParentFdn is invoked'
        def returnedFdn = FdnUtility.getParentFdn(fdn)
        then:
        returnedFdn == expectedFdn

        where: 'The given parameters'
        fdn                                        | expectedFdn       | _
        null                                      | null               | _
        ''                                         | null                | _
        'MeContext=ERBS002,ManagedElement=1'   | 'MeContext=ERBS002' | _
        'MeContext=ERBS002'                      | null        | _
    }

    @Unroll
    def 'Verify fdnToBeRemoved method'() {
        when: 'fdnToBeRemoved is invoked'
        def fdnToBeRemoved = FdnUtility.fdnToBeRemoved(fdn)
        then:
        fdnToBeRemoved == expectedFdn

        where: 'The given parameters'
        fdn             |  expectedFdn | _
        '' | '' | _
        'MeContext=ERBS002' | '' | _
        'MeContext=ERBS002,ManagedElement=1' | 'MeContext=ERBS002,' | _
        'MeContext=ERBS002,ManagedElement=1,ENodeBFunction=1' | 'MeContext=ERBS002,ManagedElement=1,' | _
    }

    @Unroll
    def 'Verify purgedFdn method'() {
        when: 'fdnToBeRemoved is invoked'
        def purgedFdn = FdnUtility.purgedFdn(fdn, fdnToBeRemoved)
        then:
        purgedFdn == expectedFdn

        where: 'The given parameters'
        fdn             | fdnToBeRemoved | expectedFdn | _
        '' | '' | '' | _
        'MeContext=ERBS002' | 'MeContext=ERBS002,' | 'MeContext=ERBS002' |_
        'MeContext=ERBS002,ManagedElement=1' | 'MeContext=ERBS002,' | 'ManagedElement=1' | _
        'MeContext=ERBS002,ManagedElement=1,ENodeBFunction=1' | 'MeContext=ERBS002,' | 'ManagedElement=1,ENodeBFunction=1' | _
    }

    def 'asking constructor throws Exception'() {
        when:
            FdnUtility fdnUtility = new FdnUtility()
        then: 'exception is thrown'
        def exception = thrown(Exception)
    }

    @Unroll
    def 'Verify getTxStatus method'() {
        when: 'getTxStatus is invoked'
        def returnedStatus = FdnUtility.getTxStatus(status)

        then: 'exception is thrown'
        returnedStatus == expectedStatus

        where: 'The given parameters'
        status   | expectedStatus            | _
        0    | 'STATUS_ACTIVE'             | _
        1    | 'STATUS_MARKED_ROLLBACK'   | _
        2    | 'STATUS_PREPARED'           | _
        3    | 'STATUS_COMMITTED'          | _
        4    | 'STATUS_ROLLEDBACK'         | _
        5    | 'STATUS_UNKNOWN'             | _
        6    | 'STATUS_NO_TRANSACTION'     | _
        7    | 'STATUS_PREPARING'           | _
        8    | 'STATUS_COMMITTING'          | _
        9    | 'STATUS_ROLLING_BACK'        | _
        100  | 'STATUS_UNKNOWN2'            | _
    }

    @Unroll
    def 'Verify getUnscopedType method for coverage_only'() {
        when:
        String ret = FdnUtility.getUnscopedType(type)
        then:
        ret == expectedType

        where:
        type                       | expectedType           | _
        ''                         | ''                     | _
        'interfaces'              | 'interfaces'          | _
        'interfaces$$interface'  | 'interface'          | _
        'interfaces$$aa$$bb'     | 'bb'                  | _
    }

    def 'dummy test to force maven to run test class with all other test methods annotated with @Unroll'() {
    when:
    "".toString()
    then:
    1 == 1
    }

}
