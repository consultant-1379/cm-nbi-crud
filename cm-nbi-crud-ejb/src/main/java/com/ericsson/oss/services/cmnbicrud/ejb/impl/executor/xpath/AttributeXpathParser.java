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
package com.ericsson.oss.services.cmnbicrud.ejb.impl.executor.xpath;

import com.ericsson.oss.services.cm.cmshared.dto.AttributeSpecification;
import com.ericsson.oss.services.cm.cmshared.dto.StringifiedAttributeSpecifications;
import com.ericsson.oss.services.cm.cmshared.dto.AttributeSpecificationContainer;
import com.ericsson.oss.services.cm.cmshared.dto.search.CmMatchCondition;
import com.ericsson.oss.services.cmnbicrud.ejb.exceptions.NbiXpathParserException;

/**
 * Created by enmadmin on 9/7/21.
 */
public class AttributeXpathParser {
   // [attributes/vendorname="Company XY"]

    public AttributeSpecificationContainer parseAttribute(final String attributeXpathString) {
        if (attributeXpathString == null) {
            return new StringifiedAttributeSpecifications();
        }

        // remove []
        final String attributeXpath = attributeXpathString.trim();
        if (attributeXpath.length()>1) {
            int length = attributeXpath.length();
            String workingAttributeString = attributeXpath.substring(1, length - 1);

            checkForUnexpectedExpression(workingAttributeString);
            String[] attributesExpression = workingAttributeString.split(" and ");
            final StringifiedAttributeSpecifications attrSpecContainer = new StringifiedAttributeSpecifications();
            for (int i = 0; i < attributesExpression.length; i++) {
                final AttributeSpecification attrSpec = findOperator(attributesExpression[i]);
                attrSpecContainer.addExtendedAttributeSpecification(attrSpec);
            }
            return attrSpecContainer;
        }

        throw new NbiXpathParserException("Xpath Parser : Unexpected value or length");
    }

    private void checkForUnexpectedExpression(final String workingAttributeString) {
        if (workingAttributeString.contains(" or ")) {
            throw new NbiXpathParserException("Xpath Parser : Unexpected OR Operator");
        }
    }

    private  AttributeSpecification findOperator(final String attrString) {
        if (attrString.contains("<=")) {
            return createAttributeSpec(attrString, CmMatchCondition.LESS_THAN_OR_EQUAL_TO,"<=");
        }
        if (attrString.contains(">=")) {
            return createAttributeSpec(attrString, CmMatchCondition.GREATER_THAN_OR_EQUAL_TO,">=");
        }
        if (attrString.contains("!=")) {
            return createAttributeSpec(attrString, CmMatchCondition.NOT_EQUALS,"!=");
        }
        if (attrString.contains("=")) {
            return createAttributeSpec(attrString, CmMatchCondition.EQUALS,"=");
        }
        if (attrString.contains("<")) {
            return createAttributeSpec(attrString,CmMatchCondition.LESS_THAN,"<");
        }
        if (attrString.contains(">")) {
            return createAttributeSpec(attrString, CmMatchCondition.GREATER_THAN,">");
        }
        throw new NbiXpathParserException("Xpath Parser : Unexpected XPath Operator");
    }

    private String cleanAttributeName(final String attributeName) {
        if(attributeName.startsWith("attributes/")) {
            return attributeName.substring(11);
        }
        return attributeName;
    }

    private AttributeSpecification createAttributeSpec(final String attrString, final CmMatchCondition matchCond, final String cond) {
        AttributeSpecification attributeSpecification = new AttributeSpecification();
        String[] aString = attrString.split(cond);
        String attrName = cleanAttributeName(aString[0].trim());
        String attrValue = aString[1].trim();
        if (attrName.contains("/")) {
            String[] aa = attrName.split("/");
            attributeSpecification.setCmMatchCondition(CmMatchCondition.COMPLEX_MATCH_REQUIRED);
            attributeSpecification.setName(aa[0]);
            AttributeSpecification memberSpec = new AttributeSpecification();
            memberSpec.setName(aa[1]);
            memberSpec.setCmMatchCondition(matchCond);
            memberSpec.setValue(cleanValue(attrValue));
            final StringifiedAttributeSpecifications attrSpecContainer = new StringifiedAttributeSpecifications();
            attrSpecContainer.addExtendedAttributeSpecification(memberSpec);
            attributeSpecification.setValue(attrSpecContainer);

        } else {
            attributeSpecification.setCmMatchCondition(matchCond);
            attributeSpecification.setName(attrName);
            attributeSpecification.setValue(cleanValue(attrValue));
        }
        return attributeSpecification;
    }

    private String cleanValue(final String value) {
        if ((value.startsWith("\"") && value.endsWith("\"")) ||
                (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1,value.length()-1);
        }
        return value;
    }
}
