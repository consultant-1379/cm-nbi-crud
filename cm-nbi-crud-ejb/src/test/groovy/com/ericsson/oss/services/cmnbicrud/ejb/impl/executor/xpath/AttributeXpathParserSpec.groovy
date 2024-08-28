package com.ericsson.oss.services.cmnbicrud.ejb.impl.executor.xpath

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.cmnbicrud.ejb.exceptions.NbiXpathParserException
import spock.lang.Unroll

import com.ericsson.oss.services.cm.cmshared.dto.AttributeSpecification;
import com.ericsson.oss.services.cm.cmshared.dto.AttributeSpecificationContainer;
import static com.ericsson.oss.services.cm.cmshared.dto.search.CmMatchCondition.EQUALS;
import static com.ericsson.oss.services.cm.cmshared.dto.search.CmMatchCondition.NOT_EQUALS;
import static com.ericsson.oss.services.cm.cmshared.dto.search.CmMatchCondition.LESS_THAN;
import static com.ericsson.oss.services.cm.cmshared.dto.search.CmMatchCondition.LESS_THAN_OR_EQUAL_TO;
import static com.ericsson.oss.services.cm.cmshared.dto.search.CmMatchCondition.GREATER_THAN;
import static com.ericsson.oss.services.cm.cmshared.dto.search.CmMatchCondition.GREATER_THAN_OR_EQUAL_TO;
import static com.ericsson.oss.services.cm.cmshared.dto.search.CmMatchCondition.COMPLEX_MATCH_REQUIRED;
/**
 * Created by enmadmin on 9/7/21.
 */
class AttributeXpathParserSpec extends CdiSpecification {

    @ObjectUnderTest
    AttributeXpathParser objUnderTest;

    @Unroll
    def 'Success XPath attribute String parsered for basic values'() {
        given: ' an attribute plm_id'
            String attrName = 'plm_id'
            String memberName = 'mcc';
        when:
            AttributeSpecificationContainer parserResult = objUnderTest.parseAttribute(xPathAttrString)
        then:
            parserResult.getExtendedAttributeNames() == [attrName] as Set
            parserResult.getExtendedAttributeSpecification(attrName).get(0).getCmMatchCondition() == COMPLEX_MATCH_REQUIRED
            parserResult.getExtendedAttributeSpecification(attrName).get(0).getName() == attrName
            AttributeSpecificationContainer memberAttrSpecCont = parserResult.getExtendedAttributeSpecification(attrName).get(0).getValue()
            AttributeSpecification memberAttrSpec = memberAttrSpecCont.getExtendedAttributeSpecification(memberName).get(0)
            memberAttrSpec.getCmMatchCondition() == memberMatchCondition
            memberAttrSpec.getName() == memberName
            memberAttrSpec.getValue() == memberValue

        and: 'no error exception is thrown'
            noExceptionThrown()
        where: ' check parser Result'
            xPathAttrString                              ||   memberMatchCondition           |   memberValue
            '[ attributes/plm_id/mcc=\"Company XY\"]'    ||   EQUALS                   |  'Company XY'
            '[attributes/plm_id/mcc!=true]'              ||   NOT_EQUALS               |  'true'
            '[attributes/plm_id/mcc  <=1  ]'             ||   LESS_THAN_OR_EQUAL_TO    |  '1'
            '[attributes/plm_id/mcc >=10 ]'              ||   GREATER_THAN_OR_EQUAL_TO |  '10'
            '[ attributes/plm_id/mcc >  10 ]'            ||   GREATER_THAN             |  '10'
            '  [attributes/plm_id/mcc <  10 ]  '         ||   LESS_THAN                |  '10'
    }

    @Unroll
    def 'Success XPath complex attribute String parsered for basic values'() {
            given: ' an attribute vendorName'
            String attrName = 'vendorName'
            when:
            AttributeSpecificationContainer parserResult = objUnderTest.parseAttribute(xPathAttrString)
            then:
            parserResult.getExtendedAttributeNames() == [attrName] as Set
            parserResult.getExtendedAttributeSpecification(attrName).get(0).getCmMatchCondition() == matchCondition
            parserResult.getExtendedAttributeSpecification(attrName).get(0).getName() == attrName
            parserResult.getExtendedAttributeSpecification(attrName).get(0).getValue() == attrValue as String
            and: 'no error exception is thrown'
            noExceptionThrown()
            where: ' check parser Result'
            xPathAttrString                              ||   matchCondition           |   attrValue
            '[ attributes/vendorName=\"Company XY\"]'    ||   EQUALS                   |  'Company XY'
            '[attributes/vendorName!=true]'              ||   NOT_EQUALS               |  'true'
            '[attributes/vendorName  <=1  ]'             ||   LESS_THAN_OR_EQUAL_TO    |  '1'
            '[attributes/vendorName >=10 ]'              ||   GREATER_THAN_OR_EQUAL_TO |  '10'
            '[ attributes/vendorName >  10 ]'            ||   GREATER_THAN             |  '10'
            '  [attributes/vendorName <  10 ]  '         ||   LESS_THAN                |  '10'
        }

    @Unroll
    def 'Success XPath attribute String parsered for basic multiple attributes in and condition'() {
        given: ' an attribute vendorName'
            String firstAttrName = 'aaa'
            String secondAttrName = 'bbb'
            String memberName = 'ccc'
        when:
        AttributeSpecificationContainer parserResult = objUnderTest.parseAttribute(xPathAttrString)
        then:
        parserResult.getExtendedAttributeNames() == [firstAttrName,secondAttrName] as Set
        parserResult.getExtendedAttributeSpecification(firstAttrName).get(0).getCmMatchCondition() == firstMatchCondition
        parserResult.getExtendedAttributeSpecification(firstAttrName).get(0).getName() == firstAttrName
        parserResult.getExtendedAttributeSpecification(firstAttrName).get(0).getValue() == firstValue as String
        parserResult.getExtendedAttributeSpecification(secondAttrName).get(0).getCmMatchCondition() == secondMatchCondition
        parserResult.getExtendedAttributeSpecification(secondAttrName).get(0).getName() == secondAttrName
        if( secondMatchCondition == COMPLEX_MATCH_REQUIRED) {
            AttributeSpecificationContainer memberAttrSpecCont = parserResult.getExtendedAttributeSpecification(secondAttrName).get(0).getValue()
            AttributeSpecification memberAttrSpec = memberAttrSpecCont.getExtendedAttributeSpecification(memberName).get(0)
            memberAttrSpec.getName() == memberName
            memberAttrSpec.getCmMatchCondition() == memberMatchCon
            memberAttrSpec.getValue() == memberValue
        } else {
            parserResult.getExtendedAttributeSpecification(secondAttrName).get(0).getValue() == secondValue
        }
        and: 'no error exception is thrown'
        noExceptionThrown()
        where: ' check parser Result'
        xPathAttrString                                          ||   firstMatchCondition  |   firstValue |  secondMatchCondition    | secondValue | memberMatchCon | memberValue
        '[ attributes/aaa=valStr and attributes/bbb>1 ]'         ||   EQUALS               |  'valStr'    | GREATER_THAN             | '1'         |     _          |   _
        ' [attributes/aaa=valStr   and attributes/bbb < 5 ]'     ||   EQUALS               |  'valStr'    | LESS_THAN                | '5'         |     _          |   _
        ' [attributes/aaa=valStr   and attributes/bbb/ccc < 7 ]' ||   EQUALS               |  'valStr'    | COMPLEX_MATCH_REQUIRED   |  _          | LESS_THAN      |   '7'
    }

    @Unroll
    def 'Success XPath attribute with multiple complex member in and condition'() {
        given: ' an complex attribute'
        String attributeName= 'aaa'
        String firstMember = 'm1'
        String secondMember = 'm2'
        when:
        AttributeSpecificationContainer parserResult = objUnderTest.parseAttribute(xPathAttrString)
        then:
        parserResult.getExtendedAttributeNames() == [attributeName] as Set
        parserResult.getExtendedAttributeSpecification(attributeName).get(0).getCmMatchCondition() == COMPLEX_MATCH_REQUIRED
        parserResult.getExtendedAttributeSpecification(attributeName).get(0).getName() == attributeName
        AttributeSpecificationContainer memberAttrSpecCont = parserResult.getExtendedAttributeSpecification(attributeName).get(0).getValue()
        memberAttrSpecCont.getExtendedAttributeSpecification(firstMember).get(0).getName() == firstMember
        memberAttrSpecCont.getExtendedAttributeSpecification(firstMember).get(0).getValue() == firstMemberValue
        memberAttrSpecCont.getExtendedAttributeSpecification(firstMember).get(0).getCmMatchCondition() == firstMemberMatchCond
        AttributeSpecificationContainer memberAttrSpecCont1 = parserResult.getExtendedAttributeSpecification(attributeName).get(1).getValue()
        memberAttrSpecCont1.getExtendedAttributeSpecification(secondMember).get(0).getName() == secondMember
        memberAttrSpecCont1.getExtendedAttributeSpecification(secondMember).get(0).getValue() == secondMemberValue
        memberAttrSpecCont1.getExtendedAttributeSpecification(secondMember).get(0).getCmMatchCondition() == secondMemberMatchCond
        and: 'no error exception is thrown'
        noExceptionThrown()
        where: ' check parser Result'
        xPathAttrString                                          || firstMemberMatchCond  |  firstMemberValue |  secondMemberMatchCond    | secondMemberValue
        '[ attributes/aaa/m1<1 and attributes/aaa/m2>5 ]'        ||   LESS_THAN           |  '1'              | GREATER_THAN              | '5'

    }

    @Unroll
    def 'Verify parseAttribute method for coverage_only'() {
        when:
        AttributeSpecificationContainer parserResult = objUnderTest.parseAttribute(xPathAttrString)
        then:
        thrown(NbiXpathParserException)

        where:
        xPathAttrString        | _
        ''                    | _
        '[]'                    | _
        '[ aa or bb ]'            | _
        '[ attributes/aaa=valStr or attributes/bbb>1 ]'            | _
        '[ attributes/aaa=valStr and attributes/bbb&&&1 ]'            | _
    }

    @Unroll
    def 'Verify cleanAttributeName method for coverage_only'() {
        when:
        String attr = objUnderTest.cleanAttributeName(attributeName)
        then:
        attr == expectedAttributeName

        where:
        attributeName  | expectedAttributeName | _
        'aa'           | 'aa'                 | _
        'attributes/aa'           | 'aa'     | _
    }

    @Unroll
    def 'Verify cleanValue method for coverage_only'() {
        when:
        String attr = objUnderTest.cleanValue(attributeName)
        then:
        attr == expectedAttributeName

        where:
        attributeName  | expectedAttributeName | _
        'aa'           | 'aa'                 | _
        '"aa"'           | 'aa'     | _
        "'aa'"           | 'aa'     | _
        '"aa'            | '"aa'     | _
        'aa"'            | 'aa"'     | _
        "'aa'"           | 'aa'     | _
        "'aa"            |  "'aa"     | _
        "aa'"            |  "aa'"     | _
    }

    def 'dummy test to force maven to run test class with all other test methods annotated with @Unroll'() {
        when:
        objUnderTest.toString()
        then:
        1 == 1
    }

}
