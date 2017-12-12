package org.alien4cloud.tosca.editor.processors.variable;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import javax.inject.Inject;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.EditorFileService;
import org.alien4cloud.tosca.editor.operations.variable.UpdateEnvironmentVariableOperation;
import org.alien4cloud.tosca.editor.processors.AbstractUpdateFileProcessor;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.variable.QuickFileStorageService;
import org.springframework.stereotype.Component;

import alien4cloud.component.repository.IFileRepository;
import alien4cloud.utils.YamlParserUtil;
import lombok.SneakyThrows;

/**
 * Process an {@link UpdateEnvironmentVariableOperation} to update the content of a file.
 */
@Component
public class UpdateEnvironmentVariableProcessor extends AbstractUpdateFileProcessor<UpdateEnvironmentVariableOperation> {
    @Inject
    private QuickFileStorageService quickFileStorageService;
    @Inject
    private IFileRepository artifactRepository;
    @Inject
    private EditorFileService editorFileService;

    @Override
    @SneakyThrows
    public void process(Csar csar, Topology topology, UpdateEnvironmentVariableOperation operation) {
        // load if exists the corresponding variables file
        operation.setPath(quickFileStorageService.getRelativeEnvironmentVariablesFilePath(operation.getEnvironmentId()));
        Properties variables = editorFileService.loadEnvironmentVariables(EditionContextManager.get().getCsar().getId(), operation.getEnvironmentId());

        // update the value of the variable
        // TODO what if the variable name is something like: toto.tata.titi ?
        variables.put(operation.getName(), YamlParserUtil.load(operation.getExpression()));
        variables.entrySet().removeIf(entry -> ((String) entry.getKey()).contains("."));
        if (operation.getTempFileId() == null) {
            operation.setArtifactStream(new ByteArrayInputStream(YamlParserUtil.dump(variables).getBytes(StandardCharsets.UTF_8)));
        }
        super.process(csar, topology, operation);
    }

}