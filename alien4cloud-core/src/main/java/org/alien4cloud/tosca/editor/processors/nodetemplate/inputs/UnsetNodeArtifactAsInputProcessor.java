package org.alien4cloud.tosca.editor.processors.nodetemplate.inputs;

import static alien4cloud.utils.AlienUtils.safe;

import org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.UnsetNodeArtifactAsInputOperation;
import org.alien4cloud.tosca.editor.processors.nodetemplate.AbstractNodeProcessor;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Component;

import org.alien4cloud.tosca.model.templates.NodeTemplate;
import alien4cloud.utils.InputArtifactUtil;

/**
 * Remove association from an artifact to an input.
 */
@Component
public class UnsetNodeArtifactAsInputProcessor extends AbstractNodeProcessor<UnsetNodeArtifactAsInputOperation> {

    @Override
    protected void processNodeOperation(Csar csar, Topology topology, UnsetNodeArtifactAsInputOperation operation, NodeTemplate nodeTemplate) {
        if (safe(nodeTemplate.getArtifacts()).containsKey(operation.getArtifactName())) {
            InputArtifactUtil.unsetInputArtifact(nodeTemplate.getArtifacts().get(operation.getArtifactName()));
        }
    }
}
