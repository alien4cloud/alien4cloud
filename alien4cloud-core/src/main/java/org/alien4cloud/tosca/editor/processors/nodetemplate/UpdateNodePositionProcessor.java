package org.alien4cloud.tosca.editor.processors.nodetemplate;

import org.alien4cloud.tosca.editor.Constants;
import org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodePositionOperation;
import org.alien4cloud.tosca.model.templates.NodeTemplate;

import com.google.common.collect.Lists;

import alien4cloud.model.common.Tag;
import org.springframework.stereotype.Component;

/**
 * Process an update node position operation and set position metadata to the node.
 */
@Component
public class UpdateNodePositionProcessor extends AbstractNodeProcessor<UpdateNodePositionOperation> {
    @Override
    protected void processNodeOperation(UpdateNodePositionOperation operation, NodeTemplate nodeTemplate) {
        // Set the position information of the node as meta-data.
        if (nodeTemplate.getTags() == null) {
            nodeTemplate.setTags(Lists.newArrayList(new Tag(Constants.X_META, String.valueOf(operation.getCoords().getX())),
                    new Tag(Constants.Y_META, String.valueOf(operation.getCoords().getY()))));
            return;
        }

        boolean xSet = false;
        boolean ySet = false;
        for (Tag tag : nodeTemplate.getTags()) {
            if (Constants.X_META.equals(tag.getName())) {
                tag.setValue(String.valueOf(operation.getCoords().getX()));
                xSet = true;
            }
            if (Constants.Y_META.equals(tag.getName())) {
                tag.setValue(String.valueOf(operation.getCoords().getY()));
                ySet = true;
            }
        }
        if (!xSet) {
            nodeTemplate.getTags().add(new Tag(Constants.X_META, String.valueOf(operation.getCoords().getX())));
        }
        if (!ySet) {
            nodeTemplate.getTags().add(new Tag(Constants.Y_META, String.valueOf(operation.getCoords().getY())));
        }
    }
}