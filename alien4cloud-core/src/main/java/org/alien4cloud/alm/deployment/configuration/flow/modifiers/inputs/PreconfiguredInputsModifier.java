package org.alien4cloud.alm.deployment.configuration.flow.modifiers.inputs;

import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.topology.task.MissingVariablesTask;
import alien4cloud.topology.task.UnresolvablePredefinedInputsTask;
import org.alien4cloud.alm.deployment.configuration.flow.EnvironmentContext;
import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.alm.deployment.configuration.model.PreconfiguredInputsConfiguration;
import org.alien4cloud.tosca.editor.EditorRepositoryService;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.variable.InputsMappingFileVariableResolver;
import org.alien4cloud.tosca.variable.MissingVariablesException;
import org.alien4cloud.tosca.variable.PredefinedVariables;
import org.alien4cloud.tosca.variable.QuickFileStorageService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

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

        PredefinedVariables predefinedVariables = new PredefinedVariables();
        predefinedVariables.setApplicationEnvironment(environment);
        predefinedVariables.setLocation(locations.values().stream().findFirst().get());
        predefinedVariables.setApplication(environmentContext.getApplication());

        // TODO: avoid reloading every time - find a way to know the last update on files (git hash ?)
        Properties appVarProps = quickFileStorageService.loadApplicationVariables(environmentContext.getApplication().getId());
        Properties envVarProps = quickFileStorageService.loadEnvironmentVariables(topology.getId(), environment.getId());
        Map<String, Object> inputsMappingsMap = quickFileStorageService.loadInputsMappingFile(topology.getId());

        InputsMappingFileVariableResolver inputsMappingFileVariableResolver = new InputsMappingFileVariableResolver(appVarProps, envVarProps,
                predefinedVariables);
        Map<String, PropertyValue> resolvedInputsMappingFile = null;
        try {
            resolvedInputsMappingFile = inputsMappingFileVariableResolver.resolveAsPropertyValue(inputsMappingsMap, topology.getInputs());
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

        context.saveConfiguration(preconfiguredInputsConfiguration);
    }

}