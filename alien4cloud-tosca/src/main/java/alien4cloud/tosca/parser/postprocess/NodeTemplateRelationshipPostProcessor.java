package alien4cloud.tosca.parser.postprocess;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
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
        final NodeType nodeType = ToscaContext.get(NodeType.class, instance.getType());
        if (nodeType == null) {
            return; // error managed by the reference post processor.
        }
        Map<String, RelationshipTemplate> updated = Maps.newLinkedHashMap();
        safe(instance.getRelationships()).entrySet().stream().forEach(entry -> {
            relationshipPostProcessor.process(nodeType, entry);
            String relationshipTemplateName = buildRelationShipTemplateName(entry.getValue());
            updated.put(getUniqueKey(updated, relationshipTemplateName), entry.getValue());
            entry.getValue().setName(relationshipTemplateName);
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

    private String getUniqueKey(Map<String, ?> map, String key) {
        int increment = 0;
        String uniqueKey = key;
        while (map.containsKey(uniqueKey)) {
            uniqueKey = key + "_" + increment;
            increment++;
        }
        return uniqueKey;
    }
}