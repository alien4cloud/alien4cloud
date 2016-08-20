package alien4cloud.topology.validation;

import alien4cloud.component.CSARRepositorySearchService;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.CapabilityDefinition;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.utils.AlienUtils;
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
    private CSARRepositorySearchService csarRepoSearchService;
    @Resource
    private TopologyServiceCore topologyServiceCore;

    //
    public boolean isCapabilityUpperBoundReachedForTarget(String nodeTemplateName, Map<String, NodeTemplate> nodeTemplates, String capabilityName,
            Set<CSARDependency> dependencies) {
        NodeTemplate nodeTemplate = nodeTemplates.get(nodeTemplateName);
        checkCapability(capabilityName, nodeTemplate);
        IndexedNodeType relatedIndexedNodeType = csarRepoSearchService.getRequiredElementInDependencies(IndexedNodeType.class, nodeTemplate.getType(),
                dependencies);
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

    private void checkCapability(String capabilityName, NodeTemplate nodeTemplate) {
        if (!AlienUtils.safe(nodeTemplate.getCapabilities()).containsKey(capabilityName)) {
            throw new NotFoundException("A capability with name [" + capabilityName + "] cannot be found in the target node [" + nodeTemplate.getName() + "].");
        }
    }
}