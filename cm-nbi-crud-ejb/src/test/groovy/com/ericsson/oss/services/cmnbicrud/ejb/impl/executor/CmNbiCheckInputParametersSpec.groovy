package com.ericsson.oss.services.cmnbicrud.ejb.impl.executor

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.cm.cmshared.dto.search.CmSearchScope
import com.ericsson.oss.services.cmnbicrud.ejb.common.ScopeType
import com.ericsson.oss.services.cmnbicrud.ejb.exceptions.CmRestNbiValidationException
import spock.lang.Unroll

class CmNbiCheckInputParametersSpec extends CdiSpecification {
    @ObjectUnderTest
    private CmNbiCheckInputParameters objUnderTest

    static Set<String> emptySet = new HashSet()
    static Set<String> mccSet = new HashSet()
    static Set<String> mccMncSet = new HashSet()

    static {
        mccSet.add("mcc")
        mccMncSet.add("mcc")
        mccMncSet.add("mnc")
    }


    @Unroll
    def 'Verify getAttributesAndFields method'() {
        when: 'getAttributesAndFields is invoked'
        def map = objUnderTest.getAttributesAndFields(attributes, fields)
        then:
        map == expectedMap

        where: 'The given parameters'
        attributes    | fields                           |  expectedMap                       | _
        // null left
        null          | null                           |  null                            | _
        null          | ''                             |   [:]                            | _
        null          | 'attributes/eNodeBPlmnId'    |  ['eNodeBPlmnId':emptySet]     | _

        // null right
        null          | null                           |  null       | _
        ''            | null                           |  [:]        | _
        'userLabel'  | null                          |  ['userLabel':emptySet]        | _


        //'' left
        ''          | null                           |  [:]                            | _
        ''          | ''                             |   [:]                            | _
        ''          | 'attributes/eNodeBPlmnId'   |  ['eNodeBPlmnId':emptySet]     | _

        //'' right
        null          | ''                           |  [:]       | _
        ''            | ''                           |  [:]        | _
        'userLabel'   | ''                          |  ['userLabel':emptySet]        | _

        // parameter left
        'userLabel'   | null                          |  ['userLabel':emptySet]        | _
        'userLabel'   | ''                          |  ['userLabel':emptySet]        | _
        'userLabel'  | 'attributes/eNodeBPlmnId/mcc,attributes/eNodeBPlmnId/mnc'      |  ['userLabel': emptySet, 'eNodeBPlmnId':mccMncSet]        | _

        // parameter right
        null  | 'attributes/eNodeBPlmnId/mcc,attributes/eNodeBPlmnId/mnc'      |  ['eNodeBPlmnId':mccMncSet]        | _
        ''  | 'attributes/eNodeBPlmnId/mcc,attributes/eNodeBPlmnId/mnc'      |  ['eNodeBPlmnId':mccMncSet]        | _
        'userLabel'  | 'attributes/eNodeBPlmnId/mcc,attributes/eNodeBPlmnId/mnc'      |  ['userLabel': emptySet, 'eNodeBPlmnId':mccMncSet]        | _

        // mixed
        ''                 | 'attributes/eNodeBPlmnId'                                          |  ['eNodeBPlmnId':emptySet]     | _
        ''                 | 'attributes/eNodeBPlmnId/mcc'                                      |  ['eNodeBPlmnId':mccSet]        | _
        'userLabel'       | 'attributes/eNodeBPlmnId/mcc,attributes/eNodeBPlmnId/mnc'      |  ['userLabel': emptySet, 'eNodeBPlmnId':mccMncSet]        | _
        'userLabel'       | 'attributes/eNodeBPlmnId/mcc,attributes/eNodeBPlmnId'          |  ['userLabel': emptySet, 'eNodeBPlmnId':emptySet]        | _
        'userLabel,other'   | 'attributes/eNodeBPlmnId/mcc,attributes/eNodeBPlmnId'          |  ['userLabel': emptySet, 'eNodeBPlmnId':emptySet, 'other':emptySet]        | _
    }


    @Unroll
    def 'Verify getAttributesAndFields with invalid fields parameter throws  CmRestNbiValidationException'() {
        when: 'getAttributesAndFields is invoked'
        def map = objUnderTest.getAttributesAndFields(attributes, fields)
        then:
        def cmValidationException = thrown(CmRestNbiValidationException)
        cmValidationException.message.contains("The supplied parameters fields")

        where: 'The given parameters'
        attributes    | fields                        |  _
        null          | 'neType'                    | _
        'userLabel'  | 'neType'                    | _
        ''            | 'neType'                    | _
        'userLabel'  | 'attrib/eNodeBPlmnId' | _
        'userLabel'   | 'attrib/eNodeBPlmnId/mcc,attributes/eNodeBPlmnId/mnc'      | _
    }

    @Unroll
    def 'Verify getScopeType method return valid ScopeType'() {
        when: 'getScopeType is invoked'
        def scopeType = objUnderTest.getScopeType(scope)
        then:
        scopeType == expectedScopeType

        where: 'The given parameters'
        scope              | expectedScopeType | _
        null               | ScopeType.BASE_ONLY | _
        "BASE_ONLY"       | ScopeType.BASE_ONLY | _
        "BASE_ALL"         | ScopeType.BASE_ALL | _
        "BASE_SUBTREE"    | ScopeType.BASE_SUBTREE | _
        "BASE_NTH_LEVEL"   | ScopeType.BASE_NTH_LEVEL | _
    }

    @Unroll
    def 'Verify getScopeType method with invalid scope throws CmRestNbiValidationException'() {
        when: 'getScopeType is invoked'
        def scopeType = objUnderTest.getScopeType(scope)
        then:
        def cmValidationException = thrown(CmRestNbiValidationException)
        cmValidationException.message.contains("The supplied parameters scopeType")

        where: 'The given parameters'
        scope              | _
        ""                | _
        "invalid"        | _
    }


    @Unroll
    def 'Verify getActualFdnFrom3gppJsonPatch throws CmRestNbiValidationException'() {
        when: 'getActualFdnFrom3gppJsonPatch is invoked'
        objUnderTest.getActualFdnFrom3gppJsonPatch(xpath , path);
        then:
        def cmValidationException = thrown(CmRestNbiValidationException)
        cmValidationException.message.contains("The supplied parameters xpath=null and path=="+path+" is not allowed.")

        where: 'The given parameters'
        xpath               | path      | _
        null                | null     | _
        null                | ''        | _
    }

    @Unroll
    def 'Verify getActualFdnFrom3gppJsonPatch'() {
        when: 'getActualFdnFrom3gppJsonPatch is invoked'
        def fdn = objUnderTest.getActualFdnFrom3gppJsonPatch(xpath , path)
        then:
        fdn == expectedFdn

        where: 'The given parameters'
        xpath               | path               | expectedFdn          | _
        null                | 'A=1/B=2'         | 'A=1,B=2'          | _
        'A=1/B=2'          | null               | 'A=1,B=2'          | _
        'A=1/B=2'          | ''               | 'A=1,B=2'          | _
        'A=1/B=2'          | 'A=1/B=2'         | 'A=1,B=2'          | _
        'A=1/B=2'          | 'D=1/B=2'         | 'A=1,B=2,D=1,B=2'  | _
        null                | '/A=1/B=2'         | 'A=1,B=2'          | _
        'A=1/B=2'          | '/A=1/B=2'         | 'A=1,B=2'          | _
        'A=1/B=2'          | '/D=1/B=2'         | 'A=1,B=2,D=1,B=2'  | _
        'A=1,B=2'          | '/D=1,B=2'         | 'A=1,B=2,D=1,B=2'  | _
    }

    @Unroll
    def 'Verify getCommonParent'() {
        when: 'getCommonParent is invoked'
        def commonParent = objUnderTest.getCommonParent(fdn , otherFdn)
        then:
        commonParent == expectedFdn

        where: 'The given parameters'
        fdn               | otherFdn               | expectedFdn         | _
        null                | null                | null               | _
        null                | 'A=1/B=2'          | null               | _
        'A=1,B=2'          | null                | null               | _
        'A=1,B=2'          | ''                  | ''                 | _
        'A=1,B=2'          | 'A=1,B=2'         | 'A=1,B=2'          | _
        'A=1,B=2'          | 'D=1,B=2'         | 'A=1,B=2'           | _
        'A=1,B=2'          | 'D=1    '         | 'A=1,B=2'           | _
        'A=1,B=2'          | 'A=1'              | 'A=1'           | _
        'A=1'              | 'A=1,B=2'         | 'A=1'          | _
    }

    @Unroll
    def 'Verify getLongestSon'() {
        when: 'getLongestSon is invoked'
        def commonParent = objUnderTest.getLongestSon(fdn , otherFdn)
        then:
        commonParent == expectedFdn

        where: 'The given parameters'
        fdn               | otherFdn               | expectedFdn         | _
        null                | null                | null               | _
        null                | 'A=1/B=2'          | 'A=1/B=2'         | _
        'A=1,B=2'          | null                | 'A=1,B=2'         | _
        'A=1,B=2'          | ''                  | 'A=1,B=2'          | _
        'A=1,B=2'          | 'A=1,B=2'          | 'A=1,B=2'          | _
        'A=1,B=2'          | 'D=1,C=2'          | 'A=1,B=2'           | _
        'A=1,B=2'          | 'D=1,B=2,C=3'      | 'D=1,B=2,C=3'           | _
    }

    @Unroll
    def 'Verify extractPathWithoutAttribute'() {
        when: 'extractPathWithoutAttribute is invoked'
        def fdn = objUnderTest.extractPathWithoutAttribute(path)
        then:
        fdn == expectedFdn

        where: 'The given parameters'
         path               | expectedFdn  | _
         'A=1/B=2'         | 'A=1/B=2'   | _
         null               | null        | _
         ''                  | ''          | _
         'A=1/B=2'          | 'A=1/B=2'  | _
        'A=1/B=2'                 | 'A=1/B=2'   | _
        'A=1/B=2#/attributes/a' | 'A=1/B=2'   | _
        'A=1/B=2#/attributes/a#aa' | 'A=1/B=2'   | _
        'attributes#/attributes/a#aa' | 'attributes'   | _
    }

    @Unroll
    def 'Verify extractAttribute3gppJsonPatch'() {
        when: 'extractAttribute3gppJsonPatch is invoked'
        def attribute = objUnderTest.extractAttribute3gppJsonPatch(path)
        then:
        attribute == expectedAttribute

        where: 'The given parameters'
        path               | expectedAttribute  | _
        null               | null        | _
        ''                  | null       | _
        'A=1/B=2'          | null       | _
        'A=1/B=2#/attributes/attr'    | 'attr'   | _
        'A=1/B=2#/attributes/attr/mcc'    | 'attr/mcc'   | _
        'A=1/B=2#attributes/attr'    | 'attr'   | _
        'A=1/B=2#attributes/attr/mcc'    | 'attr/mcc'   | _
        'A=1/B=2#/attributes/'    | null   | _
        'A=1/B=2#attributes/'    | null   | _
        'A=1/B=2#/attributes'    | null   | _
        'A=1/B=2#attributes'    | null   | _
    }

    @Unroll
    def 'Verify extractAttributeJsonPatch'() {
        when: 'extractAttributeJsonPatch is invoked'
        def attribute = objUnderTest.extractAttributeJsonPatch(path)
        then:
        attribute == expectedAttribute

        where: 'The given parameters'
        path               | expectedAttribute  | _
        null               | null        | _
        ''                  | null       | _
        'A=1/B=2'          | null       | _
        'A=1/B=2/attributes/attr'    | null   | _
        '/attributes/attr'    | 'attr'   | _
        '/attributes/attr/mcc'    | 'attr/mcc'   | _
        'attributes/attr'    | 'attr'   | _
        'attributes/attr/mcc'    | 'attr/mcc'   | _
        '/attributes/'    | null   | _
        'attributes/'    | null   | _
        '/attributes'    | null   | _
        'attributes'    | null   | _
    }

    @Unroll
    def 'Verify fromXpathOrPathToFdn'() {
        when: 'fromXpathOrPathToFdn is invoked'
        def fdn = objUnderTest.fromXpathOrPathToFdn(xpath)
        then:
        fdn == expectedFdn

        // Note: cmedit is based on:
        // Note: looking at cmedit allowed char for FDN we saw that:
        // BASIC_ID  : [a-zA-Z0-9_]([a-zA-Z0-9_] | HY)*;       => This is Mo
        // FDN_ID :    [a-zA-Z0-9 ?.!@#$%^& * /|_:-]  and  (when in quotes) with these additional chars  [ ] + < >  ( ) = \   => this is FDN value
        // So cmedit is able to manage as FDN value (FDN_ID) these extra chars   ?.!@#$%^&*/|_:-[ ]+<>()=\

        // As xpath we want to manage all possible chars (we expect that Model validation will check if they are valid or not  (Example: Constraint violation: value specified for attribute EUtranCellFDDId is not valid ... does not match pattern constraint '[]0-9A-Za-z\\[.!$%&':?@^_`{|}~ /()-]*'."))
        // So about xpath and values specified inside FDN_ID  (MO1=<FDN_ID>/MO2=<FDN_ID>)
        // 1) For REST URL perspective '?' '#' '%' '=' are special chars so they should be managed using escape if https://www.werockyourweb.com/url-escape-characters/ Please consider that '=' in real case shall not be inside FDN_ID
        // 2) For xpath definition '/' is separator of RDNs so it has to be managed as special case inside fromXpathOrPathToFdn
        // 3) For xpath definition '=' is separator of MO1=<FDN_ID> expression so it could not be managed if used inside FDN_ID (also using escape). We are pretty sure this is not a real case and model validation doesn't allow '=' inside FDN_ID

        where: 'The given parameters'
        xpath                               | expectedFdn  | _
        null                               | null        | _
       ''                                  | ''          | _
        'Mo'                               | 'Mo'          | _
        'A=1'                              | 'A=1'   | _
        'A=1/2/3'                         | 'A=1/2/3'   | _
        'A=1/B=2'                         | 'A=1,B=2'   | _
        'A=1/B=2/C=3'                    | 'A=1,B=2,C=3'   | _
        'MeContext_1=SPFRER60001_1' | 'MeContext_1=SPFRER60001_1'  |_
        'MeContext=SPFRER60001$$?.!@#$%^&*/|_' | 'MeContext=SPFRER60001$$?.!@#$%^&*/|_'  |_
        'MeContext=SPFRER60001$$?.!@#$%^&*/|_/A=1' | 'MeContext=SPFRER60001$$?.!@#$%^&*/|_,A=1'  |_
        'MeContext=SPFRER60001$$ ?.!@#$%^&*/|_[ ]+<>()' +'\\'+'/interfaces=1/interface=1/2' | 'MeContext=SPFRER60001$$ ?.!@#$%^&*/|_[ ]+<>()'+'\\'+',interfaces=1,interface=1/2'  |_
        'MeContext=SPFRER60001/ManagedElement=1/interfaces=1/interface=1/2' | 'MeContext=SPFRER60001,ManagedElement=1,interfaces=1,interface=1/2'  |_
        'MeContext=SPFRER60001' | 'MeContext=SPFRER60001'  |_
        'MeContext=SPFRER60001/ManagedElement=1/interfaces=1/2/3/4/interface=1/2' | 'MeContext=SPFRER60001,ManagedElement=1,interfaces=1/2/3/4,interface=1/2'  |_
        'MeContext=SPFRER60001/ManagedElement=1@2#3///4/interfaces=1/2/3/4/interface=1/2' | 'MeContext=SPFRER60001,ManagedElement=1@2#3///4,interfaces=1/2/3/4,interface=1/2'  |_
        'MeContext=SPFRER60001/ManagedElement=1@2#3///4/interfaces=1/2/3/4^&$$/interface=1/2' | 'MeContext=SPFRER60001,ManagedElement=1@2#3///4,interfaces=1/2/3/4^&$$,interface=1/2'  |_
        'MeContext=SPFRER60001/ManagedElement=1@2#3!!!///4/interfaces=1/2/3/4^&$$/interface=1/2' | 'MeContext=SPFRER60001,ManagedElement=1@2#3!!!///4,interfaces=1/2/3/4^&$$,interface=1/2'  |_
        'MeContext=SPFRER60001/ManagedElement=1@2#3!!!///4/interfaces=1/  2/3/4^&$$/interface=1  /2' | 'MeContext=SPFRER60001,ManagedElement=1@2#3!!!///4,interfaces=1/  2/3/4^&$$,interface=1  /2'  |_
        'MeContext=SPFRER60001/ManagedElement=1@2#3!!!///4/interfaces=1/  2/3/4^&$$/interface=1  /2' | 'MeContext=SPFRER60001,ManagedElement=1@2#3!!!///4,interfaces=1/  2/3/4^&$$,interface=1  /2'  |_

        //this for extractPathWithoutAttribute inside generateCmPutRequest3gppJsonPatch
        'MeContext=SPFRER60001/ManagedElement=1@2#3!!!///4/interfaces=1/2/3/4^&$$/interface=1/2/#attributes/userlabel' | 'MeContext=SPFRER60001,ManagedElement=1@2#3!!!///4,interfaces=1/2/3/4^&$$,interface=1/2/#attributes/userlabel'  |_
        'MeContext=SPFRER60001/ManagedElement=1@2#3!!!///4/interfaces=1/2/3/4^&$$/interface=1/2#attributes/userlabel' | 'MeContext=SPFRER60001,ManagedElement=1@2#3!!!///4,interfaces=1/2/3/4^&$$,interface=1/2#attributes/userlabel'  |_
    }

    def 'dummy test to force maven to run test class with all other test methods annotated with @Unroll'() {
    when:
    objUnderTest.toString()
    then:
    1 == 1
    }
}
