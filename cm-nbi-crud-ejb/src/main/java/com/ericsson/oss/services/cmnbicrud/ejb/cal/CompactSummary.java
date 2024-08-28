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
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("squid:S2384")
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonPropertyOrder({ "detailResult", "summaryResult", "errorDetail" })
public class CompactSummary implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String OP_TYPE_READ = "read";
    public static final String OP_TYPE_CREATE = "create";
    public static final String OP_TYPE_UPDATE = "update";
    public static final String OP_TYPE_DELETE = "delete";
    public static final String OP_TYPE_ACTION = "action";
    public static final String OP_TYPE_RESTART = "restart";
    public static final String OP_TYPE_ENABLE = "enable";


    @SuppressWarnings({"squid:S1700"})
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<DetailResultOperation> detailResult = null;

    @SuppressWarnings({"squid:S1700", "squid:S1948"})
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<SummaryResultOperation> summaryResult = null;

    @SuppressWarnings({"squid:S1700"})
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorDetail = null;

    /*
    *  getter (for serialization) and setter (for deserialization)
    * */
    @JsonProperty("detailResult")
    public List<DetailResultOperation> getDetailResult() { return detailResult; }
    public void setDetailResult(List<DetailResultOperation> detailResult) { this.detailResult = detailResult; }
    public  void addDetailResultOperation(final DetailResultOperation detailResultOperation) {
        if (detailResult == null) {
            detailResult = new ArrayList<>();
        }
        detailResult.add(detailResultOperation);
    }

    @JsonProperty("summaryResult")
    public List<SummaryResultOperation> getSummaryResult() { return summaryResult; }
    public void setSummaryResult(List<SummaryResultOperation> summaryResult) { this.summaryResult = summaryResult; }
    public  void addSummaryResultOperation(final SummaryResultOperation summaryResultOperation) {
        if (summaryResult == null) {
            summaryResult = new ArrayList<>();
        }
        summaryResult.add(summaryResultOperation);
    }

    @JsonProperty("errorDetail")
    public String getErrorDetail() { return errorDetail; }
    public void setErrorDetail(String errorDetail) { this.errorDetail = errorDetail; }
}
