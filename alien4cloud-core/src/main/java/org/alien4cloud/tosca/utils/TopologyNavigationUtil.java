package org.alien4cloud.tosca.utils;

import static alien4cloud.utils.AlienUtils.safe;

import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.normative.constants.NormativeRelationshipConstants;

import alien4cloud.tosca.context.ToscaContext;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for navigation purpose operations on node templates.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TopologyNavigationUtil {

    /**
     * Utility method to find the immediate host template of the current given template.
     * 
     * @param template The template for wich to get the immediate host.
     * @return
     */
    public static NodeTemplate getImmediateHostTemplate(Topology topology, NodeTemplate template) {
        RelationshipTemplate host = getRelationshipFromType(template, NormativeRelationshipConstants.HOSTED_ON);
        if (host == null) {
            return null;
        }
        return topology.getNodeTemplates().get(host.getTarget());
    }

    /**
     * Get all incoming relationships of a node that match the requested type.
     * 
     * @param template The node template for which to get relationships.
     * @param type The expected type.
     * @return
     */
    public static RelationshipTemplate getRelationshipFromType(NodeTemplate template, String type) {
        for (RelationshipTemplate relationshipTemplate : safe(template.getRelationships()).values()) {
            RelationshipType relationshipType = ToscaContext.getOrFail(RelationshipType.class, relationshipTemplate.getType());
            if (relationshipType != null && (relationshipType.getElementId().equals(type) || relationshipType.getDerivedFrom().contains(type))) {
                return relationshipTemplate;
            }
        }
        return null;
    }
}
