/*
 * ------------------------------------------------------------------------------
 * *******************************************************************************
 *  COPYRIGHT Ericsson  2017
 *
 *  The copyright to the computer program(s) herein is the property of
 *  Ericsson Inc. The programs may be used and/or copied only with written
 *  permission from Ericsson Inc. or in accordance with the terms and
 *  conditions stipulated in the agreement/contract under which the
 *  program(s) have been supplied.
 * ******************************************************************************
 * ----------------------------------------------------------------------------
 */

package com.ericsson.oss.services.cmnbicrud.ejb.testutil

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.providers.custom.model.ClasspathModelServiceProvider
import com.ericsson.cds.cdi.support.providers.custom.model.ModelPattern
import com.ericsson.cds.cdi.support.rule.custom.node.ManagedObjectData
import com.ericsson.cds.cdi.support.rule.custom.node.NodeDataInjector
import com.ericsson.cds.cdi.support.rule.custom.node.NodeDataProvider
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.itpf.modeling.schema.util.SchemaConstants
import org.junit.Rule

/**
 * This class would be base for adding required models to the tests.
 * It uses both node injector and RuntimeConfigurableDps.
 */
class BaseSpecWithModels extends CdiSpecification implements NodeDataProvider {
    static String ERBS_VERSION =  '10.3.100'
    static String ERBS_MODEL = 'ERBS_NODE_MODEL'
    static filteredModels = [
            new ModelPattern(SchemaConstants.DPS_PRIMARYTYPE, 'OSS_NE_DEF', '.*', '.*'),
            new ModelPattern(SchemaConstants.DPS_PRIMARYTYPE, 'OSS_TOP', 'SubNetwork', '.*'),
            new ModelPattern(SchemaConstants.DPS_PRIMARYTYPE, 'OSS_NE_CM_DEF', 'CmFunction', '.*'),
            new ModelPattern(SchemaConstants.DPS_PRIMARYTYPE, 'OSS_TOP', 'MeContext', '.*'),
            new ModelPattern(SchemaConstants.DPS_PRIMARYTYPE, 'OSS_TOP', 'MeContextWithAllObsoleteAttributes', '.*'),
            new ModelPattern(SchemaConstants.DPS_PRIMARYTYPE, 'OSS_TOP', 'ObsoleteMoMeContext', '.*'),
            new ModelPattern(SchemaConstants.DPS_PRIMARYTYPE, 'OSS_TOP', 'ManagedElement', '.*'),
            new ModelPattern(SchemaConstants.DPS_PRIMARYTYPE, 'OSS_TOP', 'NodeRoot', '.*'),
            new ModelPattern(SchemaConstants.DPS_PRIMARYTYPE, 'OSS_TOP', 'Collection', '.*'),
            new ModelPattern(SchemaConstants.DPS_PRIMARYTYPE, 'OSS_CLI', 'CliAlias', '.*'),
            new ModelPattern(SchemaConstants.DPS_PRIMARYTYPE, 'ERBS_NODE_MODEL', '.*', '.*'),
            new ModelPattern(SchemaConstants.DPS_PRIMARYTYPE, 'ComTop', '.*', '.*'),
            new ModelPattern(SchemaConstants.DPS_PRIMARYTYPE, 'BscFunction', '.*', '.*'),
            new ModelPattern(SchemaConstants.DPS_PRIMARYTYPE, 'BscM', '.*', '.*'),
            new ModelPattern(SchemaConstants.DPS_PRIMARYTYPE, 'BrM', '.*', '.*'),
            new ModelPattern(SchemaConstants.DPS_PRIMARYTYPE, 'ECIM_BrM', '.*', '.*'),
            new ModelPattern(SchemaConstants.DPS_PRIMARYTYPE, 'ECIM_Top', '.*', '.*'),
            new ModelPattern(SchemaConstants.DPS_PRIMARYTYPE, 'TOP_MED', 'ConnectivityInformation', '.*'),
            new ModelPattern(SchemaConstants.DPS_PRIMARYTYPE, 'MEDIATION', 'ConnectivityInformation', '.*'),
            new ModelPattern(SchemaConstants.DPS_PRIMARYTYPE, 'OSS_NE_SEC_DEF', 'SecurityFunction', '.*'),
            new ModelPattern(SchemaConstants.DPS_PRIMARYTYPE, 'OSS_NE_SEC_DEF', 'NetworkElementSecurity', '.*'),
            new ModelPattern(SchemaConstants.DPS_PRIMARYTYPE, 'blacklisted1', 'ENodeBFunction_BlackListed', '.*'),
            new ModelPattern(SchemaConstants.DPS_PRIMARYTYPE, 'blacklisted2', 'ENodeBFunction_BlackListed', '.*'),
            new ModelPattern(SchemaConstants.DPS_RELATIONSHIP, 'OSS_TOP', '.*', '.*'),
            new ModelPattern(SchemaConstants.DPS_RELATIONSHIP, 'ERBS_NODE_MODEL', '.*', '.*'),
            new ModelPattern(SchemaConstants.DPS_RELATIONSHIP, 'BscM', '.*', '.*'),
            new ModelPattern(SchemaConstants.DPS_RELATIONSHIP, 'ComTop', '.*', '.*'),
            new ModelPattern(SchemaConstants.OSS_CDT, 'ERBS_NODE_MODEL', '.*', '.*'),
            new ModelPattern(SchemaConstants.OSS_CDT, 'OSS_NE_DEF', '.*', '.*'),
            new ModelPattern(SchemaConstants.OSS_EDT, 'ERBS_NODE_MODEL', '.*', '.*'),
            new ModelPattern(SchemaConstants.OSS_EDT, 'OSS_NE_DEF', '.*', '.*'),
            new ModelPattern(SchemaConstants.OSS_TARGETTYPE, 'NODE', 'BSC', '.*'),
            new ModelPattern(SchemaConstants.OSS_TARGETTYPE, 'NODE', 'EPG-OI', '.*'),
            new ModelPattern(SchemaConstants.OSS_TARGETTYPE, 'NODE', 'ERBS', '.*'),
            new ModelPattern(SchemaConstants.OSS_TARGETVERSION, '.*', '.*', '.*'),
            new ModelPattern(SchemaConstants.CFM_MIMINFO, 'BSC', '.*', '.*'),
            new ModelPattern(SchemaConstants.CFM_MIMINFO, 'EPG-OI', '.*', '.*'),
            new ModelPattern(SchemaConstants.CFM_MIMINFO, 'ERBS', '.*', '.*')
    ]

    static ClasspathModelServiceProvider classpathModelServiceProvider = new ClasspathModelServiceProvider(filteredModels)

    RuntimeConfigurableDps runtimeDps

    def setup() {
        runtimeDps = cdiInjectorRule.getService(RuntimeConfigurableDps)
    }

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        super.addAdditionalInjectionProperties(injectionProperties)
        injectionProperties.addInjectionProvider(classpathModelServiceProvider)
//ORIGINAL        injectionProperties.autoLocateFrom('com.ericsson.oss.services.cmwriter')
        injectionProperties.autoLocateFrom('com.ericsson.oss.services.cmrestnbi')
    }

    @Rule
    public final NodeDataInjector nodeDataInjector = new NodeDataInjector(this, cdiInjectorRule)

    @Override
    Map<String, Object> getAttributesForMo(final String moFdn) {
        return [:]
    }

    @Override
    List<ManagedObjectData> getAdditionalNodeManagedObjects() {
        return []
    }
}
