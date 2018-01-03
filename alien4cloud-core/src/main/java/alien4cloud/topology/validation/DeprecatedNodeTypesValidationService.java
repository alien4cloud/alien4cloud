package alien4cloud.topology.validation;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.List;

import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.NodeType;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import alien4cloud.model.common.Tag;
import alien4cloud.topology.task.DeprecatedNodeTask;
import alien4cloud.tosca.context.ToscaContext;

/**
 * This validation service check if any node has a deprecated tag.
 */
@Service
public class DeprecatedNodeTypesValidationService {
    private static final String DEPRECATED = "deprecated";
    private static final String DEPRECATED_TRUE = "true";

    public List<DeprecatedNodeTask> validate(Topology topology) {
        List<DeprecatedNodeTask> taskList = Lists.newArrayList();
        topology.getNodeTemplates().forEach((nodeTemplateName, nodeTemplate) -> {
            NodeType type = ToscaContext.get(NodeType.class, nodeTemplate.getType());
            if (isDeprecated(type)) {
                taskList.add(new DeprecatedNodeTask(nodeTemplateName, type));
            }
        });
        return taskList;
    }

    private boolean isDeprecated(NodeType type) {
        for (Tag tag : safe(type.getTags())) {
            if (DEPRECATED.equals(tag.getName()) && DEPRECATED_TRUE.equals(tag.getValue())) {
                return true;
            }
        }
        return false;
    }
}