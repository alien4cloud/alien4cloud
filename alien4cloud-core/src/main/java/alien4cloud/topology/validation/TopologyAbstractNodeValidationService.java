package alien4cloud.topology.validation;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import com.google.common.collect.Maps;

import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration.ResourceMatching;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.NodeType;
import org.springframework.stereotype.Component;

import alien4cloud.topology.TopologyService;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.topology.task.SuggestionsTask;
import lombok.SneakyThrows;

/**
 * Performs validation by checking that no nodes in a deployment topology are abstract (and cannot be instantiated).
 */
@Component
public class TopologyAbstractNodeValidationService {
    @Resource
    private TopologyServiceCore topologyServiceCore;

    @Resource
    private TopologyService topologyService;

    /**
     * Build user error messages (contained into SuggestionsTask) if an abstract node is not substituted
     */
    @SneakyThrows({ IOException.class })
    public List<SuggestionsTask> findReplacementForAbstracts(Topology topology, Map<String, ResourceMatching> substitutedNodes) {
        Map<String, NodeType> nodeTempNameToAbstractIndexedNodeTypes = topologyServiceCore.getIndexedNodeTypesFromTopology(topology, true, true, true);

        // node type can be abstract if its substitute is a service (a service CANNOT be registered with a concrete type in A4C)
        exclude(nodeTempNameToAbstractIndexedNodeTypes, substitutedNodes);

        Map<String, Map<String, Set<String>>> nodeTemplatesToFilters = Maps.newHashMap();
        for (Map.Entry<String, NodeType> idntEntry : nodeTempNameToAbstractIndexedNodeTypes.entrySet()) {
            topologyService.processNodeTemplate(topology, Maps.immutableEntry(idntEntry.getKey(), topology.getNodeTemplates().get(idntEntry.getKey())),
                    nodeTemplatesToFilters);
        }
        return topologyService.searchForNodeTypes(topology.getWorkspace(), nodeTemplatesToFilters, nodeTempNameToAbstractIndexedNodeTypes, true);
    }

    private void exclude(Map<String, NodeType> nodeTempNameToAbstractIndexedNodeTypes, Map<String, ResourceMatching> substitutedNodes) {
        if (substitutedNodes == null) {
            return;
        }
        substitutedNodes.forEach((k, v) -> {
            nodeTempNameToAbstractIndexedNodeTypes.remove(k);
        });
    }
}
