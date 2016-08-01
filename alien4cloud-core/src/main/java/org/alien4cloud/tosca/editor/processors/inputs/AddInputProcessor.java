package org.alien4cloud.tosca.editor.processors.inputs;

import java.util.Map;

import javax.annotation.Resource;
import javax.inject.Inject;

import alien4cloud.exception.InvalidNameException;
import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.inputs.AddInputOperation;
import org.alien4cloud.tosca.editor.processors.IEditorCommitableProcessor;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.deployment.DeploymentTopologyService;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.topology.Topology;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.normative.ToscaType;
import lombok.extern.slf4j.Slf4j;

/**
 * Process an add input operation.
 */
@Slf4j
@Component
public class AddInputProcessor extends AbstractInputProcessor<AddInputOperation> implements IEditorCommitableProcessor<AddInputOperation> {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Inject
    private TopologyServiceCore topologyServiceCore;
    @Inject
    private DeploymentTopologyService deploymentTopologyService;

    @Override
    protected void processInputOperation(AddInputOperation operation, Map<String, PropertyDefinition> inputs) {
        if (!operation.getInputName().matches("\\w+")) {
            throw new InvalidNameException("inputName", operation.getInputName(), "\\w+");
        }

        Topology topology = EditionContextManager.getTopology();

        if (inputs.containsKey(operation.getInputName())) {
            throw new AlreadyExistException("An input with the id " + operation.getInputName() + "already exist in the topology " + topology.getId());
        }

        // FIXME fix this for alien 1.3.0
        if (!ToscaType.isSimple(operation.getPropertyDefinition().getType())) {
            throw new InvalidArgumentException("An input with a non simple propery is not supported right now.");
        }

        inputs.put(operation.getInputName(), operation.getPropertyDefinition());
        topology.setInputs(inputs);

        log.debug("Add a new input <{}> for the topology <{}>.", operation.getInputName(), topology.getId());
    }

    @Override
    public void beforeCommit(AddInputOperation operation) {
        Topology topology = EditionContextManager.getTopology();
        // Update default values for each deployment topology
        AbstractPropertyValue defaultValue = operation.getPropertyDefinition().getDefault();
        if (defaultValue != null && defaultValue instanceof ScalarPropertyValue) {
            String defaultScalarValue = ((ScalarPropertyValue) defaultValue).getValue();
            DeploymentTopology[] deploymentTopologies = deploymentTopologyService.getByTopologyId(topology.getId());
            for (DeploymentTopology deploymentTopology : deploymentTopologies) {
                if (deploymentTopology.getInputProperties() == null) {
                    deploymentTopology.setInputProperties(Maps.<String, String> newHashMap());
                }
                deploymentTopology.getInputProperties().put(operation.getInputName(), defaultScalarValue);
                alienDAO.save(deploymentTopology);
            }
        }
    }

    @Override
    protected boolean create() {
        return true; // create the inputs map if null in the topology.
    }
}