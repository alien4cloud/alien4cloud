package org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Map;
import java.util.Set;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.ServiceNodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.model.service.ServiceResource;
import alien4cloud.utils.CollectionUtils;

/**
 * This is the last node matching modifier, it actually applies the configured substitutions to the topology by merging the node provided from the location and
 * the one in the topology.
 */
@Component
public class NodeMatchingReplaceModifier extends AbstractMatchingReplaceModifier<NodeTemplate, LocationResourceTemplate> {

    @Override
    protected String getOriginalTemplateCacheKey() {
        return FlowExecutionContext.MATCHING_ORIGINAL_NODES;
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
    protected void processSpecificReplacement(NodeTemplate replacingNode, NodeTemplate replacedTopologyNode,
                                              Set<String> topologyNotMergedProps) {
        // Also merge relationships
        replacingNode.setRelationships(replacedTopologyNode.getRelationships());
        // The location node is the node from orchestrator which must be a child type of the abstract topology node so should loop on this node to do
        // not miss any capability
        for (Map.Entry<String, Capability> locationCapabilityEntry : safe(replacingNode.getCapabilities()).entrySet()) {
            // Merge capabilities properties from the topology into the substituted node un-set properties
            Capability locationCapability = locationCapabilityEntry.getValue();
            Capability abstractCapability = safe(replacedTopologyNode.getCapabilities()).get(locationCapabilityEntry.getKey());
            if (abstractCapability != null && MapUtils.isNotEmpty(abstractCapability.getProperties())) {
                locationCapability.setProperties(
                        CollectionUtils.merge(abstractCapability.getProperties(), locationCapability.getProperties(), true, topologyNotMergedProps));
            }
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
