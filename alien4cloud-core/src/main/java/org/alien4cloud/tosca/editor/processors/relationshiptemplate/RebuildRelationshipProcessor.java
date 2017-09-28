package org.alien4cloud.tosca.editor.processors.relationshiptemplate;

import java.util.Map;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.relationshiptemplate.RebuildRelationshipOperation;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.topology.TemplateBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * Process a {@link RebuildRelationshipOperation}.
 *
 * Rebuild a relationship template, synching it with the {@link RelationshipType}
 * present at the moment in the repository
 */
@Slf4j
@Component
public class RebuildRelationshipProcessor extends AbstractRelationshipProcessor<RebuildRelationshipOperation> {
    @Override
    protected void processRelationshipOperation(RebuildRelationshipOperation operation, NodeTemplate nodeTemplate, RelationshipTemplate relationshipTemplate) {
        Topology topology = EditionContextManager.getTopology();
        // rebuild a relationship template based on the current relationship type
        log.debug("Rebuilding the relationship <{}> in the node template <{}> of topology <{}> .", operation.getRelationshipName(), operation.getNodeName(),
                topology.getId());
        RelationshipType relType = ToscaContext.getOrFail(RelationshipType.class, relationshipTemplate.getType());
        Map<String, AbstractPropertyValue> properties = Maps.newHashMap();
        TemplateBuilder.fillProperties(properties, relType.getProperties(), relationshipTemplate.getProperties());
        relationshipTemplate.setProperties(properties);
        relationshipTemplate.setAttributes(relType.getAttributes());
    }
}