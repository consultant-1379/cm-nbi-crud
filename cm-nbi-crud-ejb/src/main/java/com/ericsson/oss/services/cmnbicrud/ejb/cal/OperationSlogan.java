package com.ericsson.oss.services.cmnbicrud.ejb.cal;

public enum OperationSlogan {
    NA("N/A"),
    EXECUTE_GET("<EXECUTE READ> - GET"),
    EXECUTE_PUT("<EXECUTE UPDATE> - PUT"),
    EXECUTE_PUT_CREATE("<EXECUTE CREATE> - PUT"),
    EXECUTE_POST("<EXECUTE CREATE> - POST"),
    EXECUTE_JSON_PATCH("<EXECUTE JSON_PATCH> - PATCH"),
    EXECUTE_THREE_GPP_JSON_PATCH("<EXECUTE 3GPP_JSON_PATCH> - PATCH"),
    EXECUTE_DELETE("<EXECUTE DELETE> - DELETE"),
    EXECUTE_ACTION("<EXECUTE ACTION> - PATCH");

    private final String slogan;
    OperationSlogan(final String slogan) {
        this.slogan = slogan;
    }

    public String getSlogan() {
        return slogan;
    }

}
