package alien4cloud.deployment;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.templates.AbstractTemplate;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.apache.commons.collections.MapUtils;
import org.elasticsearch.common.collect.Lists;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;

import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.orchestrators.services.OrchestratorDeploymentService;
import alien4cloud.tosca.context.ToscaContextual;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import alien4cloud.utils.InputArtifactUtil;
import alien4cloud.utils.PropertyUtil;
import alien4cloud.utils.services.ConstraintPropertyService;

@Service
public class DeploymentInputService {

    @Inject
    private ConstraintPropertyService constraintPropertyService;

    @Inject
    private OrchestratorDeploymentService orchestratorDeploymentService;

    /**
     * Fill-in the inputs properties definitions (and default values) based on the properties definitions from the topology.
     *
     * @param topology The deployment topology to impact.
     */
    @ToscaContextual
    public void processInputProperties(DeploymentTopology topology) {
        Map<String, PropertyValue> inputProperties = topology.getInputProperties();
        Map<String, PropertyDefinition> inputDefinitions = topology.getInputs();
        if (inputDefinitions == null || inputDefinitions.isEmpty()) {
            topology.setInputProperties(null);
        } else {
            if (inputProperties == null) {
                inputProperties = Maps.newHashMap();
                topology.setInputProperties(inputProperties);
            } else {
                // Ensure that previous defined values are still compatible with the latest input definition (as the topology may have changed).
                Iterator<Map.Entry<String, PropertyValue>> inputPropertyEntryIterator = inputProperties.entrySet().iterator();
                while (inputPropertyEntryIterator.hasNext()) {
                    Map.Entry<String, PropertyValue> inputPropertyEntry = inputPropertyEntryIterator.next();
                    if (!inputDefinitions.containsKey(inputPropertyEntry.getKey())) {
                        inputPropertyEntryIterator.remove();
                    } else {
                        try {
                            constraintPropertyService.checkPropertyConstraint(inputPropertyEntry.getKey(), inputPropertyEntry.getValue().getValue(),
                                    inputDefinitions.get(inputPropertyEntry.getKey()));
                        } catch (ConstraintViolationException | ConstraintValueDoNotMatchPropertyTypeException e) {
                            // Property is not valid anymore for the input, remove the old value
                            inputPropertyEntryIterator.remove();
                        }
                    }
                }
            }
            // set default values for every unset property.
            for (Map.Entry<String, PropertyDefinition> inputDefinitionEntry : inputDefinitions.entrySet()) {
                PropertyValue existingValue = inputProperties.get(inputDefinitionEntry.getKey());
                if (existingValue == null) {
                    // If user has not specified a value and there is
                    PropertyValue defaultValue = inputDefinitionEntry.getValue().getDefault();
                    if (defaultValue != null) {
                        inputProperties.put(inputDefinitionEntry.getKey(), defaultValue);
                    }
                }
            }
        }
    }

    private static void processInputArtifactForTemplate(Map<String, List<DeploymentArtifact>> artifactMap, AbstractTemplate template) {
        for (DeploymentArtifact da : template.getArtifacts().values()) {
            String inputArtifactId = InputArtifactUtil.getInputArtifactId(da);
            if (inputArtifactId != null) {
                List<DeploymentArtifact> das = artifactMap.get(inputArtifactId);
                if (das == null) {
                    das = Lists.newArrayList();
                    artifactMap.put(inputArtifactId, das);
                }
                das.add(da);
            }
        }
    }

    /**
     * Inject input artifacts in the corresponding nodes.
     */
    public void processInputArtifacts(DeploymentTopology topology) {
        if (topology.getInputArtifacts() != null && !topology.getInputArtifacts().isEmpty()) {

            // we'll build a map inputArtifactId -> List<DeploymentArtifact>
            Map<String, List<DeploymentArtifact>> artifactMap = Maps.newHashMap();
            // iterate over nodes in order to remember all nodes referencing an input artifact
            for (NodeTemplate nodeTemplate : topology.getNodeTemplates().values()) {
                if (MapUtils.isNotEmpty(nodeTemplate.getArtifacts())) {
                    processInputArtifactForTemplate(artifactMap, nodeTemplate);
                }
                if (MapUtils.isNotEmpty(nodeTemplate.getRelationships())) {
                    nodeTemplate.getRelationships().entrySet().stream()
                            .filter(relationshipTemplateEntry -> MapUtils.isNotEmpty(relationshipTemplateEntry.getValue().getArtifacts()))
                            .forEach(relationshipTemplateEntry -> processInputArtifactForTemplate(artifactMap, relationshipTemplateEntry.getValue()));
                }
            }

            Map<String, DeploymentArtifact> allInputArtifact = new HashMap<>();
            allInputArtifact.putAll(topology.getInputArtifacts());
            if (MapUtils.isNotEmpty(topology.getUploadedInputArtifacts())) {
                allInputArtifact.putAll(topology.getUploadedInputArtifacts());
            }
            for (Map.Entry<String, DeploymentArtifact> inputArtifact : allInputArtifact.entrySet()) {
                List<DeploymentArtifact> nodeArtifacts = artifactMap.get(inputArtifact.getKey());
                if (nodeArtifacts != null) {
                    for (DeploymentArtifact nodeArtifact : nodeArtifacts) {
                        nodeArtifact.setArtifactRef(inputArtifact.getValue().getArtifactRef());
                        nodeArtifact.setArtifactName(inputArtifact.getValue().getArtifactName());
                        nodeArtifact.setArtifactRepository(inputArtifact.getValue().getArtifactRepository());
                        nodeArtifact.setRepositoryName(inputArtifact.getValue().getRepositoryName());
                        nodeArtifact.setRepositoryCredential(inputArtifact.getValue().getRepositoryCredential());
                        nodeArtifact.setRepositoryURL(inputArtifact.getValue().getRepositoryURL());
                        nodeArtifact.setArchiveName(inputArtifact.getValue().getArchiveName());
                        nodeArtifact.setArchiveVersion(inputArtifact.getValue().getArchiveVersion());
                    }
                }
            }
        }
    }

    /**
     * Process default deployment properties
     *
     * @param deploymentTopology the deployment setup to generate configuration for
     */
    public void processProviderDeploymentProperties(DeploymentTopology deploymentTopology) {
        if (deploymentTopology.getOrchestratorId() == null) {
            // No orchestrator assigned for the topology do nothing
            return;
        }
        Map<String, PropertyDefinition> propertyDefinitionMap = orchestratorDeploymentService
                .getDeploymentPropertyDefinitions(deploymentTopology.getOrchestratorId());
        if (propertyDefinitionMap != null) {
            // Reset deployment properties as it might have changed between cloud
            Map<String, String> propertyValueMap = deploymentTopology.getProviderDeploymentProperties();
            if (propertyValueMap == null) {
                propertyValueMap = Maps.newHashMap();
            } else {
                Iterator<Map.Entry<String, String>> propertyValueMapIterator = propertyValueMap.entrySet().iterator();
                while (propertyValueMapIterator.hasNext()) {
                    Map.Entry<String, String> entry = propertyValueMapIterator.next();
                    if (!propertyDefinitionMap.containsKey(entry.getKey())) {
                        // Remove the mapping if topology do not contain the node with that name and of type compute
                        // Or the mapping do not exist anymore in the match result
                        propertyValueMapIterator.remove();
                    }
                }
            }
            for (Map.Entry<String, PropertyDefinition> propertyDefinitionEntry : propertyDefinitionMap.entrySet()) {
                String existingValue = propertyValueMap.get(propertyDefinitionEntry.getKey());
                if (existingValue != null) {
                    try {
                        constraintPropertyService.checkSimplePropertyConstraint(propertyDefinitionEntry.getKey(), existingValue,
                                propertyDefinitionEntry.getValue());
                    } catch (ConstraintViolationException | ConstraintValueDoNotMatchPropertyTypeException e) {
                        PropertyUtil.setScalarDefaultValueOrNull(propertyValueMap, propertyDefinitionEntry.getKey(),
                                propertyDefinitionEntry.getValue().getDefault());
                    }
                } else {
                    PropertyUtil.setScalarDefaultValueIfNotNull(propertyValueMap, propertyDefinitionEntry.getKey(),
                            propertyDefinitionEntry.getValue().getDefault());
                }
            }
            deploymentTopology.setProviderDeploymentProperties(propertyValueMap);
        }
    }

}
