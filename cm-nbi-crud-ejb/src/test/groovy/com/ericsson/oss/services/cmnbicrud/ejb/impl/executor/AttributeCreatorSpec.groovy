package com.ericsson.oss.services.cmnbicrud.ejb.impl.executor

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.cmnbicrud.ejb.log.ResponseHelper
import spock.lang.Unroll

class AttributeCreatorSpec extends CdiSpecification {

    @ObjectUnderTest
    AttributeCreator objUnderTest;

    def 'AttributeCreator simple test'() {
        when: 'createAttributeContainer is invoked'
            Map<String, Object> attributes = new HashMap<>()
            attributes.put("userLabel", "pippo")

            // Complex
            Map<String, Object> eNodeBPlmnIdPop = new HashMap<>()
            eNodeBPlmnIdPop.put("mcc", 53)
            eNodeBPlmnIdPop.put("mnc", 57)
            eNodeBPlmnIdPop.put("mncLength", 2)
            attributes.put("eNodeBPlmnId", eNodeBPlmnIdPop)


            // List
            ArrayList list = new ArrayList()
            list.add("4G")
            list.add("5G")
            attributes.put("technologyDomain", list)

            // List of complex
            Map<String, Object> complex1 = new HashMap<>()
            complex1.put("m1", 10)
            complex1.put("m2", 20)
            Map<String, Object> complex2 = new HashMap<>()
            complex2.put("m1", 30)
            complex2.put("m2", 40)

            ArrayList listOfComplex = new ArrayList()
            listOfComplex.add(complex1)
            listOfComplex.add(complex2)
            attributes.put("ListOfComplex", listOfComplex)



        objUnderTest.createAttributeContainer(attributes)
        then:
         objUnderTest.toString()
    }

    @Unroll
    def 'getTransformedValue return right value '() {
        when: 'getTransformedValue is invoked'
            def returnedValue = objUnderTest.getTransformedValue(value)
        then:
            returnedValue == expectedValue
        where:
        value      | expectedValue | _
        'null' | null           | _
        null     | null            |_
        1         | '1'            |_
    }

     def 'dummy test to force maven to run test class with all other test methods annotated with @Unroll'() {
            when:
            objUnderTest.toString()
            then:
            1 == 1
        }
    }
