package alien4cloud.tosca.parser.postprocess;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.tosca.context.ToscaContext;

/**
 * Relationship must be processed after a pre-processing of all nodes (to inject capabilities/requirements from type).
 */
@Component
public class NodeTemplateRelationshipPostProcessor implements IPostProcessor<NodeTemplate> {
    @Resource
    private RelationshipPostProcessor relationshipPostProcessor;

    @Override
    public void process(NodeTemplate instance) {
        final IndexedNodeType nodeType = ToscaContext.get(IndexedNodeType.class, instance.getType());
        if (nodeType == null) {
            return; // error managed by the reference post processor.
        }
        Map<String, RelationshipTemplate> updated = Maps.newLinkedHashMap();
        safe(instance.getRelationships()).entrySet().stream().forEach(entry -> {
            relationshipPostProcessor.process(nodeType, entry);
            updated.put(buildRelationShipTemplateName(entry.getValue()), entry.getValue());
        });
        instance.setRelationships(updated);
    }

    private String buildRelationShipTemplateName(RelationshipTemplate relationshipTemplate) {
        String value = relationshipTemplate.getType();
        if (value.contains(".")) {
            value = value.substring(value.lastIndexOf(".") + 1);
        }
        value = StringUtils.uncapitalize(value);
        value = value + StringUtils.capitalize(relationshipTemplate.getTarget());
        return value;
    }
}