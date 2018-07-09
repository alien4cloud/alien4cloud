package org.alien4cloud.alm.deployment.configuration.flow.modifiers.inputs;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Date;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;

import org.alien4cloud.alm.deployment.configuration.flow.EnvironmentContext;
import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.alm.deployment.configuration.model.PreconfiguredInputsConfiguration;
import org.alien4cloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;
import org.alien4cloud.tosca.exceptions.ConstraintViolationException;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.variable.AlienContextVariables;
import org.alien4cloud.tosca.variable.InputsMappingFileVariableResolver;
import org.alien4cloud.tosca.variable.service.QuickFileStorageService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.topology.task.MissingVariablesTask;
import alien4cloud.topology.task.PredefinedInputsConstraintViolationTask;
import alien4cloud.topology.task.TaskCode;
import alien4cloud.topology.task.UnresolvablePredefinedInputsTask;
import alien4cloud.tosca.properties.constraints.ConstraintUtil;
import alien4cloud.utils.services.ConstraintPropertyService;

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
        if (locations != null) {
            alienContextVariables.setLocation(locations.values().stream().findFirst().get());
        }
        alienContextVariables.setApplication(environmentContext.getApplication());

        // TODO: avoid reloading every time - find a way to know the last update on files (git hash ?)
        Properties appVarProps = quickFileStorageService.loadApplicationVariables(environmentContext.getApplication().getId());
        Properties envTypeVarProps = quickFileStorageService.loadEnvironmentTypeVariables(topology.getId(), environment.getEnvironmentType());
        Properties envVarProps = quickFileStorageService.loadEnvironmentVariables(topology.getId(), environment.getId());
        Map<String, Object> inputsMappingsMap = quickFileStorageService.loadInputsMappingFile(topology.getId());

        InputsMappingFileVariableResolver.InputsResolvingResult inputsResolvingResult = InputsMappingFileVariableResolver
                    .configure(appVarProps, envTypeVarProps, envVarProps, alienContextVariables)
                    .resolve(inputsMappingsMap, topology.getInputs());

        if (CollectionUtils.isNotEmpty(inputsResolvingResult.getMissingVariables())) {
            context.log().error(new MissingVariablesTask(inputsResolvingResult.getMissingVariables()));
        }

        if (CollectionUtils.isNotEmpty(inputsResolvingResult.getUnresolved())) {
            context.log().error(new UnresolvablePredefinedInputsTask(inputsResolvingResult.getUnresolved()));
        }

        // checking constraints
        Map<String, ConstraintUtil.ConstraintInformation> violations = Maps.newHashMap();
        Map<String, ConstraintUtil.ConstraintInformation> typesViolations = Maps.newHashMap();
        for (Map.Entry<String, PropertyValue> entry : safe(inputsResolvingResult.getResolved()).entrySet()) {
            try {
                ConstraintPropertyService.checkPropertyConstraint(entry.getKey(), entry.getValue(), topology.getInputs().get(entry.getKey()));
            } catch (ConstraintViolationException e) {
                violations.put(entry.getKey(), getConstraintInformation(e.getMessage(), e.getConstraintInformation()));
            } catch (ConstraintValueDoNotMatchPropertyTypeException e) {
                typesViolations.put(entry.getKey(), getConstraintInformation(e.getMessage(), e.getConstraintInformation()));
            }
        }

        if (MapUtils.isNotEmpty(violations)) {
            context.log().error(new PredefinedInputsConstraintViolationTask(violations, TaskCode.PREDEFINED_INPUTS_CONSTRAINT_VIOLATION));
        }
        if (MapUtils.isNotEmpty(typesViolations)) {
            context.log().error(new PredefinedInputsConstraintViolationTask(typesViolations, TaskCode.PREDEFINED_INPUTS_TYPE_VIOLATION));
        }

        PreconfiguredInputsConfiguration preconfiguredInputsConfiguration = new PreconfiguredInputsConfiguration(environment.getTopologyVersion(),
                environment.getId());
        preconfiguredInputsConfiguration.setInputs(inputsResolvingResult.getResolved());

        // add unresolved so that they are not considered as deployer input
        inputsResolvingResult.getUnresolved().forEach(unresolved -> preconfiguredInputsConfiguration.getInputs().put(unresolved, null));

        // TODO: improve me
        preconfiguredInputsConfiguration.setLastUpdateDate(new Date());
        preconfiguredInputsConfiguration.setCreationDate(new Date());

        context.saveConfiguration(preconfiguredInputsConfiguration);
    }

    private ConstraintUtil.ConstraintInformation getConstraintInformation(String message, ConstraintUtil.ConstraintInformation constraint) {
        if (constraint == null) {
            constraint = new ConstraintUtil.ConstraintInformation(null, message, null, null);
        }

        return constraint;
    }

}