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
package com.ericsson.oss.services.cmnbicrud.spi.output;

import com.fasterxml.jackson.annotation.*;

import java.io.Serializable;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown=true)
public class JsonPatchObject implements Serializable {
    private static final long serialVersionUID = 1L;

    private String op;
    private String path;

    @SuppressWarnings("squid:S1948")
    private Object value;


    @JsonProperty("op")
    public String getOp() {
        return op;
    }

    @JsonProperty("path")
    public String getPath() {
        return path;
    }

    @JsonProperty("value")
    public Object getValue() {
        return value;
    }
    public void setValue(Object value) {
        this.value = value;
    }

    public boolean valueIsAnObject() {
        return (value instanceof Map);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        JsonPatchObject that = (JsonPatchObject) o;

        if (op != null ? !op.equals(that.op) : that.op != null) return false;
        if (path != null ? !path.equals(that.path) : that.path != null) return false;
        return (value != null ? !value.equals(that.value) : that.value == null);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (op != null ? op.hashCode() : 0);
        result = 31 * result + (path != null ? path.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "jsonPatchObject{ " +
                " op=" + op +
                " path=" + path +
                " value=" + value +
                '}';
    }

    public String toJsonString() {
        return "{ \"op\": \"" + op + "\"," +
                " \"path\": \"" + path + "\"," +
                " \"value\": \"" + value +"\"" +
                '}';
    }

}
