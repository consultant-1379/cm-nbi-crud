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
package com.ericsson.oss.services.cmnbicrud.ejb.impl.executor;

import com.ericsson.oss.services.cm.cmshared.dto.AttributeSpecification;
import com.ericsson.oss.services.cm.cmshared.dto.search.CmMatchCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import  com.ericsson.oss.services.cm.cmshared.dto.StringifiedAttributeSpecifications;
import com.ericsson.oss.services.cmnbicrud.ejb.patch.MapWithOnlyOneComplexMember;

/**
 * Created by enmadmin on 9/2/21.
 */
public class AttributeCreator {

        public StringifiedAttributeSpecifications createAttributeContainer(Map<String, Object> attributes) {
            StringifiedAttributeSpecifications stringifiedAttributeSpecifications = new StringifiedAttributeSpecifications();

            for (Map.Entry<String,Object> entry : attributes.entrySet()) {
                final String attributeName = entry.getKey();
                final Object value = entry.getValue();
                AttributeSpecification attributeSpecification = createAttributeSpecification(attributeName, value);
                stringifiedAttributeSpecifications.addExtendedAttributeSpecification(attributeSpecification);
            }
            return  stringifiedAttributeSpecifications;
        }

    public AttributeSpecification createAttributeSpecification(final String attributeName, final Object value) {
        if (value instanceof Map) {
            AttributeSpecification attributeSpecification = new AttributeSpecification();
            attributeSpecification.setName(attributeName);
            attributeSpecification.setCmMatchCondition(CmMatchCondition.NO_MATCH_REQUIRED);
            attributeSpecification.setValue(createAttributeContainer((Map)value));
            return attributeSpecification;
        } else if (value instanceof MapWithOnlyOneComplexMember) {
            AttributeSpecification attributeSpecification = new AttributeSpecification();
            attributeSpecification.setName(attributeName);
            attributeSpecification.setCmMatchCondition(CmMatchCondition.COMPLEX_MATCH_REQUIRED);
            attributeSpecification.setValue(createAttributeContainer(((MapWithOnlyOneComplexMember)value).getMap()));
            return attributeSpecification;
        } else if (value instanceof  List) {
            AttributeSpecification attributeSpecification = new AttributeSpecification();
            attributeSpecification.setName(attributeName);
            attributeSpecification.setCmMatchCondition(CmMatchCondition.NO_MATCH_REQUIRED);
            attributeSpecification.setValue(getListValue(value));
            return attributeSpecification;
        } else {
            AttributeSpecification attributeSpecification = new AttributeSpecification();
            attributeSpecification.setName(attributeName);
            attributeSpecification.setCmMatchCondition(CmMatchCondition.NO_MATCH_REQUIRED);
            attributeSpecification.setValue(getTransformedValue(value));
            return attributeSpecification;
        }
    }

    private Object getListValue(Object value) {
            if (value instanceof Map) {
                return createAttributeContainer((Map)value);
            } else if (value instanceof  List) {
                ArrayList list = new ArrayList();
                for (final Object member:(List)value) {
                    list.add(getListValue(member));
                }
                return list;
            } else {
               return getTransformedValue(value);
            }
    }

    private String getTransformedValue(Object value) {
            if (value == null || "null".equals(value)) {
                return null;
            }
            return (value.toString());
    }
}
