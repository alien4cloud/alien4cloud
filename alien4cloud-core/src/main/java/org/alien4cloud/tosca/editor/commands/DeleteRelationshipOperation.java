package org.alien4cloud.tosca.editor.commands;

import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.model.topology.Topology;
import lombok.Getter;
import lombok.Setter;

/**
 * Operation to delete a {@link RelationshipTemplate} from a {@link NodeTemplate} in a {@link Topology}.
 */
@Getter
@Setter
public class DeleteRelationshipOperation extends AbstractEditorOperation {
    private String nodeTemplateName;
    private String relationshipTemplateName;
}
