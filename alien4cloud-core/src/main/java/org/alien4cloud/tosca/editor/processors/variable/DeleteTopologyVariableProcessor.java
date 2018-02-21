package org.alien4cloud.tosca.editor.processors.variable;

import java.util.List;

import javax.inject.Inject;

import org.alien4cloud.tosca.editor.EditorService;
import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;
import org.alien4cloud.tosca.editor.operations.variable.DeleteTopologyVariableOperation;
import org.alien4cloud.tosca.editor.operations.variable.UpdateEnvironmentTypeVariableOperation;
import org.alien4cloud.tosca.editor.operations.variable.UpdateEnvironmentVariableOperation;
import org.alien4cloud.tosca.editor.processors.IEditorCommitableProcessor;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.variable.ScopeVariableExpressionDTO;
import org.alien4cloud.tosca.variable.service.VariableExpressionService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import alien4cloud.model.application.EnvironmentType;
import lombok.SneakyThrows;

/**
 * Process an {@link DeleteTopologyVariableOperation} to delete a variable form env and env_type scopes.
 */
@Component
public class DeleteTopologyVariableProcessor
        implements IEditorCommitableProcessor<DeleteTopologyVariableOperation>, IEditorOperationProcessor<DeleteTopologyVariableOperation> {

    @Inject
    private VariableExpressionService variableExpressionService;
    @Inject
    private EditorService editorService;

    @Override
    @SneakyThrows
    public void process(Csar csar, Topology topology, DeleteTopologyVariableOperation operation) {
        // Find environments where the var is defined and delete from there
        List<ScopeVariableExpressionDTO> envVars = variableExpressionService.getInEnvironmentScope(operation.getName(), topology.getArchiveName(),
                topology.getArchiveVersion(), null);
        envVars.forEach(scopeVariableExpressionDTO -> deleteVariableFromEnvironment(operation, scopeVariableExpressionDTO.getScopeId(), csar, topology));

        // Find environments types where the var is defined and delete from there
        List<ScopeVariableExpressionDTO> envTypeVars = variableExpressionService.getInEnvironmentTypeScope(operation.getName(), topology.getArchiveName(),
                topology.getArchiveVersion(), null);
        envTypeVars
                .forEach(scopeVariableExpressionDTO -> deleteVariableFromEnvironmentType(operation, scopeVariableExpressionDTO.getScopeId(), csar, topology));

    }

    private void deleteVariableFromEnvironmentType(DeleteTopologyVariableOperation operation, String environmentType, Csar csar, Topology topology) {
        UpdateEnvironmentTypeVariableOperation updateEnvironmentVariableOperation = new UpdateEnvironmentTypeVariableOperation();
        updateEnvironmentVariableOperation.setName(operation.getName());
        updateEnvironmentVariableOperation.setEnvironmentType(EnvironmentType.valueOf(StringUtils.upperCase(environmentType)));
        editorService.getProcessor(updateEnvironmentVariableOperation).process(csar, topology, updateEnvironmentVariableOperation);
        operation.getOperations().add(updateEnvironmentVariableOperation);
    }

    private void deleteVariableFromEnvironment(DeleteTopologyVariableOperation operation, String environmentId, Csar csar, Topology topology) {
        UpdateEnvironmentVariableOperation updateEnvironmentVariableOperation = new UpdateEnvironmentVariableOperation();
        updateEnvironmentVariableOperation.setName(operation.getName());
        updateEnvironmentVariableOperation.setEnvironmentId(environmentId);
        editorService.getProcessor(updateEnvironmentVariableOperation).process(csar, topology, updateEnvironmentVariableOperation);

        operation.getOperations().add(updateEnvironmentVariableOperation);
    }

    @Override
    public void beforeCommit(DeleteTopologyVariableOperation operation) {
        operation.getOperations().forEach(innerOperation -> {
            IEditorOperationProcessor<? extends AbstractEditorOperation> processor = editorService.getProcessor(innerOperation);
            if (processor instanceof IEditorCommitableProcessor) {
                ((IEditorCommitableProcessor) processor).beforeCommit(innerOperation);
            }
        });
    }
}