/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.cmnbicrud.ejb.impl.executor;

import com.ericsson.oss.services.cmnbicrud.ejb.common.WriteRequest;
import com.ericsson.oss.services.cmnbicrud.ejb.common.WriteResponse;
import com.ericsson.oss.services.cmnbicrud.ejb.create.CmCreateRequest;
import com.ericsson.oss.services.cmnbicrud.ejb.delete.CmDeleteRequest;
import com.ericsson.oss.services.cmnbicrud.ejb.put.CmPutRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.transaction.TransactionSynchronizationRegistry;
import java.util.ArrayList;
import java.util.List;

import static com.ericsson.oss.services.cmnbicrud.ejb.common.FdnUtility.getTxStatus;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class CmNbiCrudPatchExecutorBean {

    private static final Logger logger = LoggerFactory.getLogger(CmNbiCrudPatchExecutorBean.class);

    @Inject
    CmNbiCrudCreateExecutor cmNbiCrudCreateExecutor;

    @Inject
    CmNbiCrudPutExecutor cmNbiCrudPutExecutor;

    @Inject
    CmNbiCrudDeleteExecutor cmNbiCrudDeleteExecutor;

    @Resource
    private EJBContext context;

    @Resource(mappedName = "java:comp/TransactionSynchronizationRegistry")
    TransactionSynchronizationRegistry tsr;


    public List<WriteResponse> executor(final List<WriteRequest> requests) {
        List<WriteResponse> responses = new ArrayList<>();

        WriteResponse writeResponse = null;
        int requestNum = 0;
        for (final WriteRequest request:requests) {
            requestNum++;
            logger.debug("CmNbiCrudPatchExecutorBean:: execute request{}={} [{}]", requestNum, request, getTxDescription());
            if (request instanceof CmCreateRequest) {
                writeResponse = cmNbiCrudCreateExecutor.executor((CmCreateRequest)request);
            } else if (request instanceof  CmPutRequest) {
                writeResponse = cmNbiCrudPutExecutor.executor((CmPutRequest)request);
            } else if (request instanceof CmDeleteRequest) {
                writeResponse = cmNbiCrudDeleteExecutor.executor((CmDeleteRequest) request);
            }

            if (writeResponse != null) {
                logger.debug("CmNbiCrudPatchExecutorBean:: response writeResponse{}={} [{}]", requestNum, writeResponse, getTxDescription());
                responses.add(writeResponse);
                if (writeResponse.isErrored()) {
                    context.setRollbackOnly();
                    logger.debug("CmNbiCrudPatchExecutorBean:: errored response so forced setRollbackOnly()  writeResponse{}={} [{}]", requestNum, writeResponse, getTxDescription());
                    break;
                }
            }
        }

        return responses;
    }

    private String getTxDescription() {
        return "Tx key="+tsr.getTransactionKey() + " status=" + getTxStatus(tsr.getTransactionStatus());

    }
}
