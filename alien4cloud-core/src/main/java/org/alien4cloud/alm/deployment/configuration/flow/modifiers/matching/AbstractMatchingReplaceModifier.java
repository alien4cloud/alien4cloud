package org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.alm.service.ServiceResourceService;
import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.model.templates.AbstractTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.context.annotation.Lazy;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.model.orchestrators.locations.AbstractLocationResourceTemplate;
import alien4cloud.orchestrators.locations.services.ILocationResourceService;
import alien4cloud.topology.task.LocationPolicyTask;
import alien4cloud.utils.CollectionUtils;
import lombok.Getter;
import lombok.Setter;

/**
 * Replace a template with a matched template.
 */
@Getter
@Setter
public abstract class AbstractMatchingReplaceModifier<T extends AbstractTemplate, V extends AbstractLocationResourceTemplate<T>> implements ITopologyModifier {
    @Inject
    @Lazy
    private ILocationResourceService locationResourceService;
    @Inject
    private ServiceResourceService serviceResourceService;
    @Inject
    private IToscaTypeSearchService toscaTypeSearchService;

    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        Optional<DeploymentMatchingConfiguration> configurationOptional = context.getConfiguration(DeploymentMatchingConfiguration.class,
                this.getClass().getSimpleName());

        if (!configurationOptional.isPresent()) { // we should not end-up here as location matching should be processed first
            context.log().error(new LocationPolicyTask());
            return;
        }

        DeploymentMatchingConfiguration matchingConfiguration = configurationOptional.get();

        Map<String, String> lastUserSubstitutions = getUserMatches(matchingConfiguration);
        Map<String, V> matchesById = getMatchesById(context);

        Map<String, T> topologyTemplateMap = getTopologyTemplates(topology);

        Map<String, T> originalTemplates = Maps.newHashMap();
        // Now modify the topology to replace nodes with the one selected during matching
        for (Map.Entry<String, String> substitutedNodeEntry : lastUserSubstitutions.entrySet()) {
            // Substitute the node template of the topology by those matched
            String templateId = substitutedNodeEntry.getKey();
            String matchedLocationResourceId = substitutedNodeEntry.getValue();
            originalTemplates.put(templateId, topologyTemplateMap.get(templateId));
            processReplacement(topology, topologyTemplateMap, matchesById, templateId, matchedLocationResourceId);
        }
        context.getExecutionCache().put(getOriginalTemplateCacheKey(), originalTemplates);
    }

    public void processReplacement(Topology topology, Map<String, T> topologyTemplateMap, Map<String, V> allAvailableResourceTemplate, String nodeId,
            String substitutionTemplateId) {
        V selectedMatchedTemplate = allAvailableResourceTemplate.get(substitutionTemplateId);
        if (selectedMatchedTemplate.isService()) {
            // it's a service - only for node substitution.
            processServiceResourceReplacement(topology, topologyTemplateMap, nodeId, substitutionTemplateId);
        } else {
            // it's a real location resource template
            processSpecificReplacement(topologyTemplateMap, nodeId, substitutionTemplateId);
        }
    }

    private void processSpecificReplacement(Map<String, T> topologyTemplateMap, String nodeId, String locationResourceTemplateId) {
        // Fetching a new node copy from elasticsearch avoid later issues if the same subtituted node is used in multiple templates (shared maps, shallow copies
        // etc.)
        V resourceTemplate = getLocationResourceTemplateCopy(locationResourceTemplateId);
        T replacingNode = resourceTemplate.getTemplate();
        // Substitute the node in the topology with the location provided implementation.
        T replacedTopologyNode = topologyTemplateMap.put(nodeId, replacingNode);
        // Merge name, properties and capability properties
        replacingNode.setName(replacedTopologyNode.getName());
        // TODO Log all properties defined in the topology but not merged into the final node
        Set<String> topologyNotMergedProps = Sets.newHashSet();
        // Merge properties from the topology node but prevent any override.
        replacingNode.setProperties(CollectionUtils.merge(replacedTopologyNode.getProperties(), replacingNode.getProperties(), true, topologyNotMergedProps));

        processSpecificReplacement(replacingNode, replacedTopologyNode, topologyNotMergedProps);
    }

    protected abstract String getOriginalTemplateCacheKey();

    protected abstract Map<String, V> getMatchesById(FlowExecutionContext context);

    protected abstract Map<String, String> getUserMatches(DeploymentMatchingConfiguration matchingConfiguration);

    protected abstract Map<String, T> getTopologyTemplates(Topology topology);

    protected abstract V getLocationResourceTemplateCopy(String locationResourceTemplateId);

    protected abstract void processSpecificReplacement(T replacingNode, T replacedTopologyNode, Set<String> topologyNotMergedProps);

    protected void processServiceResourceReplacement(Topology topology, Map<String, T> topologyTemplateMap, String nodeId, String serviceResourceId) {
        // No default implementation
    }
}
