package org.alien4cloud.tosca.editor.processors.variable;

import java.util.Map;

import javax.inject.Inject;

import org.alien4cloud.tosca.editor.EditorFileService;
import org.alien4cloud.tosca.editor.operations.variable.UpdateEnvironmentTypeVariableOperation;
import org.alien4cloud.tosca.variable.service.QuickFileStorageService;
import org.springframework.stereotype.Component;

/**
 * Process an {@link UpdateEnvironmentTypeVariableOperation} to update the content of a file.
 */
@Component
public class UpdateEnvironmentTypeVariableProcessor extends AbstractUpdateTopologyVariableProcessor<UpdateEnvironmentTypeVariableOperation> {
    @Inject
    private QuickFileStorageService quickFileStorageService;
    @Inject
    private EditorFileService editorFileService;

    @Override
    protected String getRelativeVariablesFilePath(UpdateEnvironmentTypeVariableOperation operation) {
        return quickFileStorageService.getRelativeEnvironmentTypeVariablesFilePath(operation.getEnvironmentType().toString());
    }

    @Override
    protected Map<String, Object> loadVariables(String archiveId, UpdateEnvironmentTypeVariableOperation operation) {
        return editorFileService.loadEnvironmentTypeVariables(archiveId, operation.getEnvironmentType());
    }
}