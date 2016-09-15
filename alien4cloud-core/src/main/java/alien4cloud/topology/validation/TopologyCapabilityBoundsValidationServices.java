package alien4cloud.topology.validation;

import org.alien4cloud.tosca.catalog.index.ToscaTypeSearchService;
import alien4cloud.exception.NotFoundException;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.definitions.CapabilityDefinition;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import alien4cloud.topology.TopologyServiceCore;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
@Component
public class TopologyCapabilityBoundsValidationServices {
    @Resource
    private ToscaTypeSearchService csarRepoSearchService;
    @Resource
    private TopologyServiceCore topologyServiceCore;

    //
    public boolean isCapabilityUpperBoundReachedForTarget(String nodeTemplateName, Map<String, NodeTemplate> nodeTemplates, String capabilityName,
            Set<CSARDependency> dependencies) {
        NodeTemplate nodeTemplate = nodeTemplates.get(nodeTemplateName);
        NodeType relatedIndexedNodeType = csarRepoSearchService.getRequiredElementInDependencies(NodeType.class, nodeTemplate.getType(),
                dependencies);
        chekCapability(nodeTemplateName, capabilityName, nodeTemplate);

        CapabilityDefinition capabilityDefinition = getCapabilityDefinition(relatedIndexedNodeType.getCapabilities(), capabilityName);
        if (capabilityDefinition.getUpperBound() == Integer.MAX_VALUE) {
            return false;
        }

        List<RelationshipTemplate> targetRelatedRelationships = topologyServiceCore.getTargetRelatedRelatonshipsTemplate(nodeTemplateName, nodeTemplates);
        if (targetRelatedRelationships == null || targetRelatedRelationships.isEmpty()) {
            return false;
        }

        int count = 0;
        for (RelationshipTemplate rel : targetRelatedRelationships) {
            if (rel.getTargetedCapabilityName().equals(capabilityName)) {
                count++;
            }
        }

        return count >= capabilityDefinition.getUpperBound();
    }

    private CapabilityDefinition getCapabilityDefinition(Collection<CapabilityDefinition> capabilityDefinitions, String capabilityName) {
        for (CapabilityDefinition capabilityDef : capabilityDefinitions) {
            if (capabilityDef.getId().equals(capabilityName)) {
                return capabilityDef;
            }
        }

        throw new NotFoundException("Capability definition [" + capabilityName + "] cannot be found");
    }

    private void chekCapability(String nodeTemplateName, String capabilityName, NodeTemplate nodeTemplate) {
        boolean capablityExists = false;
        if (nodeTemplate.getCapabilities() != null) {
            for (Map.Entry<String, Capability> capaEntry : nodeTemplate.getCapabilities().entrySet()) {
                if (capaEntry.getKey().equals(capabilityName)) {
                    capablityExists = true;
                }
            }
        }
        if (!capablityExists) {
            throw new NotFoundException("A capability with name [" + capabilityName + "] cannot be found in the target node [" + nodeTemplateName + "].");
        }
    }
}