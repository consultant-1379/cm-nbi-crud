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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown=true)
public class MoObjects implements Serializable {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings({"squid:S1700"})
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, List<ResourceRepresentationType>> moObjects = null;

    @JsonAnyGetter
    @JsonFormat(with = JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED) //this allow to serialize single element as-is (written as single element without [])
    public Map<String, List<ResourceRepresentationType>> getMoObjects() {
        return moObjects;
    }

    @JsonAnySetter
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY) //this allow to deserialize single element as-is (written as single element without [])
    public void addMoObjects(String key, List<ResourceRepresentationType> value) {
        if (moObjects == null) {
            moObjects = new HashMap<>();
        }
        moObjects.put(key, value);
    }

    public void initMoObjects() {
        if (moObjects == null) {
            moObjects = new HashMap<>();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MoObjects moObject = (MoObjects) o;

        return moObjects != null ? moObjects.equals(moObject.moObjects) : moObject.moObjects == null;
    }

    @Override
    public int hashCode() {
        return moObjects != null ? moObjects.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "MoObjects{" +
                "moObjects=" + moObjects +
                '}';
    }

}
