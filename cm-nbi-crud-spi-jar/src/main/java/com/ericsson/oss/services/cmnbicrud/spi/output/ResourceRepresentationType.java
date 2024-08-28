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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonPropertyOrder({ "id", "attributes", "additionalProperties" })
@JsonIgnoreProperties(ignoreUnknown=true)
public class ResourceRepresentationType implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;

    @SuppressWarnings({"squid:S1948"})
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> attributes = null;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, List<ResourceRepresentationType>> additionalProperties = null;

    @JsonProperty("id")
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("attributes")
    public Map<String, Object> getAttributes() {
        return attributes;
    }
    public  void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
    public  void addAttribute(final String attributeName, final Object attributeValue) {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        attributes.put(attributeName, attributeValue);
    }

    public void initAdditionalProperties() {
        if (additionalProperties == null) {
            additionalProperties = new HashMap<>();
        }
    }


    @JsonAnyGetter
    public Map<String, List<ResourceRepresentationType>> getAdditionalProperties() {
        return additionalProperties;
    }

    @JsonAnySetter
    public void addAdditionalProperty(String key, List<ResourceRepresentationType> value) {
        if (additionalProperties == null) {
            additionalProperties = new HashMap<>();
        }
        additionalProperties.put(key, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourceRepresentationType myBean = (ResourceRepresentationType) o;

        if (id != null ? !id.equals(myBean.id) : myBean.id != null) return false;
        if (attributes != null ? !attributes.equals(myBean.attributes) : myBean.attributes != null) return false;
        return additionalProperties != null ? additionalProperties.equals(myBean.additionalProperties) : myBean.additionalProperties == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
        result = 31 * result + (additionalProperties != null ? additionalProperties.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ResourceRepresentationType{" +
                "id='" + id + '\'' +
                ", attributes=" + attributes +
                ", additionalProperties=" + additionalProperties +
                '}';
    }
}
