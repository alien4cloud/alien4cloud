package org.alien4cloud.tosca.model.definitions;

import java.util.List;

import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Specifies an implementation artifact for interfaces or operations of a {@link NodeType node type} or {@link RelationshipType relation type}.
 */
@Getter
@Setter
@NoArgsConstructor
public class ImplementationArtifact extends AbstractArtifact {
    /**
     * Constructor is used to create an artifact out of the artifact reference. This is used when parsing short notation.
     *
     * @param artifactRef The reference of the artifact within the archive.
     */
    public ImplementationArtifact(String artifactRef) {
        super(artifactRef);
    }

    @Override
    public String toString() {
        return "ImplementationArtifact{} " + super.toString();
    }
}