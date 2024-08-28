package com.ericsson.oss.services.cmnbicrud.ejb.testutil

import com.ericsson.cds.cdi.support.rule.ImplementationClasses
import com.ericsson.cds.cdi.support.rule.custom.node.ManagedObjectData
import com.ericsson.cds.cdi.support.rule.custom.node.NodeDataProvider
import com.ericsson.cds.cdi.support.rule.custom.node.NodeProperty
import com.ericsson.cds.cdi.support.rule.custom.node.RootNode
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject
import com.ericsson.oss.itpf.sdk.security.accesscontrol.EAccessControl
import com.ericsson.oss.services.cm.cmparser.impl.CmParserServiceBean
import com.ericsson.oss.services.cm.cmreader.ejb.impl.AsyncQueryExecutorDelegate
import com.ericsson.oss.services.cm.cmreader.ejb.impl.CmReaderServiceBean
import com.ericsson.oss.services.cm.cmshared.availability.DatabaseAvailabilityChecker
import com.ericsson.oss.services.cm.cmwriter.api.delete.CmWriterDeleteServiceLocal
import com.ericsson.oss.services.cm.cmwriter.dao.DataAccessBean
import com.ericsson.oss.services.cm.cmwriter.ejb.CmWriterServiceLocalBean
import com.ericsson.oss.services.cm.dto.mapping.DpsObjectMapperImpl
import com.ericsson.oss.services.cm.modelserviceextensions.AttributeInformationServiceImpl
import com.ericsson.oss.services.cm.modelserviceextensions.AttributeValidationServiceImpl
import com.ericsson.oss.services.cm.modelserviceextensions.ValidationServiceImpl
import com.ericsson.oss.services.scriptengine.spi.AsyncCommandRequest
import javax.inject.Inject

class BaseForCommandReceiverSpecs extends BaseSpecWithModels implements NodeDataProvider {

    public static final ACCESS_ALL = null
    @RootNode(nodeName = 'ERBS001', ipAddress = '10.0.0.1', version = '10.3.100', properties = [
            @NodeProperty(name = 'neType', value = 'ERBS'),
            @NodeProperty(name = 'ossPrefix', value = 'MeContext=ERBS001'),
            @NodeProperty(name = 'utcOffset', value = '+00:18')
    ])
    private ManagedObject erbsNode1

    @RootNode(nodeName = 'ERBS002', ipAddress = '10.0.0.2', version = '10.3.100', properties = [
            @NodeProperty(name = 'neType', value = 'ERBS'),
            @NodeProperty(name = 'ossPrefix', value = 'MeContext=ERBS002'),
            @NodeProperty(name = 'utcOffset', value = '+00:18')
    ])
    private ManagedObject erbsNode2

    @RootNode(nodeName = 'TESTNODE', ipAddress = '10.0.0.2', version = '10.3.100', properties = [
            @NodeProperty(name = 'neType', value = 'ERBS'),
            @NodeProperty(name = 'ossPrefix', value = 'MeContext=TESTNODE'),
            @NodeProperty(name = 'utcOffset', value = '+00:18')
    ])
    private ManagedObject testNode

    @RootNode(nodeName = 'BSC011', ipAddress = '10.0.0.3', version = '10.21.0', namespace = 'ComTop', properties = [
            @NodeProperty(name = 'neType', value = 'BSC'),
            @NodeProperty(name = 'ossPrefix', value = 'MeContext=BSC011')
    ])
    private ManagedObject bscNode1

    @RootNode(nodeName = 'ANOTHERNODE', ipAddress = '10.0.0.2', version = '7.1.301', properties = [
            @NodeProperty(name = 'neType', value = 'ERBS'),
            @NodeProperty(name = 'ossPrefix', value = 'SubNetwork=Sample,MeContext=ANOTHERNODE'),
            @NodeProperty(name = 'utcOffset', value = '+00:18')
    ])
    private ManagedObject anotherNode

    static technologyDomainProp1 = ['3G', '4G']
    static technologyDomainProp2 = ['3G', '4G', '5G']

    static neProductVersionProp1 = [['revision': 'H1160', 'identity': 'CXP\nL17"ACP1']]
    static neProductVersionProp2 = [['revision': 'H1160', 'identity': 'CXP\nL17"ACP1']]

    @ImplementationClasses
    def classes = [
            CmParserServiceBean,
            ContextServiceImplForTest,
            ValidationServiceImpl,
            AttributeValidationServiceImpl,
            AttributeInformationServiceImpl,
            DpsObjectMapperImpl,
            //  StubbedCmWriterRequestDispatcher,  forse questo non serve
            AsyncQueryExecutorDelegate,
            CmReaderServiceBean,
            CmWriterServiceLocalBean,
            DataAccessBean
    ]


    @Inject
    DatabaseAvailabilityChecker mockedDatabaseAvailabilityChecker

    def setup() {
        mockedDatabaseAvailabilityChecker.isAvailable() >> true
        setupDbNodes(2,4)
        addSubnetWork()
    }

    def addSubnetWork() {
        runtimeDps.addManagedObject().withFdn("SubNetwork=Sample").addAttribute("SubNetworkId", "Sample")
                .namespace("OSS_TOP").version("3.0.0").build()
    }


    @Override
    Map<String, Object> getAttributesForMo(final String moFdn) {
        def fdnToAttributeMap = [
                //  'MeContext=ERBS002,ManagedElement=1' : [userLabel: 'otherLabel', neType: 'ERBS', mimInfo:mimInfoProp, healthCheckResult:healthCheckResultProp],
                'NetworkElement=ERBS001' : [technologyDomain:technologyDomainProp1, neProductVersion: neProductVersionProp1, testDate:new Date(119, 8, 18, 11, 0, 0)],
                'NetworkElement=ERBS002' : [technologyDomain:technologyDomainProp2, neProductVersion: neProductVersionProp2, testDate:new Date(119, 8, 18, 11, 0, 0)]
                //'MeContext=ERBS001,ManagedElement=1,ENodeBFunction=1' : [dnsLookupTimer: 5100, pmPagS1DiscDistr: 0, eNodeBPlmnId: eNodeBPlmnIdPop, userLabel:'test'],
                //'MeContext=ERBS002,ManagedElement=1,ENodeBFunction=1' : [dnsLookupTimer: 5100, pmPagS1DiscDistr: 0, eNodeBPlmnId: eNodeBPlmnIdPop, userLabel:'test']

        ]

        def map = fdnToAttributeMap[moFdn]
        map == null ? [:] : map
    }


    @Override
    List<ManagedObjectData> getAdditionalNodeManagedObjects() {
        []  //we use setupDbNodes to create new objects
    }

    def setupDbNodes(final int numberOfNode, final int numberOfCell) {
        (1..numberOfNode).each { node ->
            runtimeDps.addManagedObject().withFdn("MeContext=ERBS00${node},ManagedElement=1,ENodeBFunction=1")
                    .addAttribute('ENodeBFunctionId', "1")
                    .addAttribute('userLabel', 'value')
                    .addAttribute('eNodeBPlmnId', [mcc: 533, mnc: 57, mncLength:2])
                    .onAction('updateMMEConnection').returnValue("RETAIN_EXISTING")
                    .namespace(ERBS_MODEL)
                    .version(ERBS_VERSION)
                    .type("ENodeBFunction")
                    .build()

            (1..numberOfCell).each { cell ->
                runtimeDps.addManagedObject().withFdn("MeContext=ERBS00${node},ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=${cell}")
                        .addAttribute('EUtranCellFDDId', "${cell}")
                        .addAttribute('userLabel', 'value')
                        .onAction('startAilg').returnValue(null)
                        .onAction('changeFrequency').returnValue(null)
                        .namespace(ERBS_MODEL)
                        .version(ERBS_VERSION)
                        .type("EUtranCellFDD")
                        .build()
            }
        }
    }

    def setupDbNodesInSubNetwork(final int numberOfNode, final int numberOfCell) {
        (1..numberOfNode).each { node ->
            runtimeDps.addManagedObject().withFdn("SubNetwork=Sample,MeContext=ERBS88${node},ManagedElement=1,ENodeBFunction=1")
                    .addAttribute('ENodeBFunctionId', "1")
                    .addAttribute('userLabel', 'value')
                    .addAttribute('eNodeBPlmnId', [mcc: 533, mnc: 57, mncLength:2])
                    .onAction('updateMMEConnection').returnValue("RETAIN_EXISTING")
                    .namespace(ERBS_MODEL)
                    .version(ERBS_VERSION)
                    .type("ENodeBFunction")
                    .build()

            (1..numberOfCell).each { cell ->
                runtimeDps.addManagedObject().withFdn("SubNetwork=Sample,MeContext=ERBS88${node},ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=${cell}")
                        .addAttribute('EUtranCellFDDId', "${cell}")
                        .addAttribute('userLabel', 'value')
                        .onAction('startAilg').returnValue(null)
                        .onAction('changeFrequency').returnValue(null)
                        .namespace(ERBS_MODEL)
                        .version(ERBS_VERSION)
                        .type("EUtranCellFDD")
                        .build()
            }
        }
    }

    def setupDbSpecialNode() {
            runtimeDps.addManagedObject().withFdn("MeContext=TESTNODE,ManagedElement=1,ENodeBFunction=1")
                    .addAttribute('ENodeBFunctionId', "1")
                    .addAttribute('userLabel', 'value')
                    .addAttribute('eNodeBPlmnId', null)
                    .onAction('updateMMEConnection').returnValue("RETAIN_EXISTING")
                    .namespace(ERBS_MODEL)
                    .version(ERBS_VERSION)
                    .type("ENodeBFunction")
                    .build()
    }

    def setupAdditionalNode() {
            runtimeDps.addManagedObject().withFdn("MeContext=ANOTHERNODE,ManagedElement=1,ENodeBFunction=1")
                    .addAttribute('ENodeBFunctionId', "1")
                    .addAttribute('userLabel', 'value')
                    .addAttribute('eNodeBPlmnId', [mcc: 533, mnc: 57, mncLength:2])
                    .onAction('updateMMEConnection').returnValue("RETAIN_EXISTING")
                    .namespace(ERBS_MODEL)
                    .version('7.1.301')
                    .type("ENodeBFunction")
                    .build()

            runtimeDps.addManagedObject().withFdn("MeContext=ANOTHERNODE,ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=1")
                        .addAttribute('EUtranCellFDDId', "1")
                        .addAttribute('userLabel', 'value')
                        .onAction('startAilg').returnValue(null)
                        .onAction('changeFrequency').returnValue(null)
                        .namespace(ERBS_MODEL)
                        .version('7.1.301')
                        .type("EUtranCellFDD")
                        .build()

    }

    def addChildToEutranCellFdd(final String nodeName, final int numberOfCell) {
        (1..numberOfCell).each { cell ->
            runtimeDps.addManagedObject().withFdn("MeContext="+nodeName+",ManagedElement=1,ENodeBFunction=1,EUtranCellFDD=${cell},UtranFreqRelation=1")
                    .addAttribute('UtranFreqRelationId ', "1")
                    .namespace(ERBS_MODEL)
                    .version(ERBS_VERSION)
                    .type("UtranFreqRelation")
                    .build()
        }
    }

    def createCommandRequest(final String command, final String userId, final String requestId) {
        def commandRequest = AsyncCommandRequest.fromCommandString(command)
        commandRequest.requestId = requestId
        commandRequest.userId = userId
        return commandRequest
    }
}
