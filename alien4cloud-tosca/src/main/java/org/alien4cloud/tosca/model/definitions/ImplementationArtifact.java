package org.alien4cloud.tosca.model.definitions;

import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;

/**
 * Specifies an implementation artifact for interfaces or operations of a {@link NodeType node type} or {@link RelationshipType relation type}.
 * 
 * @author luc boutier
 */
public class ImplementationArtifact extends AbstractArtifact {
    @Override
    public String toString() {
        return "ImplementationArtifact{} " + super.toString();
    }
}