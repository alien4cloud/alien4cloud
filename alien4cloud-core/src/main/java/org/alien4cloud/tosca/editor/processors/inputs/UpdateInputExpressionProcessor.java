package org.alien4cloud.tosca.editor.processors.inputs;

import java.util.Map;

import javax.inject.Inject;

import org.alien4cloud.tosca.editor.EditorFileService;
import org.alien4cloud.tosca.editor.operations.inputs.UpdateInputExpressionOperation;
import org.alien4cloud.tosca.editor.processors.variable.AbstractUpdateTopologyVariableProcessor;
import org.alien4cloud.tosca.variable.QuickFileStorageService;
import org.springframework.stereotype.Component;

@Component
public class UpdateInputExpressionProcessor extends AbstractUpdateTopologyVariableProcessor<UpdateInputExpressionOperation> {
    @Inject
    private QuickFileStorageService quickFileStorageService;
    @Inject
    private EditorFileService editorFileService;

    @Override
    protected String getRelativeVariablesFilePath(UpdateInputExpressionOperation operation) {
        return quickFileStorageService.getRelativeInputsFilePath();
    }

    @Override
    protected Map<String, Object> loadVariables(String archiveId, UpdateInputExpressionOperation operation) {
        return editorFileService.loadInputsVariables(archiveId);
    }
}
