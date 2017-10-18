package org.alien4cloud.tosca.editor.processors.nodetemplate.inputs;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Map;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.nodetemplate.inputs.SetNodeArtifactAsInputOperation;
import org.alien4cloud.tosca.editor.processors.nodetemplate.AbstractNodeProcessor;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import alien4cloud.exception.NotFoundException;
import alien4cloud.utils.InputArtifactUtil;

/**
 * Create or set an input artifact to the given node template artifact
 */
@Component
public class SetNodeArtifactAsInputProcessor extends AbstractNodeProcessor<SetNodeArtifactAsInputOperation> {
    @Override
    protected void processNodeOperation(Csar csar, Topology topology, SetNodeArtifactAsInputOperation operation, NodeTemplate nodeTemplate) {
        if (safe(nodeTemplate.getArtifacts()).get(operation.getArtifactName()) == null) {
            throw new NotFoundException("The artifact <" + operation.getArtifactName() + "> cannot be found on node <" + operation.getNodeName() + ">");
        }
        DeploymentArtifact artifact = nodeTemplate.getArtifacts().get(operation.getArtifactName());

        if (!safe(topology.getInputArtifacts()).containsKey(operation.getInputName())) {
            // we have to create the artifact
            operation.setNewArtifact(true);
            DeploymentArtifact inputArtifact = new DeploymentArtifact();
            inputArtifact.setArchiveName(artifact.getArchiveName());
            inputArtifact.setArchiveVersion(artifact.getArchiveVersion());
            inputArtifact.setArtifactType(artifact.getArtifactType());
            Map<String, DeploymentArtifact> inputArtifacts = topology.getInputArtifacts();
            if (inputArtifacts == null) {
                inputArtifacts = Maps.newHashMap();
                topology.setInputArtifacts(inputArtifacts);
            }
            inputArtifacts.put(operation.getInputName(), inputArtifact);
        }

        InputArtifactUtil.setInputArtifact(artifact, operation.getInputName());
    }
}
