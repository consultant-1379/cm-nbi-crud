package com.ericsson.oss.services.cmnbicrud.ejb.impl.executor

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.cm.cmshared.dto.AutoAttributeList
import com.ericsson.oss.services.cm.cmshared.dto.CmOutputSpecification
import com.ericsson.oss.services.cm.cmshared.dto.search.CmSearchCriteria
import com.ericsson.oss.services.cmnbicrud.ejb.common.ScopeType
import com.ericsson.oss.services.cmnbicrud.ejb.get.CmGetRequest
import spock.lang.Unroll

class CmGetRequestParserSpec extends CdiSpecification {
    @ObjectUnderTest
    private CmGetRequestParser objUnderTest

    String userId = "user"
    String requestId = "crud:1234"
    Map<String, Set<String>> attributes
    String filter = null


//    ManagedElement    seleziona tutti I managedElement
//    /Subnetwork/ManagedElement         Prende tutti I ManagedElement che hanno 1 sola Subnetwork come padre.
//    //ManagedElement      Tutti i managedElement indipendentemente dalla loro posizione nell albero
//    ManagedElement/EnodebFunction    tutti gli EnodeBfunction  che hann come padre diretto managedElement
//    ManagedElement//EutrancelllFDD    tutti gli EutrancelllFDD    che hann come padre managedElement
//    '/SubNetwork/ManagedElement[attributes/vendorname="Company XY"] | /SubNetwork/ManagedElement[attributes/vendorname!="Company XY"]//EutrancellFdd' | _


    @Unroll
    def 'Execute createCmSearchCriteria return success'() {
        given:
            CmGetRequest cmGetRequest = new CmGetRequest("MeContext=ERBS002,ManagedElement=1", ScopeType.BASE_ONLY, 0, attributes, filter)

        when:
            CmSearchCriteria cmSearchCriteria = objUnderTest.createCmSearchCriteria(cmGetRequest)
        then:
             1 == 1

        where:
            filter             |  _
         //   'ManagedElement' | _
            '/Subnetwork/ManagedElement' | _
           // '//ManagedElement' | _
            'ManagedElement/EnodebFunction' | _
            'ManagedElement//EutrancelllFDD' | _
            'MeContext/ManagedElement/EnodebFunction[attributes/vendorname="Company XY"]' | _

    }

    @Unroll
    def 'Execute createCmOutputSpecifications with attributes=null return success'() {
        given:
        attributes = null
        CmGetRequest cmGetRequest = new CmGetRequest("MeContext=ERBS002,ManagedElement=1", ScopeType.BASE_ALL, 0, attributes, filter)
        CmSearchCriteria cmSearchCriteria = objUnderTest.createCmSearchCriteria(cmGetRequest);

        when:
        List<CmOutputSpecification> cmOutputSpecifications = objUnderTest.createCmOutputSpecifications(cmSearchCriteria, cmGetRequest)
        then:
        cmOutputSpecifications.size == 1
        CmOutputSpecification cmOutputSpecification = cmOutputSpecifications.iterator().next()
        cmOutputSpecification.attributeNames == null
        cmOutputSpecification.autoAttributeList == AutoAttributeList.PERSISTED_ATTRIBUTES
        cmOutputSpecification.type == outputMo

        where:
        filter                          | outputMo |_
        '/Subnetwork/ManagedElement'| 'Subnetwork' |_
        'ManagedElement/EnodebFunction' |  'EnodebFunction' |_
        'ManagedElement//EutrancelllFDD' |  'EutrancelllFDD' |_
        'MeContext/ManagedElement/EnodebFunction[attributes/vendorname="Company XY"]' |  'EnodebFunction' |_
        'MeContext/ManagedElement/EnodebFunction[attributes/vendorname="Company XY"]/EutrancellFDD' |  'EutrancellFDD' |_
        'MeContext/ManagedElement/EnodebFunction[attributes/vendorname="Company XY"]/EutrancellFDD/EutranCellRelation' |  'EutranCellRelation' |_
    }

    @Unroll
    def 'Execute createCmOutputSpecifications with attributes empty return success'() {
        given:
        attributes = new HashMap<>()
        CmGetRequest cmGetRequest = new CmGetRequest("MeContext=ERBS002,ManagedElement=1", ScopeType.BASE_ALL, 0, attributes, filter)
        CmSearchCriteria cmSearchCriteria = objUnderTest.createCmSearchCriteria(cmGetRequest);

        when:
        List<CmOutputSpecification> cmOutputSpecifications = objUnderTest.createCmOutputSpecifications(cmSearchCriteria, cmGetRequest)
        then:
        cmOutputSpecifications.size == 1
        CmOutputSpecification cmOutputSpecification = cmOutputSpecifications.iterator().next()
        cmOutputSpecification.attributeNames == []
        cmOutputSpecification.autoAttributeList == AutoAttributeList.USER_SPECIFIED_LIST
        cmOutputSpecification.type == outputMo

        where:
        filter                          | outputMo |_
        '/Subnetwork/ManagedElement'| 'Subnetwork' |_
        'ManagedElement/EnodebFunction' |  'EnodebFunction' |_
        'ManagedElement//EutrancelllFDD' |  'EutrancelllFDD' |_
        'MeContext/ManagedElement/EnodebFunction[attributes/vendorname="Company XY"]' |  'EnodebFunction' |_
        'MeContext/ManagedElement/EnodebFunction[attributes/vendorname="Company XY"]/EutrancellFDD' |  'EutrancellFDD' |_
        'MeContext/ManagedElement/EnodebFunction[attributes/vendorname="Company XY"]/EutrancellFDD/EutranCellRelation' |  'EutranCellRelation' |_
    }

    @Unroll
    def 'Execute createRegexForVerifyingFdnContainsAllSpecifiedParentsInCorrectOrder return success'() {
        when:
        def regex  = objUnderTest.createRegexForVerifyingFdnContainsAllSpecifiedParentsInCorrectOrder(filter)
        then:
        System.out.println(regex);
        1 == 1


        where:
        filter             |  _
        '/Subnetwork/ManagedElement' | _
        'ManagedElement/EnodebFunction' | _
        'ManagedElement//EutrancelllFDD' | _
        '/SubNetwork/ManagedElement[attributes/vendorname="Company XY"] | /SubNetwork/ManagedElement[attributes/vendorname!="Company XY"]//EutrancellFdd' | _
        'MeContext/ManagedElement/EnodebFunction[attributes/vendorname="Company XY"]' | _

        '/MeContext/ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'     | _
        '/MeContext/ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'     | _
        '//MeContext/ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]'     | _
        'MeContext//ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]'     | _
        '////MeContext///ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]'     | _

    }

    @Unroll
    def 'Execute isFdnValidAgainstRegexPattern return success'() {
        when:
        def ret  = objUnderTest.isFdnValidAgainstRegexPattern(fdn, objUnderTest.createRegexForVerifyingFdnContainsAllSpecifiedParentsInCorrectOrder(filter))
        then:
        ret == result

        where:
        fdn                  |     filter                       |  result | _
        'subnetwork=2,managedelement=2' | '/Subnetwork/ManagedElement' |    true      | _
        'subnetwork=2,subnetwork=2,managedelement=2' | '/Subnetwork/ManagedElement' |    false      | _
        'A=2,subnetwork=2,managedelement=2' | '/Subnetwork/ManagedElement' |    false      | _
        'subnetwork=2,managedelement=2' | '/Subnetwork/ManagedElement' |    true      | _
        'subnetwork=2,managedelement=2,enodebfunction=1' | '/Subnetwork/ManagedElement' |    false      | _


        'subnetwork=2,managedelement=2' | '/Subnetwork//ManagedElement' |    true      | _
        'subnetwork=2,subnetwork=2,A=2,managedelement=2' | '/Subnetwork//ManagedElement' |    true      | _
        'A=2,subnetwork=2,managedelement=2' | '/Subnetwork//ManagedElement' |    false      | _
        'subnetwork=2,managedelement=2' | '/Subnetwork//ManagedElement' |    true      | _
        'subnetwork=2,managedelement=2,enodebfunction=1' | '/Subnetwork//ManagedElement' |    false      | _


        'subnetwork=2,managedelement=2' | '//Subnetwork/ManagedElement' |    true      | _
        'subnetwork=2,subnetwork=2,managedelement=2' | '//Subnetwork/ManagedElement' |    true      | _
        'A=2,subnetwork=2,managedelement=2' | '//Subnetwork/ManagedElement' |    true      | _
        'subnetwork=2,managedelement=2' | '/Subnetwork/ManagedElement' |    true      | _
        'subnetwork=2,managedelement=2' | '//Subnetwork/ManagedElement' |    true      | _
        'subnetwork=2,managedelement=2,enodebfunction=1' | '//Subnetwork/ManagedElement' |    false      | _


        'subnetwork=2,managedelement=2' | '//Subnetwork//ManagedElement' |    true      | _
        'subnetwork=2,subnetwork=2,A=2,managedelement=2' | '//Subnetwork//ManagedElement' |    true      | _
        'A=2,subnetwork=2,managedelement=2' | '//Subnetwork//ManagedElement' |    true      | _
        'subnetwork=2,managedelement=2' | '/Subnetwork//ManagedElement' |    true      | _
        'subnetwork=2,managedelement=2,enodebfunction=1' | '//Subnetwork//ManagedElement' |    false      | _


        'managedelement=2' | '/ManagedElement' |    true      | _
        'subnetwork=2,subnetwork=2,managedelement=2' | '//ManagedElement' |    true      | _
        'A=2,subnetwork=2,managedelement=2' | 'ManagedElement' |    false      | _
        'subnetwork=2,managedelement=2' | '/ManagedElement' |    false      | _
        'subnetwork=2,managedelement=2,enodebfunction=1' | '//ManagedElement' |    false      | _

        'subnetwork=2,managedelement=2' | '/Subnetwork/ManagedElement/Enodebfunction' |    false      | _
        'subnetwork=2,subnetwork=2,managedelement=2' | '/Subnetwork/ManagedElement/Enodebfunction' |    false      | _
        'A=2,subnetwork=2,managedelement=2' | '/Subnetwork/ManagedElement/Enodebfunction' |    false      | _
        'subnetwork=2,managedelement=2' | '/Subnetwork/ManagedElement/Enodebfunction' |    false      | _
        'subnetwork=2,managedelement=2,A=1,enodebfunction=1' | '/Subnetwork/ManagedElement/Enodebfunction' |    false      | _

        'subnetwork=2,managedelement=2' | '/Subnetwork/ManagedElement//Enodebfunction' |    false      | _
        'subnetwork=2,subnetwork=2,managedelement=2' | '/Subnetwork/ManagedElement//Enodebfunction' |    false      | _
        'A=2,subnetwork=2,managedelement=2' | '/Subnetwork/ManagedElement//Enodebfunction' |    false      | _
        'subnetwork=2,managedelement=2' | '/Subnetwork/ManagedElement//Enodebfunction' |    false      | _
        'subnetwork=2,managedelement=2,A=1,enodebfunction=1' | '/Subnetwork/ManagedElement//Enodebfunction' |    true      | _
    }


    @Unroll
    def 'Execute isFdnValidAgainstScopeAndScopeLevel return success'() {
        given:
        CmGetRequest cmGetRequest = new CmGetRequest(baseFdn, scopeType, scopeLevel, null, null)

        when:
        def ret  = objUnderTest.isFdnValidAgainstScopeAndScopeLevel(fdn, cmGetRequest)
        then:
        ret == result

        where:
        baseFdn             | fdn                              |  scopeType             |  scopeLevel | result      | _
        'A=1,B=1,C=1'     | 'A=1'                            | ScopeType.BASE_ONLY       |    0        | true       | _

        null               | ''                               | ScopeType.BASE_SUBTREE   |    0        | true       | _
        null               | 'A=1'                            | ScopeType.BASE_SUBTREE   |    0        | false       | _
        null               | 'A=1'                            | ScopeType.BASE_SUBTREE   |    1        | true       | _
        null               | 'A=1,B=1'                       | ScopeType.BASE_SUBTREE   |    1        | false       | _
        null               | 'A=1,B=1'                       | ScopeType.BASE_SUBTREE   |    2        | true       | _

        'A=1'              | ''                                | ScopeType.BASE_SUBTREE   |    0        | true       | _
        'A=1'              | 'A=1'                            | ScopeType.BASE_SUBTREE   |     0        | true       | _
        'A=1'              | 'A=1,B=1'                       | ScopeType.BASE_SUBTREE   |     0        | false       | _
        'A=1'              | 'A=1,B=1'                       | ScopeType.BASE_SUBTREE   |     1        | true       | _
        'A=1'              | 'A=1,B=1,C=1'                  | ScopeType.BASE_SUBTREE   |     1        | false       | _
        'A=1'              | 'M=1,B=1,D=1'                  | ScopeType.BASE_SUBTREE   |    2        | true       | _

        'A=1,B=1'              | ''                                | ScopeType.BASE_SUBTREE   |    0        | true       | _
        'A=1,B=1'              | 'A=1'                            | ScopeType.BASE_SUBTREE   |     0        | true       | _
        'A=1,B=1'              | 'A=1,B=1'                       | ScopeType.BASE_SUBTREE   |     0        | true       | _
        'A=1,B=1'              | 'A=1,B=1,C=1'                  | ScopeType.BASE_SUBTREE   |     0        | false       | _
        'A=1,B=1'              | 'A=1,B=1,C=1'                  | ScopeType.BASE_SUBTREE   |     1        | true       | _
        'A=1,B=1'              | 'A=1,B=1,C=1,D=1'                | ScopeType.BASE_SUBTREE   |     1        | false       | _
        'A=1,B=1'              | 'A=1,B=1,C=1,D=1'                | ScopeType.BASE_SUBTREE   |     2        | true       | _

        null               | 'A=1'                            | ScopeType.BASE_NTH_LEVEL   |    0        | false       | _
        null               | 'A=1'                            | ScopeType.BASE_NTH_LEVEL   |    1        | true       | _
        null               | 'A=1,B=2'                       | ScopeType.BASE_NTH_LEVEL   |    1        | false       | _

        null               | 'A=1'                            | ScopeType.BASE_NTH_LEVEL   |    2        | false       | _
        null               | 'A=1,B=1'                       | ScopeType.BASE_NTH_LEVEL   |    2        | true      | _
        null               | 'A=1,B=1,C=1'                   | ScopeType.BASE_NTH_LEVEL   |    2        | false      | _

        ''               | 'A=1'                            | ScopeType.BASE_NTH_LEVEL   |    0        | false       | _
        ''               | 'A=1'                            | ScopeType.BASE_NTH_LEVEL   |    1        | true       | _
        ''               | 'A=1,B=2'                       | ScopeType.BASE_NTH_LEVEL   |    1        | false       | _

        ''               | 'A=1'                            | ScopeType.BASE_NTH_LEVEL   |    2        | false       | _
        ''               | 'A=1,B=1'                       | ScopeType.BASE_NTH_LEVEL   |    2        | true      | _
        ''               | 'A=1,B=1,C=1'                   | ScopeType.BASE_NTH_LEVEL   |    2        | false      | _


        'A=1'              | ''                                | ScopeType.BASE_NTH_LEVEL   |    0        | false       | _
        'A=1'              | 'A=1'                            | ScopeType.BASE_NTH_LEVEL   |     0        | true       | _
        'A=1'              | 'A=1,B=1'                       | ScopeType.BASE_NTH_LEVEL   |     0        | false       | _


        'A=1'              | ''                                | ScopeType.BASE_NTH_LEVEL   |    1        | false       | _
        'A=1'              | 'A=1'                             | ScopeType.BASE_NTH_LEVEL   |     1        | false       | _
        'A=1'              | 'A=1,B=1'                       | ScopeType.BASE_NTH_LEVEL   |     1        | true       | _
        'A=1'              | 'A=1,B=1,C=1'                  | ScopeType.BASE_NTH_LEVEL   |     1        | false       | _

        'A=1'              | ''                                | ScopeType.BASE_NTH_LEVEL   |    2        | false       | _
        'A=1'              | 'A=1'                             | ScopeType.BASE_NTH_LEVEL   |    2         | false       | _
        'A=1'              | 'A=1,B=1'                       | ScopeType.BASE_NTH_LEVEL   |     2        | false       | _
        'A=1'              | 'A=1,B=1,C=1'                  | ScopeType.BASE_NTH_LEVEL   |     2       | true       | _
        'A=1'              | 'A=1,B=1,C=1,D=1'              | ScopeType.BASE_NTH_LEVEL   |     2       | false       | _


        'A=1,B=1'          | ''                               | ScopeType.BASE_NTH_LEVEL   |     0        | false       | _
        'A=1,B=1'          | 'A=1'                            | ScopeType.BASE_NTH_LEVEL   |     0        | false       | _
        'A=1,B=1'          | 'A=1,B=1'                       | ScopeType.BASE_NTH_LEVEL    |     0        | true       | _
        'A=1,B=1'          | 'A=1,B=1,C=1'                  | ScopeType.BASE_NTH_LEVEL     |     0        | false       | _

        'A=1,B=1'          | ''                               | ScopeType.BASE_NTH_LEVEL   |     1        | false       | _
        'A=1,B=1'          | 'A=1'                            | ScopeType.BASE_NTH_LEVEL   |     1        | false       | _
        'A=1,B=1'          | 'A=1,B=1'                       | ScopeType.BASE_NTH_LEVEL    |     1       | false      | _
        'A=1,B=1'          | 'A=1,B=1,C=1'                  | ScopeType.BASE_NTH_LEVEL     |     1        | true       | _
        'A=1,B=1'          | 'A=1,B=1,C=1,D=1'             | ScopeType.BASE_NTH_LEVEL     |     1        | false       | _

        'A=1,B=1'          | 'A=1,B=1,C=1'                  | ScopeType.BASE_NTH_LEVEL     |     2        | false       | _
        'A=1,B=1'          | 'A=1,B=1,C=1,D=1'             | ScopeType.BASE_NTH_LEVEL     |     2         | true       | _
    }

    @Unroll
    def 'Execute filterWithoutAttributesHasTooManySlash return success'() {
        when:
        def ret  = objUnderTest.filterWithoutAttributesHasTooManySlash(filter)
        then:
        ret == result

        where:
        filter             | result  | _
        null | false | _
        '/Subnetwork/ManagedElement' | false | _
        'ManagedElement/EnodebFunction'| false | _
        'ManagedElement//EutrancelllFDD' | false | _
        'MeContext/ManagedElement/EnodebFunction[attributes/vendorname="Company XY"]' | false | _
        '/MeContext/ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'  | false | _
        '/MeContext/ManagedElement/ENodeBFunction[attributes/USERLABEL="value"]'  | false | _
        '//MeContext/ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]' | false | _
        'MeContext//ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]'  | false | _
        '////MeContext///ManagedElement//ENodeBFunction[attributes/USERLABEL="value"]' | true | _
        'ManagedElement///EnodebFunction'| true | _
        '///ManagedElement/EnodebFunction'| true | _
    }

    def 'dummy test to force maven to run test class with all other test methods annotated with @Unroll'() {
        when:
        objUnderTest.toString()
        new ParentChildRegexProducer()
        then:
        1 == 1
    }
}
