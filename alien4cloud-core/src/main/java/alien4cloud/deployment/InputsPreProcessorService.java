package alien4cloud.deployment;

import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;

import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;

import alien4cloud.application.ApplicationService;
import alien4cloud.application.TopologyCompositionService;
import alien4cloud.common.MetaPropertiesService;
import alien4cloud.deployment.matching.services.location.TopologyLocationUtils;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.common.MetaPropConfiguration;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.orchestrators.locations.Location;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.tosca.normative.ToscaFunctionConstants;
import alien4cloud.utils.TagUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Inputs pre processor service manages pre-processing of inputs parameters in a Topology.
 *
 * Inputs in alien 4 cloud may be stored in the DeploymentSetup or may be injected from the cloud, application or environment (not implemented yet).
 */
@Slf4j
@Service
public class InputsPreProcessorService {
    private static final String LOC_META = "loc_meta_";
    private static final String APP_META = "app_meta_";
    private static final String APP_TAGS = "app_tags_";

    @Resource
    private LocationService locationService;
    @Resource
    private ApplicationService applicationService;
    @Resource
    private MetaPropertiesService metaPropertiesService;
    @Resource
    private TopologyCompositionService topologyCompositionService;

    /**
     * Process the get inputs functions of a topology to inject actual input provided by the deployer user (from deployment setup) or from the location
     * or application meta-properties.
     *
     * @param deploymentTopology The deployment setup that contains the input values.
     * @param environment The environment instance linked to the deployment setup.
     * @param topology
     */
    public void processGetInput(DeploymentTopology deploymentTopology, ApplicationEnvironment environment, Topology topology) {
        topologyCompositionService.processTopologyComposition(topology);
        Map<String, PropertyValue> inputs = getInputs(deploymentTopology, environment);
        if (deploymentTopology.getNodeTemplates() != null) {
            for (Entry<String, NodeTemplate> entry : deploymentTopology.getNodeTemplates().entrySet()) {
                NodeTemplate nodeTemplate = entry.getValue();
                NodeTemplate initialNodeTemplate = topology.getNodeTemplates().get(entry.getKey());
                mergeGetInputProperties(initialNodeTemplate.getProperties(), nodeTemplate.getProperties());
                processGetInput(inputs, nodeTemplate.getProperties());

                // process relationships
                if (nodeTemplate.getRelationships() != null) {
                    for (Entry<String, RelationshipTemplate> relEntry : nodeTemplate.getRelationships().entrySet()) {
                        RelationshipTemplate relationshipTemplate = relEntry.getValue();
                        Map<String, AbstractPropertyValue> initialProperties = getInitialRelationshipProperties(relEntry.getKey(), initialNodeTemplate);
                        mergeGetInputProperties(initialProperties, relationshipTemplate.getProperties());
                        processGetInput(inputs, relationshipTemplate.getProperties());
                    }
                }
                if (nodeTemplate.getCapabilities() != null) {
                    for (Entry<String, Capability> capaEntry : nodeTemplate.getCapabilities().entrySet()) {
                        Capability capability = capaEntry.getValue();
                        Map<String, AbstractPropertyValue> capaInitialProps = getInitialCapabilityProperties(capaEntry.getKey(), initialNodeTemplate);
                        mergeGetInputProperties(capaInitialProps, capability.getProperties());
                        processGetInput(inputs, capability.getProperties());
                    }
                }
            }
        }
    }

    private void mergeGetInputProperties(Map<String, AbstractPropertyValue> from, Map<String, AbstractPropertyValue> to) {
        if (from != null && to != null) {
            for (Entry<String, AbstractPropertyValue> entry : to.entrySet()) {
                AbstractPropertyValue fromValue = from.get(entry.getKey());
                if (fromValue instanceof FunctionPropertyValue) {
                    entry.setValue(fromValue);
                }
            }
        }
    }

    private Map<String, AbstractPropertyValue> getInitialRelationshipProperties(String relationShipName, NodeTemplate nodeTemplate) {
        Map<String, AbstractPropertyValue> properties = Maps.newHashMap();
        if (nodeTemplate.getRelationships() != null && nodeTemplate.getRelationships().get(relationShipName) != null) {
            if (nodeTemplate.getRelationships().get(relationShipName).getProperties() != null) {
                properties = nodeTemplate.getRelationships().get(relationShipName).getProperties();
            }
        }
        return properties;
    }

    private Map<String, AbstractPropertyValue> getInitialCapabilityProperties(String capabilityName, NodeTemplate nodeTemplate) {
        Map<String, AbstractPropertyValue> properties = Maps.newHashMap();
        if (nodeTemplate.getCapabilities() != null && nodeTemplate.getCapabilities().get(capabilityName) != null) {
            if (nodeTemplate.getCapabilities().get(capabilityName).getProperties() != null) {
                properties = nodeTemplate.getCapabilities().get(capabilityName).getProperties();
            }
        }
        return properties;
    }

    /**
     *
     * Set the value of the not found getInput functions to null
     *
     * @param deploymentTopology
     */
    public void setUnprocessedGetInputToNullValue(DeploymentTopology deploymentTopology) {
        if (deploymentTopology.getNodeTemplates() != null) {
            for (NodeTemplate nodeTemplate : deploymentTopology.getNodeTemplates().values()) {
                setUnprocessedGetInputToNullValue(nodeTemplate.getProperties());
                if (nodeTemplate.getRelationships() != null) {
                    for (RelationshipTemplate relationshipTemplate : nodeTemplate.getRelationships().values()) {
                        setUnprocessedGetInputToNullValue(relationshipTemplate.getProperties());
                    }
                }
                if (nodeTemplate.getCapabilities() != null) {
                    for (Capability capability : nodeTemplate.getCapabilities().values()) {
                        setUnprocessedGetInputToNullValue(capability.getProperties());
                    }
                }
            }
        }
    }

    /**
     * Inputs can come from the deployer user (in such situations they are saved in the deployment setup) but also from the location or application
     * meta-properties.
     * This method creates a unified map of inputs to be injected in deployed applications.
     *
     * @param deploymentTopology The deployment topology.
     * @param environment The environment instance linked to the deployment setup.
     * @return A unified map of input for the topology containing the inputs from the deployment setup as well as the ones coming from location or application
     *         meta-properties.
     */
    private Map<String, PropertyValue> getInputs(DeploymentTopology deploymentTopology, ApplicationEnvironment environment) {
        // initialize a map with input from the deployment setup
        Map<String, PropertyValue> inputs = MapUtils.isEmpty(deploymentTopology.getInputProperties()) ? Maps.newHashMap()
                : deploymentTopology.getInputProperties();

        // Map id -> value of meta properties from cloud or application.
        Map<String, String> metaPropertiesValuesMap = Maps.newHashMap();

        Map<String, String> locationIds = TopologyLocationUtils.getLocationIds(deploymentTopology);
        if (MapUtils.isNotEmpty(locationIds)) {
            Map<String, Location> locations = locationService.getMultiple(locationIds.values());
            for (Location location : locations.values()) {
                if (MapUtils.isNotEmpty(location.getMetaProperties())) {
                    metaPropertiesValuesMap.putAll(location.getMetaProperties());
                }
            }
        }

        // inputs from the location starts with location meta
        prefixAndAddContextInput(inputs, InputsPreProcessorService.LOC_META, metaPropertiesValuesMap, true);

        // and the ones from the application meta-properties
        // meta or tags from application
        if (environment.getApplicationId() != null) {
            metaPropertiesValuesMap = Maps.newHashMap();
            Application application = applicationService.getOrFail(environment.getApplicationId());
            if (application.getMetaProperties() != null) {
                metaPropertiesValuesMap.putAll(application.getMetaProperties());
            }
            prefixAndAddContextInput(inputs, InputsPreProcessorService.APP_META, metaPropertiesValuesMap, true);

            Map<String, String> tags = TagUtil.tagListToMap(application.getTags());
            prefixAndAddContextInput(inputs, InputsPreProcessorService.APP_TAGS, tags, false);
        }

        return inputs;
    }

    /**
     * Add the context inputs to the actual deployment inputs by adding the given prefix to the key.
     *
     * @param inputs The inputs to be used for the topology deployment.
     * @param prefix The prefix to be added to the context inputs.
     * @param contextInputs The map of inputs from context elements (cloud, application, environment).
     */
    private void prefixAndAddContextInput(Map<String, PropertyValue> inputs, String prefix, Map<String, String> contextInputs, boolean isMeta) {
        if (contextInputs == null || contextInputs.isEmpty()) {
            return; // no inputs to add.
        }
        if (isMeta) {
            String[] ids = new String[contextInputs.size()];
            int i = 0;
            for (String id : contextInputs.keySet()) {
                ids[i++] = id;
            }
            Map<String, MetaPropConfiguration> configurationMap = metaPropertiesService.getByIds(ids);
            for (Map.Entry<String, String> contextInputEntry : contextInputs.entrySet()) {
                inputs.put(prefix + configurationMap.get(contextInputEntry.getKey()).getName(), new ScalarPropertyValue(contextInputEntry.getValue()));
            }
        } else {
            for (Map.Entry<String, String> contextInputEntry : contextInputs.entrySet()) {
                inputs.put(prefix + contextInputEntry.getKey(), new ScalarPropertyValue(contextInputEntry.getValue()));
            }
        }
    }

    private void processGetInput(Map<String, PropertyValue> inputs, Map<String, AbstractPropertyValue> properties) {
        if (properties != null) {
            for (Map.Entry<String, AbstractPropertyValue> propEntry : properties.entrySet()) {
                if (propEntry.getValue() instanceof FunctionPropertyValue) {
                    FunctionPropertyValue function = (FunctionPropertyValue) propEntry.getValue();
                    if (ToscaFunctionConstants.GET_INPUT.equals(function.getFunction())) {
                        String inputName = function.getParameters().get(0);
                        PropertyValue value = inputs.get(inputName);
                        // if not null, replace the input value. Otherwise, let it as a function for validation purpose later
                        if (value != null) {
                            propEntry.setValue(value);
                        }
                    } else {
                        log.warn("Function <{}> detected for property <{}> while only <get_input> should be authorized.", function.getFunction(),
                                propEntry.getKey());
                    }
                }
            }
        }
    }

    private void setUnprocessedGetInputToNullValue(Map<String, AbstractPropertyValue> properties) {
        if (properties != null) {
            for (Map.Entry<String, AbstractPropertyValue> propEntry : properties.entrySet()) {
                if (propEntry.getValue() instanceof FunctionPropertyValue) {
                    FunctionPropertyValue function = (FunctionPropertyValue) propEntry.getValue();
                    if (!ToscaFunctionConstants.GET_INPUT.equals(function.getFunction())) {
                        log.warn("Function <{}> detected for property <{}> while only <get_input> should be authorized. Value will be set to null",
                                function.getFunction(), propEntry.getKey());
                    }
                    propEntry.setValue(null);
                }
            }
        }
    }
}
