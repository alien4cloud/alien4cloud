package org.alien4cloud.tosca.editor.processors;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.alien4cloud.tosca.editor.operations.UpdateFileContentOperation;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.utils.PropertiesYamlParser;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

/**
 * Process an {@link org.alien4cloud.tosca.editor.operations.UpdateFileContentOperation} to update the content of a file.
 */
@Component
public class UpdateFileContentProcessor extends AbstractUpdateFileProcessor<UpdateFileContentOperation> {
    @Override
    public void process(Csar csar, Topology topology, UpdateFileContentOperation operation) {
        if (operation.getTempFileId() == null) {
            operation.setArtifactStream(new ByteArrayInputStream(operation.getContent().getBytes(StandardCharsets.UTF_8)));
        }

        if (operation.getPath().startsWith("inputs/var_env_")) {
           // check syntax
           ByteArrayResource vars = new ByteArrayResource(operation.getContent().getBytes());
           PropertiesYamlParser.ToProperties.from(vars);
        }

        super.process(csar, topology, operation);
        // content is store in a temp file on disk, no need to keep data in memory.
        operation.setContent(null);
    }
}