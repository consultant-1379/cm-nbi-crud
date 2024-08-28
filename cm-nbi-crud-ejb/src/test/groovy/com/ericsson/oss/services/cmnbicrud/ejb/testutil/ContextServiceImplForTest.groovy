package com.ericsson.oss.services.cmnbicrud.ejb.testutil

import com.ericsson.oss.itpf.sdk.context.ContextService

/**
 * simple implementation of the context service for testing.
 */
class ContextServiceImplForTest implements ContextService {

    def properties = [:]

    @Override
    void setContextValue(final String key, final Serializable value) {
        properties.put(key, value)
    }

    @Override
    def <T> T getContextValue(final String key) {
        return properties.get(key)
    }

    @Override
    Map<String, Serializable> getContextData() {
        return properties
    }
}
