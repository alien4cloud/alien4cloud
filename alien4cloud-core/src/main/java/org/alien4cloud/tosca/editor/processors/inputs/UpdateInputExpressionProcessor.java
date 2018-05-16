package org.alien4cloud.tosca.editor.processors.inputs;

import java.util.Map;

import javax.inject.Inject;

import org.alien4cloud.tosca.editor.EditorFileService;
import org.alien4cloud.tosca.editor.operations.inputs.UpdateInputExpressionOperation;
import org.alien4cloud.tosca.editor.processors.variable.AbstractUpdateTopologyVariableProcessor;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.variable.service.QuickFileStorageService;
import org.springframework.stereotype.Component;

import alien4cloud.exception.NotFoundException;

@Component
public class UpdateInputExpressionProcessor extends AbstractUpdateTopologyVariableProcessor<UpdateInputExpressionOperation> {
    @Inject
    private QuickFileStorageService quickFileStorageService;
    @Inject
    private EditorFileService editorFileService;

    @Override
    public void process(Csar csar, Topology topology, UpdateInputExpressionOperation operation) {
        if (topology.getInputs() == null || !topology.getInputs().containsKey(operation.getName())) {
            throw new NotFoundException("Input <" + operation.getName() + "> is not defined in topology <" + topology.getId() + ">");
        }
        super.process(csar, topology, operation);
    }

    @Override
    protected String getRelativeVariablesFilePath(UpdateInputExpressionOperation operation) {
        return quickFileStorageService.getRelativeInputsFilePath();
    }

    @Override
    protected Map<String, Object> loadVariables(String archiveId, UpdateInputExpressionOperation operation) {
        return editorFileService.loadInputsVariables(archiveId);
    }
}
