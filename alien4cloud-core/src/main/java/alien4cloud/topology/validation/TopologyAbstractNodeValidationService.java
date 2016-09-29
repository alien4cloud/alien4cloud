package alien4cloud.topology.validation;

import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.templates.Topology;
import alien4cloud.topology.TopologyService;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.topology.task.SuggestionsTask;
import com.google.common.collect.Maps;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Performs validation by checking that no nodes in a topology are abstract (and cannot be instanciated).
 */
@Component
public class TopologyAbstractNodeValidationService {
    @Resource
    private TopologyServiceCore topologyServiceCore;

    @Resource
    private TopologyService topologyService;

    /**
     * Find replacements components for abstract nodes in a Topology
     */
    @SneakyThrows({ IOException.class })
    public List<SuggestionsTask> findReplacementForAbstracts(Topology topology) {
        Map<String, NodeType> nodeTempNameToAbstractIndexedNodeTypes = topologyServiceCore.getIndexedNodeTypesFromTopology(topology, true, true, true);
        Map<String, Map<String, Set<String>>> nodeTemplatesToFilters = Maps.newHashMap();
        for (Map.Entry<String, NodeType> idntEntry : nodeTempNameToAbstractIndexedNodeTypes.entrySet()) {
            topologyService.processNodeTemplate(topology, Maps.immutableEntry(idntEntry.getKey(), topology.getNodeTemplates().get(idntEntry.getKey())),
                    nodeTemplatesToFilters);
        }
        return topologyService.searchForNodeTypes(nodeTemplatesToFilters, nodeTempNameToAbstractIndexedNodeTypes);
    }
}
