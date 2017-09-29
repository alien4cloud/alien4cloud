package org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.alm.service.ServiceResourceService;
import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.ServiceNodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.apache.commons.collections4.MapUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.model.service.ServiceResource;
import alien4cloud.orchestrators.locations.services.ILocationResourceService;
import alien4cloud.topology.task.LocationPolicyTask;
import alien4cloud.utils.CollectionUtils;

/**
 * This is the last node matching modifier, it actually applies the configured substitutions to the topology by merging the node provided from the location and
 * the one in the topology.
 */
@Component
public class NodeMatchingReplaceModifier implements ITopologyModifier {
    @Inject
    @Lazy(true)
    private ILocationResourceService locationResourceService;
    @Inject
    private ServiceResourceService serviceResourceService;
    @Inject
    private IToscaTypeSearchService toscaTypeSearchService;

    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        Optional<DeploymentMatchingConfiguration> configurationOptional = context.getConfiguration(DeploymentMatchingConfiguration.class,
                NodeMatchingReplaceModifier.class.getSimpleName());

        if (!configurationOptional.isPresent()) { // we should not end-up here as location matching should be processed first
            context.log().error(new LocationPolicyTask());
            return;
        }

        DeploymentMatchingConfiguration matchingConfiguration = configurationOptional.get();
        Map<String, LocationResourceTemplate> allAvailableResourceTemplate = (Map<String, LocationResourceTemplate>) context.getExecutionCache()
                .get(FlowExecutionContext.MATCHED_LOCATION_RESOURCE_TEMPLATES);

        Map<String, String> lastUserSubstitutions = matchingConfiguration.getMatchedLocationResources();

        Map<String, NodeTemplate> originalNodes = Maps.newHashMap();
        // Now modify the topology to replace nodes with the one selected during matching
        for (Map.Entry<String, String> substitutedNodeEntry : lastUserSubstitutions.entrySet()) {
            // Substitute the node template of the topology by those matched
            String nodeId = substitutedNodeEntry.getKey();
            String substitutionTemplateId = substitutedNodeEntry.getValue();
            originalNodes.put(nodeId, topology.getNodeTemplates().get(nodeId));
            processNodeSubstitution(topology, allAvailableResourceTemplate, nodeId, substitutionTemplateId);
        }
        context.getExecutionCache().put(FlowExecutionContext.MATCHING_ORIGINAL_NODES, originalNodes);

    }

    public void processNodeSubstitution(Topology topology, Map<String, LocationResourceTemplate> allAvailableResourceTemplate, String nodeId,
            String substitutionTemplateId) {
        LocationResourceTemplate substitutionTemplate = allAvailableResourceTemplate.get(substitutionTemplateId);
        if (substitutionTemplate.isService()) {
            // it's a service
            processServiceResourceSubstitution(nodeId, substitutionTemplateId, topology);
        } else {
            // it's a real location resource template
            processLocationResourceTemplateSubstitution(nodeId, substitutionTemplateId, topology);
        }
    }

    /**
     * In this {@link alien4cloud.model.deployment.DeploymentTopology}, proceed the substitution of the given node using the given service.
     */
    private void processServiceResourceSubstitution(String nodeId, String serviceResourceId, Topology topology) {
        ServiceResource serviceResource = serviceResourceService.getOrFail(serviceResourceId);
        NodeTemplate serviceNodeTemplate = serviceResource.getNodeInstance().getNodeTemplate();
        ServiceNodeTemplate substitutionNodeTemplate = new ServiceNodeTemplate(serviceNodeTemplate.getType(), serviceNodeTemplate.getProperties(),
                serviceNodeTemplate.getAttributes(), serviceNodeTemplate.getRelationships(), serviceNodeTemplate.getRequirements(),
                serviceNodeTemplate.getCapabilities(), serviceNodeTemplate.getInterfaces(), serviceNodeTemplate.getArtifacts());

        substitutionNodeTemplate.setServiceResourceId(serviceResource.getId());
        substitutionNodeTemplate.setAttributeValues(serviceResource.getNodeInstance().getAttributeValues());
        NodeTemplate abstractTopologyNode = topology.getNodeTemplates().put(nodeId, substitutionNodeTemplate);
        substitutionNodeTemplate.setName(abstractTopologyNode.getName());
        substitutionNodeTemplate.setRelationships(abstractTopologyNode.getRelationships());

        // add all the necessary dependencies to the topology
        Csar csar = toscaTypeSearchService.getArchive(serviceResource.getDependency().getName(), serviceResource.getDependency().getVersion());
        Set<CSARDependency> dependencies = Sets.newHashSet();
        if (csar.getDependencies() != null) {
            dependencies.addAll(csar.getDependencies());
        }
        dependencies.add(new CSARDependency(csar.getName(), csar.getVersion()));
        topology.getDependencies().addAll(dependencies);
    }

    private void processLocationResourceTemplateSubstitution(String nodeId, String locationResourceTemplateId, Topology topology) {
        // Fetching a new node copy from elasticsearch avoid later issues if the same subtituted node is used in multiple templates (shared maps, shallow copies
        // etc.)
        NodeTemplate locationNode = locationResourceService.getOrFail(locationResourceTemplateId).getTemplate();
        // Substitute the node in the topology with the location provided implementation.
        NodeTemplate abstractTopologyNode = topology.getNodeTemplates().put(nodeId, locationNode);
        // Merge name, properties and capability properties
        locationNode.setName(abstractTopologyNode.getName());
        // Also merge relationships
        locationNode.setRelationships(abstractTopologyNode.getRelationships());
        // TODO Log all properties defined in the topology but not merged into the final node
        Set<String> topologyNotMergedProps = Sets.newHashSet();
        // Merge properties from the topology node but prevent any override.
        locationNode.setProperties(CollectionUtils.merge(abstractTopologyNode.getProperties(), locationNode.getProperties(), true, topologyNotMergedProps));
        // The location node is the node from orchestrator which must be a child type of the abstract topology node so should loop on this node to do
        // not miss any capability
        for (Map.Entry<String, Capability> locationCapabilityEntry : safe(locationNode.getCapabilities()).entrySet()) {
            // Merge capabilities properties from the topology into the substituted node un-set properties
            Capability locationCapability = locationCapabilityEntry.getValue();
            Capability abstractCapability = safe(abstractTopologyNode.getCapabilities()).get(locationCapabilityEntry.getKey());
            if (abstractCapability != null && MapUtils.isNotEmpty(abstractCapability.getProperties())) {
                locationCapability.setProperties(
                        CollectionUtils.merge(abstractCapability.getProperties(), locationCapability.getProperties(), true, topologyNotMergedProps));
            }
        }
    }
}
