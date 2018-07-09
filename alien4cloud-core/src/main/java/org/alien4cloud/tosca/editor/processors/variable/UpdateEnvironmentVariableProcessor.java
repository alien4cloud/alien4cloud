package org.alien4cloud.tosca.editor.processors.variable;

import java.util.Map;

import javax.inject.Inject;

import org.alien4cloud.tosca.editor.EditorFileService;
import org.alien4cloud.tosca.editor.operations.variable.UpdateEnvironmentVariableOperation;
import org.alien4cloud.tosca.variable.service.QuickFileStorageService;
import org.springframework.stereotype.Component;

/**
 * Process an {@link UpdateEnvironmentVariableOperation} to update the content of a file.
 */
@Component
public class UpdateEnvironmentVariableProcessor extends AbstractUpdateTopologyVariableProcessor<UpdateEnvironmentVariableOperation> {
    @Inject
    private QuickFileStorageService quickFileStorageService;
    @Inject
    private EditorFileService editorFileService;

    @Override
    protected String getRelativeVariablesFilePath(UpdateEnvironmentVariableOperation operation) {
        return quickFileStorageService.getRelativeEnvironmentVariablesFilePath(operation.getEnvironmentId());
    }

    @Override
    protected Map<String, Object> loadVariables(String archiveId, UpdateEnvironmentVariableOperation operation) {
        return editorFileService.loadEnvironmentVariables(archiveId, operation.getEnvironmentId());
    }
}