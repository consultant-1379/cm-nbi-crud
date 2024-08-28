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
package com.ericsson.oss.services.cmnbicrud.ejb.cal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("squid:S2384")
@JsonPropertyOrder({ "opType", "id", "oldValues", "currentValues" })
@JsonIgnoreProperties(ignoreUnknown=true)
public class DetailResultOperation implements Serializable {
    private static final long serialVersionUID = 1L;

    private String opType;
    private String id;

    @SuppressWarnings({"squid:S1948"})
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> currentValues = null;

    @SuppressWarnings({"squid:S1948"})
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> oldValues = null;

    @JsonProperty("opType")
    public String getOpType() {
        return opType;
    }
    public void setOpType(String opType) {
        this.opType = opType;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public DetailResultOperation(String opType, String id) {
        this.opType = opType;
        this.id = id;
    }

    /*
     *  getter (for serialization) and setter (for deserialization)
     * */
    @JsonProperty("currentValues")
    public Map<String, Object> getCurrentValues() {
        return currentValues;
    }
    public  void setCurrentValues(Map<String, Object> currentValues) {
        this.currentValues = currentValues;
    }
    public  void addAttributeCurrentValue(final String attributeName, final Object attributeValue) {
        if (currentValues == null) {
            currentValues = new LinkedHashMap<>();
        }
        currentValues.put(attributeName, attributeValue);
    }

    @JsonProperty("oldValues")
    public Map<String, Object> getOldValues() {
        return oldValues;
    }
    public  void setOldValues(Map<String, Object> oldValues) {
        this.oldValues = oldValues;
    }
    public  void addAttributeOldValue(final String attributeName, final Object attributeValue) {
        if (oldValues == null) {
            oldValues = new LinkedHashMap<>();
        }
        oldValues.put(attributeName, attributeValue);
    }

    @Override
    public String toString() {
        return "CompactOperation{" +
                "opType='" + opType + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
