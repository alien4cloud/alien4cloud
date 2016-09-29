package org.alien4cloud.tosca.editor.operations.relationshiptemplate;

import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import lombok.Getter;
import lombok.Setter;

/**
 * Operation to delete a {@link RelationshipTemplate} from a {@link NodeTemplate} in a {@link Topology}.
 */
@Getter
@Setter
public class DeleteRelationshipOperation extends AbstractRelationshipOperation {
    @Override
    public String commitMessage() {
        return "delete relationship with name <" + getRelationshipName() + "> on node <" + getNodeName() + ">";
    }
}
