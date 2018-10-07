package org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching;

import static alien4cloud.utils.AlienConstants.GROUP_ALL;
import static alien4cloud.utils.AlienUtils.safe;
import static org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext.DEPLOYMENT_LOCATIONS_MAP_CACHE_KEY;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.ServiceNodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.normative.constants.NormativeCapabilityTypes;
import org.alien4cloud.tosca.utils.ToscaTypeUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.model.service.ServiceResource;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.utils.CollectionUtils;

/**
 * This is the last node matching modifier, it actually applies the configured substitutions to the topology by merging the node provided from the location and
 * the one in the topology.
 */
@Component
public class NodeMatchingReplaceModifier extends AbstractMatchingReplaceModifier<NodeTemplate, LocationResourceTemplate, NodeType> {

    /**
     * Add locations dependencies
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void init(Topology topology, FlowExecutionContext context) {
        Location location = ((Map<String, Location>) context.getExecutionCache().get(DEPLOYMENT_LOCATIONS_MAP_CACHE_KEY)).get(GROUP_ALL);
        topology.getDependencies().addAll(location.getDependencies());
        ToscaContext.get().resetDependencies(topology.getDependencies());
    }

    @Override
    protected Class<NodeType> getToscaTypeClass() {
        return NodeType.class;
    }

    @Override
    protected String getOriginalTemplateCacheKey() {
        return FlowExecutionContext.MATCHING_ORIGINAL_NODES;
    }

    @Override
    protected String getReplacedTemplateCacheKey() {
        return FlowExecutionContext.MATCHING_REPLACED_NODES;
    }

    @Override
    protected Map<String, LocationResourceTemplate> getMatchesById(FlowExecutionContext context) {
        return (Map<String, LocationResourceTemplate>) context.getExecutionCache().get(FlowExecutionContext.MATCHED_NODE_LOCATION_TEMPLATES_BY_ID_MAP);
    }

    @Override
    protected Map<String, String> getUserMatches(DeploymentMatchingConfiguration matchingConfiguration) {
        return matchingConfiguration.getMatchedLocationResources();
    }

    @Override
    protected LocationResourceTemplate getLocationResourceTemplateCopy(String locationResourceTemplateId) {
        return getLocationResourceService().getOrFail(locationResourceTemplateId);
    }

    @Override
    protected Map<String, NodeTemplate> getTopologyTemplates(Topology topology) {
        return topology.getNodeTemplates();
    }

    @Override
    protected void processSpecificReplacement(NodeTemplate replacingNode, NodeTemplate replacedTopologyNode, Set<String> topologyNotMergedProps) {
        // Also merge relationships
        replacingNode.setRelationships(replacedTopologyNode.getRelationships());
        // The location node is the node from orchestrator which must be a child type of the abstract topology node so should loop on this node to do
        // not miss any capability
        for (Map.Entry<String, Capability> locationCapabilityEntry : safe(replacingNode.getCapabilities()).entrySet()) {
            // Merge capabilities properties from the topology into the substituted node un-set properties
            CapabilityType capabilityType = ToscaContext.get(CapabilityType.class, locationCapabilityEntry.getValue().getType());

            Capability locationCapability = locationCapabilityEntry.getValue();
            Capability abstractCapability = safe(replacedTopologyNode.getCapabilities()).get(locationCapabilityEntry.getKey());

            // Ignore injection of location values for scalable capability
            if (abstractCapability != null && MapUtils.isNotEmpty(abstractCapability.getProperties())) {
                if (capabilityType != null && !ToscaTypeUtils.isOfType(capabilityType, NormativeCapabilityTypes.SCALABLE)) {
                    locationCapability.setProperties(
                            CollectionUtils.merge(abstractCapability.getProperties(), locationCapability.getProperties(), true, topologyNotMergedProps));
                } else {
                    locationCapability.setProperties(abstractCapability.getProperties());
                }
            }
        }
        // merge artifacts when needed
        if (replacedTopologyNode.getArtifacts() != null) {
            replacedTopologyNode.getArtifacts().forEach((key, deploymentArtifact) -> {
                Map<String, DeploymentArtifact> targetArtifacts = replacingNode.getArtifacts();
                if (targetArtifacts == null) {
                    targetArtifacts = Maps.newHashMap();
                    replacingNode.setArtifacts(targetArtifacts);
                }
                if (replacingNode.getArtifacts().containsKey(key)) {
                    replacingNode.getArtifacts().put(key, deploymentArtifact);
                }
            });
        }
    }

    @Override
    protected void processServiceResourceReplacement(Topology topology, Map<String, NodeTemplate> topologyTemplateMap, String nodeId,
            String serviceResourceId) {
        ServiceResource serviceResource = getServiceResourceService().getOrFail(serviceResourceId);
        NodeTemplate serviceNodeTemplate = serviceResource.getNodeInstance().getNodeTemplate();
        ServiceNodeTemplate substitutionNodeTemplate = new ServiceNodeTemplate(serviceNodeTemplate.getType(), serviceNodeTemplate.getProperties(),
                serviceNodeTemplate.getAttributes(), serviceNodeTemplate.getRelationships(), serviceNodeTemplate.getRequirements(),
                serviceNodeTemplate.getCapabilities(), serviceNodeTemplate.getInterfaces(), serviceNodeTemplate.getArtifacts());

        substitutionNodeTemplate.setServiceResourceId(serviceResource.getId());
        substitutionNodeTemplate.setAttributeValues(serviceResource.getNodeInstance().getAttributeValues());
        NodeTemplate abstractTopologyNode = topologyTemplateMap.put(nodeId, substitutionNodeTemplate);
        substitutionNodeTemplate.setName(abstractTopologyNode.getName());
        substitutionNodeTemplate.setRelationships(abstractTopologyNode.getRelationships());

        // add all the necessary dependencies to the topology
        Csar csar = getToscaTypeSearchService().getArchive(serviceResource.getDependency().getName(), serviceResource.getDependency().getVersion());
        Set<CSARDependency> dependencies = Sets.newHashSet();
        if (csar.getDependencies() != null) {
            dependencies.addAll(csar.getDependencies());
        }
        dependencies.add(new CSARDependency(csar.getName(), csar.getVersion()));
        topology.getDependencies().addAll(dependencies);
    }
}
