package org.alien4cloud.tosca.editor.processors.inputs;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Arrays;
import java.util.Map;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.inputs.RenameInputOperation;
import org.alien4cloud.tosca.editor.processors.IEditorCommitableProcessor;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.deployment.DeploymentTopologyService;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.InvalidNameException;
import alien4cloud.exception.NotFoundException;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import alien4cloud.model.deployment.DeploymentTopology;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.normative.ToscaFunctionConstants;
import lombok.extern.slf4j.Slf4j;

/**
 * Removes an input from the topology and reset the properties values if the input was assigned.
 */
@Slf4j
@Component
public class RenameInputProcessor extends AbstractInputProcessor<RenameInputOperation> implements IEditorCommitableProcessor<RenameInputOperation> {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Inject
    private TopologyServiceCore topologyServiceCore;
    @Inject
    private DeploymentTopologyService deploymentTopologyService;

    @Override
    protected void processInputOperation(RenameInputOperation operation, Map<String, PropertyDefinition> inputs) {
        if (!inputs.containsKey(operation.getInputName())) {
            throw new NotFoundException("Input " + operation.getInputName() + " not found");
        }
        if (operation.getInputName().equals(operation.getNewInputName())) {
            return; // nothing has changed.
        }
        if (operation.getNewInputName() == null || operation.getNewInputName().isEmpty() || !operation.getNewInputName().matches("\\w+")) {
            throw new InvalidNameException("newInputName", operation.getNewInputName(), "\\w+");
        }
        if (inputs.containsKey(operation.getNewInputName())) {
            throw new AlreadyExistException("Input " + operation.getNewInputName() + " already existed");
        }

        PropertyDefinition propertyDefinition = inputs.remove(operation.getInputName());
        inputs.put(operation.getNewInputName(), propertyDefinition);

        Topology topology = EditionContextManager.getTopology();
        Map<String, NodeTemplate> nodeTemplates = topology.getNodeTemplates();
        for (NodeTemplate nodeTemp : safe(nodeTemplates).values()) {
            renameInputInProperties(nodeTemp.getProperties(), operation.getInputName(), operation.getNewInputName());
            if (nodeTemp.getRelationships() != null) {
                for (RelationshipTemplate relationshipTemplate : nodeTemp.getRelationships().values()) {
                    renameInputInProperties(relationshipTemplate.getProperties(), operation.getInputName(), operation.getNewInputName());
                }
            }
            if (nodeTemp.getCapabilities() != null) {
                for (Capability capability : nodeTemp.getCapabilities().values()) {
                    renameInputInProperties(capability.getProperties(), operation.getInputName(), operation.getNewInputName());
                }
            }
        }

        log.debug("Change the name of an input parameter <{}> to <{}> for the topology ", operation.getInputName(), operation.getNewInputName(),
                topology.getId());
    }

    /**
     * Update the input name for the {@link FunctionPropertyValue} in a map of properties.
     *
     * @param properties The map of properties in which to rename input
     * @param inputName The name of the input to rename.
     * @param newInputName The new name for the input.
     */
    private void renameInputInProperties(final Map<String, AbstractPropertyValue> properties, final String inputName, final String newInputName) {
        if (MapUtils.isNotEmpty(properties)) {
            for (AbstractPropertyValue propertyValue : properties.values()) {
                if (propertyValue instanceof FunctionPropertyValue) {
                    FunctionPropertyValue functionPropertyValue = (FunctionPropertyValue) propertyValue;
                    if (ToscaFunctionConstants.GET_INPUT.equals(functionPropertyValue.getFunction())
                            && functionPropertyValue.getParameters().get(0).equals(inputName)) {
                        functionPropertyValue.setParameters(Arrays.asList(newInputName));
                    }
                }
            }
        }
    }

    @Override
    public void beforeCommit(RenameInputOperation operation) {
        Topology topology = EditionContextManager.getTopology();
        DeploymentTopology[] deploymentTopologies = deploymentTopologyService.getByTopologyId(topology.getId());
        for (DeploymentTopology deploymentTopology : deploymentTopologies) {
            if (deploymentTopology.getInputProperties() != null && deploymentTopology.getInputProperties().containsKey(operation.getInputName())) {
                PropertyValue previousValue = deploymentTopology.getInputProperties().remove(operation.getInputName());
                deploymentTopology.getInputProperties().put(operation.getNewInputName(), previousValue);
                alienDAO.save(deploymentTopology);
            }
        }
    }

    @Override
    protected boolean create() {
        return false;
    }
}