package alien4cloud.topology.validation;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.elasticsearch.common.collect.Sets;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import alien4cloud.exception.NotFoundException;
import alien4cloud.topology.task.AbstractRelationshipTask;
import alien4cloud.topology.task.TaskCode;

/**
 * Performs validation by checking that no relationships in a topology are abstract (and cannot be instanciated).
 */
@Component
public class TopologyAbstractRelationshipValidationService {
    @Resource
    private IToscaTypeSearchService csarRepoSearchService;

    /**
     * Checks that no relationships in a topology are abstract (and cannot be instanciated).
     *
     * @param topology The topology to validate.
     * @return A list tasks to be done to make this topology valid.
     */
    public List<AbstractRelationshipTask> validateAbstractRelationships(Topology topology) {
        Map<String, RelationshipType[]> abstractIndexedRelationshipTypes = getIndexedRelationshipTypesFromTopology(topology, true);
        return getTaskListFromMapArray(abstractIndexedRelationshipTypes, TaskCode.IMPLEMENT_RELATIONSHIP);
    }

    /**
     * Get the relationships from a topology
     *
     * @param topology topology to be validated
     * @param abstractOnes if only abstract ones should be retrieved
     * @return a map containing node template id --> list of relationship type that this node references
     */
    private Map<String, RelationshipType[]> getIndexedRelationshipTypesFromTopology(Topology topology, Boolean abstractOnes) {
        Map<String, RelationshipType[]> indexedRelationshipTypesMap = Maps.newHashMap();
        if (topology.getNodeTemplates() == null) {
            return indexedRelationshipTypesMap;
        }
        for (Map.Entry<String, NodeTemplate> template : topology.getNodeTemplates().entrySet()) {
            if (template.getValue().getRelationships() == null) {
                continue;
            }

            Set<RelationshipType> indexedRelationshipTypes = Sets.newHashSet();
            for (RelationshipTemplate relTemplate : template.getValue().getRelationships().values()) {
                RelationshipType indexedRelationshipType = csarRepoSearchService.getElementInDependencies(RelationshipType.class, relTemplate.getType(),
                        topology.getDependencies());
                if (indexedRelationshipType != null) {
                    if (abstractOnes == null || abstractOnes.equals(indexedRelationshipType.isAbstract())) {
                        indexedRelationshipTypes.add(indexedRelationshipType);
                    }
                } else {
                    throw new NotFoundException("Relationship Type [" + relTemplate.getType() + "] cannot be found");
                }
            }
            if (indexedRelationshipTypes.size() > 0) {
                indexedRelationshipTypesMap.put(template.getKey(), indexedRelationshipTypes.toArray(new RelationshipType[indexedRelationshipTypes.size()]));
            }

        }
        return indexedRelationshipTypesMap;
    }

    /**
     * Constructs a TopologyTask list given a Map (node template name => component) and the code
     */
    private <T extends AbstractInheritableToscaType> List<AbstractRelationshipTask> getTaskListFromMapArray(Map<String, T[]> components, TaskCode taskCode) {
        List<AbstractRelationshipTask> taskList = Lists.newArrayList();
        for (Map.Entry<String, T[]> entry : components.entrySet()) {
            for (AbstractInheritableToscaType compo : entry.getValue()) {
                AbstractRelationshipTask task = new AbstractRelationshipTask();
                task.setNodeTemplateName(entry.getKey());
                task.setComponent(compo);
                task.setCode(taskCode);
                taskList.add(task);
            }
        }
        if (taskList.isEmpty()) {
            return null;
        } else {
            return taskList;
        }
    }
}
