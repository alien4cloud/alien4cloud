package org.alien4cloud.tosca.editor.processors.nodetemplate;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Map;

import org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateDockerImageOperation;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.ImplementationArtifact;
import org.alien4cloud.tosca.model.definitions.Interface;
import org.alien4cloud.tosca.model.definitions.Operation;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.normative.constants.NormativeArtifactTypes;
import org.alien4cloud.tosca.normative.constants.NormativeNodeTypesConstants;
import org.alien4cloud.tosca.utils.ToscaTypeUtils;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.utils.CloneUtil;

@Component
public class UpdateDockerImageProcessor extends AbstractNodeProcessor<UpdateDockerImageOperation> {

    @Override
    protected void processNodeOperation(Csar csar, Topology topology, UpdateDockerImageOperation operation, NodeTemplate nodeTemplate) {
        NodeType nodeType = ToscaContext.get(NodeType.class, nodeTemplate.getType());
        if (!ToscaTypeUtils.isOfType(nodeType, NormativeNodeTypesConstants.DOCKER_TYPE)) {
            throw new IllegalArgumentException("Updating docker image can only be done on docker nodes. [" + nodeTemplate.getName()
                    + "] does not inherit from [" + NormativeNodeTypesConstants.DOCKER_TYPE + "].");
        }
        Interface standard = safe(nodeTemplate.getInterfaces()).get(ToscaNodeLifecycleConstants.STANDARD);
        if (standard == null) {
            standard = new Interface(ToscaNodeLifecycleConstants.STANDARD);
            if (nodeTemplate.getInterfaces() == null) {
                nodeTemplate.setInterfaces(Maps.newHashMap());
            }
            nodeTemplate.getInterfaces().put(ToscaNodeLifecycleConstants.STANDARD, standard);
        }
        Operation create = safe(standard.getOperations()).get(ToscaNodeLifecycleConstants.CREATE);
        if (create == null) {
            create = getCreateOperation(nodeType.getInterfaces());
            if (create == null) {
                create = new Operation();
            } else {
                create = CloneUtil.clone(create);
            }
            if (standard.getOperations() == null) {
                standard.setOperations(Maps.newHashMap());
            }
            standard.getOperations().put(ToscaNodeLifecycleConstants.CREATE, create);
            if (create.getImplementationArtifact() == null) {
                ImplementationArtifact createIA = new ImplementationArtifact();
                createIA.setArtifactType(NormativeArtifactTypes.DOCKER);
                createIA.setRepositoryName("docker");
                create.setImplementationArtifact(createIA);
            }
        }
        create.getImplementationArtifact().setArchiveName(csar.getName());
        create.getImplementationArtifact().setArchiveVersion(csar.getVersion());
        create.getImplementationArtifact().setArtifactRef(operation.getDockerImage());
        create.getImplementationArtifact().setArtifactRepository("a4c_ignore");
    }

    private Operation getCreateOperation(Map<String, Interface> interfaces) {
        Interface standard = safe(interfaces).get(ToscaNodeLifecycleConstants.STANDARD);
        if (standard == null) {
            return null;
        }
        return safe(standard.getOperations()).get(ToscaNodeLifecycleConstants.CREATE);
    }
}
