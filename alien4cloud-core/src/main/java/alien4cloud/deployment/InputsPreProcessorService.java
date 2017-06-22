package alien4cloud.deployment;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;

import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.templates.AbstractTemplate;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Requirement;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.normative.constants.ToscaFunctionConstants;
import org.alien4cloud.tosca.utils.FunctionEvaluator;
import org.alien4cloud.tosca.utils.FunctionEvaluatorContext;
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
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.tosca.context.ToscaContextual;
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
     * @param topology The original topology.
     * @return The full inputs map used to actually inject inputs. This includes both the user inputs as well as location meta and applications meta and tags.
     */
    @ToscaContextual
    public Map<String, PropertyValue> injectInputValues(DeploymentTopology deploymentTopology, ApplicationEnvironment environment, Topology topology) {
        // This is required to get all the functions before they where processed in the deployment topology.
        topologyCompositionService.processTopologyComposition(topology);
        Map<String, PropertyValue> inputs = computeInputs(deploymentTopology, environment);

        if (deploymentTopology.getNodeTemplates() != null) {
            FunctionEvaluatorContext evaluatorContext = new FunctionEvaluatorContext(deploymentTopology, inputs);

            for (Entry<String, NodeTemplate> entry : deploymentTopology.getNodeTemplates().entrySet()) {
                NodeTemplate nodeTemplate = entry.getValue();
                NodeTemplate initialNodeTemplate = topology.getNodeTemplates().get(entry.getKey());
                mergeGetInputProperties(initialNodeTemplate.getProperties(), nodeTemplate.getProperties());
                processGetInput(evaluatorContext, nodeTemplate, nodeTemplate.getProperties());

                // process relationships
                if (nodeTemplate.getRelationships() != null) {
                    for (Entry<String, RelationshipTemplate> relEntry : nodeTemplate.getRelationships().entrySet()) {
                        RelationshipTemplate relationshipTemplate = relEntry.getValue();
                        Map<String, AbstractPropertyValue> initialProperties = getInitialRelationshipProperties(relEntry.getKey(), initialNodeTemplate);
                        mergeGetInputProperties(initialProperties, relationshipTemplate.getProperties());
                        processGetInput(evaluatorContext, relationshipTemplate, relationshipTemplate.getProperties());
                    }
                }
                if (nodeTemplate.getCapabilities() != null) {
                    for (Entry<String, Capability> capaEntry : nodeTemplate.getCapabilities().entrySet()) {
                        Capability capability = capaEntry.getValue();
                        Map<String, AbstractPropertyValue> capaInitialProps = getInitialCapabilityProperties(capaEntry.getKey(), initialNodeTemplate);
                        mergeGetInputProperties(capaInitialProps, capability.getProperties());
                        processGetInput(evaluatorContext, nodeTemplate, capability.getProperties());
                    }
                }
                if (nodeTemplate.getRequirements() != null) {
                    for (Entry<String, Requirement> requirementEntry : nodeTemplate.getRequirements().entrySet()) {
                        Requirement requirement = requirementEntry.getValue();
                        Map<String, AbstractPropertyValue> capaInitialProps = getInitialRequirementProperties(requirementEntry.getKey(), initialNodeTemplate);
                        mergeGetInputProperties(capaInitialProps, requirement.getProperties());
                        processGetInput(evaluatorContext, nodeTemplate, requirement.getProperties());
                    }
                }
            }
        }
        return inputs;
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
        if (nodeTemplate.getRelationships() != null && nodeTemplate.getRelationships().get(relationShipName) != null
                && nodeTemplate.getRelationships().get(relationShipName).getProperties() != null) {
            properties = nodeTemplate.getRelationships().get(relationShipName).getProperties();
        }
        return properties;
    }

    private Map<String, AbstractPropertyValue> getInitialCapabilityProperties(String capabilityName, NodeTemplate nodeTemplate) {
        Map<String, AbstractPropertyValue> properties = Maps.newHashMap();
        if (nodeTemplate.getCapabilities() != null && nodeTemplate.getCapabilities().get(capabilityName) != null
                && nodeTemplate.getCapabilities().get(capabilityName).getProperties() != null) {
            properties = nodeTemplate.getCapabilities().get(capabilityName).getProperties();
        }
        return properties;
    }

    private Map<String, AbstractPropertyValue> getInitialRequirementProperties(String requirementName, NodeTemplate nodeTemplate) {
        Map<String, AbstractPropertyValue> properties = Maps.newHashMap();
        if (nodeTemplate.getRequirements() != null && nodeTemplate.getRequirements().get(requirementName) != null
                && nodeTemplate.getRequirements().get(requirementName).getProperties() != null) {
            properties = nodeTemplate.getRequirements().get(requirementName).getProperties();
        }
        return properties;
    }

    /**
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
    public Map<String, PropertyValue> computeInputs(DeploymentTopology deploymentTopology, ApplicationEnvironment environment) {
        // initialize a map with input from the deployment setup
        Map<String, PropertyValue> inputs = Maps.newHashMap();
        if (!MapUtils.isEmpty(deploymentTopology.getInputProperties())) {
            inputs.putAll(deploymentTopology.getInputProperties());
        }

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
        prefixAndAddContextInput(deploymentTopology, inputs, InputsPreProcessorService.LOC_META, metaPropertiesValuesMap, true);

        // and the ones from the application meta-properties
        // meta or tags from application
        if (environment.getApplicationId() != null) {
            metaPropertiesValuesMap = Maps.newHashMap();
            Application application = applicationService.getOrFail(environment.getApplicationId());
            if (application.getMetaProperties() != null) {
                metaPropertiesValuesMap.putAll(application.getMetaProperties());
            }
            prefixAndAddContextInput(deploymentTopology, inputs, InputsPreProcessorService.APP_META, metaPropertiesValuesMap, true);

            Map<String, String> tags = TagUtil.tagListToMap(application.getTags());
            prefixAndAddContextInput(deploymentTopology, inputs, InputsPreProcessorService.APP_TAGS, tags, false);
        }

        return inputs;
    }

    /**
     * Add the context inputs to the actual deployment inputs by adding the given prefix to the key.
     *
     * @param deploymentTopology The deployment topology under processing (to know if the input exists and should be added).
     * @param inputs The inputs to be used for the topology deployment.
     * @param prefix The prefix to be added to the context inputs.
     * @param contextInputs The map of inputs from context elements (cloud, application, environment).
     */
    private void prefixAndAddContextInput(DeploymentTopology deploymentTopology, Map<String, PropertyValue> inputs, String prefix,
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
                addToInputs(deploymentTopology, inputs, prefix + configurationMap.get(contextInputEntry.getKey()).getName(), contextInputEntry.getValue());
            }
        } else {
            for (Map.Entry<String, String> contextInputEntry : contextInputs.entrySet()) {
                addToInputs(deploymentTopology, inputs, prefix + contextInputEntry.getKey(), contextInputEntry.getValue());
            }
        }
    }

    private void addToInputs(DeploymentTopology deploymentTopology, Map<String, PropertyValue> inputs, String inputKey, String value) {
        if (value != null && deploymentTopology.getInputProperties() != null && deploymentTopology.getInputs().containsKey(inputKey)) {
            inputs.put(inputKey, new ScalarPropertyValue(value));
        }
    }

    private void processGetInput(FunctionEvaluatorContext evaluatorContext, AbstractTemplate template, Map<String, AbstractPropertyValue> properties) {
        for (Map.Entry<String, AbstractPropertyValue> propEntry : safe(properties).entrySet()) {
            PropertyValue value = FunctionEvaluator.resolveValue(evaluatorContext, template, properties, propEntry.getValue());
            if (value != null) {
                propEntry.setValue(value);
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
