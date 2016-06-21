package org.alien4cloud.tosca.editor;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.tosca.editor.model.EditionConcurrencyException;
import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;
import org.alien4cloud.tosca.editor.operations.UpdateFileOperation;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.io.Closeables;

import alien4cloud.component.repository.IFileRepository;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.topology.TopologyDTO;
import io.swagger.annotations.ApiOperation;
import lombok.SneakyThrows;

/**
 * Controller endpoint for topology edition.
 */
@RestController
@RequestMapping({ "/rest/v2/editor", "/rest/latest/editor" })
public class EditorController {
    @Inject
    private TopologyDTOBuilder dtoBuilder;
    @Inject
    private TopologyEditorService editorService;
    /** We use the artifact repository to store temporary files from the edition context. */
    @Resource
    private IFileRepository artifactRepository;

    /**
     * Execute an operation on a topology.
     *
     * @param topologyId The id of the topology/archive under edition.
     * @param operation The operation to execute
     */
    @SneakyThrows
    @ApiOperation(value = "Updates the deployment artifact of the node template.", notes = "The logged-in user must have the application manager role for this application. Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public <T extends AbstractEditorOperation> RestResponse<TopologyDTO> execute(@PathVariable String topologyId, T operation) {
        TopologyEditionContext context = editorService.execute(topologyId, operation);
        return RestResponseBuilder.<TopologyDTO> builder().data(dtoBuilder.buildTopologyDTO(context)).build();
    }

    /**
     * Method exposed to REST to upload a file in an archive under edition.
     * 
     * @param topologyId The id of the topology/archive under edition.
     * @param lastOperationId The id of the user last known operation (for optimistic locking edition).
     * @param path The path in which to save/override the file in the archive.
     * @param file The file to save in the archive.
     */
    @SneakyThrows
    public void upload(@PathVariable String topologyId, String lastOperationId, String path, @RequestParam("file") MultipartFile file) throws IOException {
        // The controller saves the file in a temporary location and create a UpdateFileOperation to be sent in the edition context.
        InputStream artifactStream = file.getInputStream();
        String artifactFileId = null;
        try {
            artifactFileId = artifactRepository.storeFile(artifactStream);
            // FIXME return the topology context dto from the command execution ?
        } finally {
            Closeables.close(artifactStream, true);
        }
        try {
            editorService.execute(topologyId, new UpdateFileOperation(path, artifactFileId));
        } catch (EditionConcurrencyException e) {
            // Failed to perform the operation for concurrency issue, delete the temporary file.
            artifactRepository.deleteFile(artifactFileId);
            throw e;
        }
    }

    /**
     * Save the given topology and commit to the local git repository.
     *
     * @param topologyId The id of the topology/archive under edition to save.
     */
    public void save(@PathVariable String topologyId) {
        // Call the service that will save and commit

    }

    /**
     * Undo or redo operations so that we reach the given index in the operation stack.
     *
     * @param topologyId The id of the topology/archive under edition to save.
     * @param at the index of the operation to go to.
     */
    public void undoRedo(@PathVariable String topologyId, int at, String lastOperationId) {
        // Call the service that will save and commit

    }

}