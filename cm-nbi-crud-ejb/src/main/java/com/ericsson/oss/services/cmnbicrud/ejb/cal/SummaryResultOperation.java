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
import java.util.Map;

@SuppressWarnings("squid:S2384")
@JsonPropertyOrder({ "opType", "entity", "result" })
@JsonIgnoreProperties(ignoreUnknown=true)
public class SummaryResultOperation implements Serializable {
    private static final long serialVersionUID = 1L;

    private String opType;
    private String entity;

    @SuppressWarnings({"squid:S1948"})
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> result = null;

    @JsonProperty("opType")
    public String getOpType() {
        return opType;
    }
    public void setOpType(String opType) {
        this.opType = opType;
    }

    @JsonProperty("entity")
    public String getEntity() {
        return entity;
    }
    public void setEntity(String entity) {
        this.entity = entity;
    }

    public SummaryResultOperation(String opType, String entity) {
        this.opType = opType;
        this.entity = entity;
    }

    /*
     *  getter (for serialization) and setter (for deserialization)
     * */
    @JsonProperty("result")
    public Map<String, Object> getResult() {
        return result;
    }
    public  void setResult(Map<String, Object> result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "CompactOperation{" +
                "opType='" + opType + '\'' +
                ", entity='" + entity + '\'' +
                '}';
    }
}
