package alien4cloud.tosca.parser.postprocess;

import static alien4cloud.utils.AlienUtils.safe;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.topology.NodeTemplate;
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
        safe(instance.getRelationships()).entrySet().stream().forEach(entry -> relationshipPostProcessor.process(nodeType, entry));
    }
}