package org.alien4cloud.alm.deployment.configuration.services;

import alien4cloud.common.MetaPropertiesService;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.common.MetaPropConfiguration;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.utils.TagUtil;
import alien4cloud.utils.services.ConstraintPropertyService;
import alien4cloud.utils.services.PropertyService;
import com.google.common.collect.Maps;
import org.alien4cloud.alm.deployment.configuration.events.OnDeploymentConfigCopyEvent;
import org.alien4cloud.alm.deployment.configuration.model.AbstractDeploymentConfig;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentInputs;
import org.alien4cloud.tosca.exceptions.ConstraintTechnicalException;
import org.alien4cloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;
import org.alien4cloud.tosca.exceptions.ConstraintViolationException;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.templates.Topology;
import org.apache.commons.collections4.MapUtils;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Map;
import java.util.stream.Collectors;

import static alien4cloud.utils.AlienUtils.safe;

/**
 * Manage input values storage and retrieval.
 */
@Service
public class InputService {
    // Prefix for contextual inputs.
    public static final String LOC_META = "loc_meta_";
    public static final String APP_META = "app_meta_";
    public static final String APP_TAGS = "app_tags_";

    @Inject
    private MetaPropertiesService metaPropertiesService;
    @Inject
    private DeploymentConfigurationDao deploymentConfigurationDao;
    @Inject
    private PropertyService propertyService;
    @Inject
    private TopologyServiceCore topologyServiceCore;

    public void setInputValues(ApplicationEnvironment environment, Topology topology, Map<String, Object> inputProperties) {
        if (safe(inputProperties).isEmpty()) {
            return;
        }

        // Get the configuration object
        DeploymentInputs configuration = deploymentConfigurationDao.findById(DeploymentInputs.class,
                AbstractDeploymentConfig.generateId(environment.getTopologyVersion(), environment.getId()));

        if (configuration == null) {
            configuration = new DeploymentInputs(environment.getTopologyVersion(), environment.getId());
        }

        for (Map.Entry<String, Object> inputPropertyValue : inputProperties.entrySet()) {
            if (topology.getInputs() == null || topology.getInputs().get(inputPropertyValue.getKey()) == null) {
                throw new NotFoundException("Input", inputPropertyValue.getKey(), "Input <" + inputPropertyValue.getKey()
                        + "> cannot be found on topology for application <" + environment.getApplicationId() + "> environement <" + environment.getId() + ">");
            }

            try {
                propertyService.setPropertyValue(configuration.getInputs(), topology.getInputs().get(inputPropertyValue.getKey()), inputPropertyValue.getKey(),
                        inputPropertyValue.getValue());
            } catch (ConstraintValueDoNotMatchPropertyTypeException | ConstraintViolationException e) {
                throw new ConstraintTechnicalException("Dispatching constraint violation.", e);
            }
        }

        // Save configuration
        deploymentConfigurationDao.save(configuration);
    }

    /**
     * Get input values matching the requested input definitions as defined in application meta properties or tags.
     *
     * @param application      The application for which to fetch meta properties or tag based inputs.
     * @param inputDefinitions The input definitions that may define request for application meta or tags based inputs.
     * @return A map of <Input name, Property value> computed from the application meta and tags.
     */
    public Map<String, PropertyValue> getAppContextualInputs(Application application, Map<String, PropertyDefinition> inputDefinitions) {
        if (inputDefinitions == null) {
            return Maps.newHashMap();
        }

        Map<String, PropertyValue> inputs = Maps.newHashMap();

        if (application.getMetaProperties() != null) {
            Map<String, String> metaPropertiesValuesMap = application.getMetaProperties();
            prefixAndAddContextInput(inputDefinitions, inputs, APP_META, metaPropertiesValuesMap, true);
        }

        Map<String, String> tags = TagUtil.tagListToMap(application.getTags());
        prefixAndAddContextInput(inputDefinitions, inputs, APP_TAGS, tags, false);
        return inputs;
    }

    /**
     * Get input values matching the requested input definitions as defined in location meta properties.
     *
     * @param locations        The map of locations from which to fetch meta properties based inputs.
     * @param inputDefinitions The input definitions that may define request for location meta based inputs.
     * @return A map of <Input name, Property value> computed from the location meta properties.
     */
    public Map<String, PropertyValue> getLocationContextualInputs(Map<String, Location> locations, Map<String, PropertyDefinition> inputDefinitions) {
        if (inputDefinitions == null) {
            return Maps.newHashMap();
        }

        Map<String, String> metaPropertiesValuesMap = Maps.newHashMap();
        for (Location location : safe(locations).values()) {
            metaPropertiesValuesMap.putAll(safe(location.getMetaProperties()));
        }

        Map<String, PropertyValue> inputs = Maps.newHashMap();
        prefixAndAddContextInput(inputDefinitions, inputs, LOC_META, metaPropertiesValuesMap, true);
        return inputs;
    }

    /**
     * Add the context inputs to the actual deployment inputs by adding the given prefix to the key.
     *
     * @param inputDefinitions The input definitions for which to retrieve values.
     * @param inputs           The map of input values in which to add the fetched values.
     * @param prefix           The prefix to be added to the context inputs.
     * @param contextInputs    The map of inputs from context elements (cloud, application, environment).
     */
    private void prefixAndAddContextInput(Map<String, PropertyDefinition> inputDefinitions, Map<String, PropertyValue> inputs, String prefix,
                                          Map<String, String> contextInputs, boolean isMeta) {
        if (contextInputs == null || contextInputs.isEmpty()) {
            // no inputs to add.
            return;
        }
        if (isMeta) {
            String[] ids = new String[contextInputs.size()];
            int i = 0;
            for (String id : contextInputs.keySet()) {
                ids[i++] = id;
            }
            Map<String, MetaPropConfiguration> configurationMap = metaPropertiesService.getByIds(ids);
            for (Map.Entry<String, String> contextInputEntry : contextInputs.entrySet()) {
                MetaPropConfiguration metaPropConfiguration = configurationMap.get(contextInputEntry.getKey());
                if (metaPropConfiguration != null) {
                    addToInputs(inputDefinitions, inputs, prefix + metaPropConfiguration.getName(), contextInputEntry.getValue());
                }
            }
        } else {
            for (Map.Entry<String, String> contextInputEntry : contextInputs.entrySet()) {
                addToInputs(inputDefinitions, inputs, prefix + contextInputEntry.getKey(), contextInputEntry.getValue());
            }
        }
    }

    private void addToInputs(Map<String, PropertyDefinition> inputDefinitions, Map<String, PropertyValue> inputs, String inputKey, String value) {
        if (value != null && inputDefinitions.containsKey(inputKey)) {
            inputs.put(inputKey, new ScalarPropertyValue(value));
        }
    }

    @EventListener
    @Order(10)
    public void onCopyConfiguration(OnDeploymentConfigCopyEvent onDeploymentConfigCopyEvent) {
        ApplicationEnvironment source = onDeploymentConfigCopyEvent.getSourceEnvironment();
        ApplicationEnvironment target = onDeploymentConfigCopyEvent.getTargetEnvironment();
        DeploymentInputs deploymentInputs = deploymentConfigurationDao.findById(DeploymentInputs.class,
                AbstractDeploymentConfig.generateId(source.getTopologyVersion(), source.getId()));

        if (deploymentInputs == null || MapUtils.isEmpty(deploymentInputs.getInputs())) {
            return; // Nothing to copy
        }

        Topology topology = topologyServiceCore.getOrFail(Csar.createId(target.getApplicationId(), target.getTopologyVersion()));

        if (MapUtils.isNotEmpty(topology.getInputs())) {
            Map<String, PropertyDefinition> inputsDefinitions = topology.getInputs();
            Map<String, AbstractPropertyValue> inputsToCopy = deploymentInputs.getInputs().entrySet().stream()
                    // Copy only inputs which exist in new topology's definition
                    .filter(inputEntry -> inputsDefinitions.containsKey(inputEntry.getKey())).filter(inputEntry -> {
                        // Copy only inputs which satisfy the new input definition
                        try {
                            if (! (inputEntry.getValue() instanceof FunctionPropertyValue)) {
                                ConstraintPropertyService.checkPropertyConstraint(inputEntry.getKey(), PropertyService.asPropertyValue(inputEntry.getValue()),
                                        inputsDefinitions.get(inputEntry.getKey()));
                            }
                            return true;
                        } catch (ConstraintValueDoNotMatchPropertyTypeException | ConstraintViolationException e) {
                            return false;
                        }
                    }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            if (MapUtils.isNotEmpty(inputsToCopy)) {
                DeploymentInputs targetDeploymentInputs = deploymentConfigurationDao.findById(DeploymentInputs.class,
                        AbstractDeploymentConfig.generateId(target.getTopologyVersion(), target.getId()));
                if (targetDeploymentInputs == null) {
                    targetDeploymentInputs = new DeploymentInputs(target.getTopologyVersion(), target.getId());
                }
                // Copy inputs from original topology
                targetDeploymentInputs.setInputs(inputsToCopy);
                deploymentConfigurationDao.save(targetDeploymentInputs);
            }
        }
    }

}