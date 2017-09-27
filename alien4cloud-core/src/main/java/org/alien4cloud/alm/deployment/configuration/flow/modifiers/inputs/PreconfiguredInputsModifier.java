package org.alien4cloud.alm.deployment.configuration.flow.modifiers.inputs;

import java.util.Date;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;

import org.alien4cloud.alm.deployment.configuration.flow.EnvironmentContext;
import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.alm.deployment.configuration.model.PreconfiguredInputsConfiguration;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.variable.AlienContextVariables;
import org.alien4cloud.tosca.variable.InputsMappingFileVariableResolver;
import org.alien4cloud.tosca.variable.MissingVariablesException;
import org.alien4cloud.tosca.variable.QuickFileStorageService;
import org.springframework.stereotype.Component;

import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.topology.task.MissingVariablesTask;
import alien4cloud.topology.task.UnresolvablePredefinedInputsTask;

/**
 * This modifier loads preconfigured inputs from inputs mapping file and make them available to other
 * modifiers by storing them into {@link org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext}
 * as a Configuration file {@link PreconfiguredInputsConfiguration}
 */
@Component
public class PreconfiguredInputsModifier implements ITopologyModifier {

    @Inject
    private QuickFileStorageService quickFileStorageService;

    @Override
    public void process(Topology topology, FlowExecutionContext context) {

        EnvironmentContext environmentContext = context.getEnvironmentContext()
                .orElseThrow(() -> new IllegalArgumentException("Preconfigured input modifier requires an environment context."));
        ApplicationEnvironment environment = environmentContext.getEnvironment();
        Map<String, Location> locations = (Map<String, Location>) context.getExecutionCache().get(FlowExecutionContext.DEPLOYMENT_LOCATIONS_MAP_CACHE_KEY);

        AlienContextVariables alienContextVariables = new AlienContextVariables();
        alienContextVariables.setApplicationEnvironment(environment);
        alienContextVariables.setLocation(locations.values().stream().findFirst().get());
        alienContextVariables.setApplication(environmentContext.getApplication());

        // TODO: avoid reloading every time - find a way to know the last update on files (git hash ?)
        Properties appVarProps = quickFileStorageService.loadApplicationVariables(environmentContext.getApplication().getId());
        Properties envTypeVarProps = quickFileStorageService.loadEnvironmentTypeVariables(topology.getId(), environment.getEnvironmentType());
        Properties envVarProps = quickFileStorageService.loadEnvironmentVariables(topology.getId(), environment.getId());
        Map<String, Object> inputsMappingsMap = quickFileStorageService.loadInputsMappingFile(topology.getId());

        Map<String, PropertyValue> resolvedInputsMappingFile = null;
        try {
            resolvedInputsMappingFile = InputsMappingFileVariableResolver
                    .configure(appVarProps, envTypeVarProps, envVarProps, alienContextVariables)
                    .resolve(inputsMappingsMap, topology.getInputs());
        } catch (MissingVariablesException e) {
            context.log().error(new MissingVariablesTask(e.getMissingVariables()));
            context.log().error(new UnresolvablePredefinedInputsTask(e.getUnresolvableInputs()));
        }

        PreconfiguredInputsConfiguration preconfiguredInputsConfiguration = new PreconfiguredInputsConfiguration(environment.getTopologyVersion(),
                environment.getId());
        preconfiguredInputsConfiguration.setInputs(resolvedInputsMappingFile);
        // TODO: improve me
        preconfiguredInputsConfiguration.setLastUpdateDate(new Date());
        preconfiguredInputsConfiguration.setCreationDate(new Date());

        // TODO: check constraints

        context.saveConfiguration(preconfiguredInputsConfiguration);
    }

}