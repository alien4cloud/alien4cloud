package org.alien4cloud.tosca.editor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.validation.Valid;

import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;
import org.alien4cloud.tosca.editor.operations.UpdateFileOperation;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import alien4cloud.component.repository.IFileRepository;
import alien4cloud.git.SimpleGitHistoryEntry;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.topology.TopologyDTO;
import io.swagger.annotations.ApiOperation;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Controller endpoint for topology edition.
 */
@RestController
@RequestMapping({ "/rest/v2/editor", "/rest/latest/editor" })
public class EditorController {
    @Inject
    private EditorService editorService;
    /** We use the artifact repository to store temporary files from the edition context. */
    @Resource
    private IFileRepository artifactRepository;

    /**
     * Execute an operation on a topology.
     *
     * @param topologyId The id of the topology/archive under edition.
     * @param operation The operation to execute
     */
    @ApiIgnore
    @RequestMapping(value = "/{topologyId}/execute", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<TopologyDTO> execute(@PathVariable String topologyId, @RequestBody @Valid AbstractEditorOperation operation) {
        TopologyDTO topologyDTO = editorService.execute(topologyId, operation);
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyDTO).build();
    }

    /**
     * Undo or redo operations.
     * 
     * @param topologyId The id of the topology under edition on which to undo operations.
     * @param at The index in the operations array to reach (0 means no operations, 1 means first operation etc.).
     * @param lastOperationId The id of the last operation from editor client point of view (for optimistic locking).
     * @return A topology DTO with the updated topology.
     */
    @ApiIgnore
    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/{topologyId}/undo", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TopologyDTO> undoRedo(@PathVariable String topologyId, @RequestParam("at") int at,
            @RequestParam("lastOperationId") String lastOperationId) {
        if (lastOperationId != null && "null".equals(lastOperationId)) {
            lastOperationId = null;
        }
        // Call the service that will save and commit
        TopologyDTO topologyDTO = editorService.undoRedo(topologyId, at, lastOperationId);
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyDTO).build();
    }

    /**
     * Method exposed to REST to upload a file in an archive under edition.
     * 
     * @param topologyId The id of the topology/archive under edition.
     * @param lastOperationId The id of the user last known operation (for optimistic locking edition).
     * @param path The path in which to save/override the file in the archive.
     * @param file The file to save in the archive.
     */
    @ApiIgnore
    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/{topologyId}/upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TopologyDTO> upload(@PathVariable String topologyId, @RequestParam("lastOperationId") String lastOperationId,
            @RequestParam("path") String path, @RequestParam(value = "file") MultipartFile file) throws IOException {
        if (lastOperationId != null && "null".equals(lastOperationId)) {
            lastOperationId = null;
        }

        try (InputStream artifactStream = file.getInputStream()) {
            UpdateFileOperation updateFileOperation = new UpdateFileOperation(path, artifactStream);
            updateFileOperation.setPreviousOperationId(lastOperationId);
            TopologyDTO topologyDTO = editorService.execute(topologyId, updateFileOperation);
            return RestResponseBuilder.<TopologyDTO> builder().data(topologyDTO).build();
        }
    }

    /**
     * Download a temporary file which is not yet commited (uploaded or modified through an operation).
     * 
     * @param topologyId The if of the topology.
     * @param artifactId The id of the temporary artifact.
     * @return The response entity with the input stream of the file.
     */
    @ApiIgnore
    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/{topologyId}/file/{artifactId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InputStreamResource> downloadTempFile(@PathVariable String topologyId, @PathVariable String artifactId) {
        editorService.checkAuthorization(topologyId);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");

        long length = artifactRepository.getFileLength(artifactId);

        return ResponseEntity.ok().headers(headers).contentLength(length).contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(new InputStreamResource(artifactRepository.getFile(artifactId)));
    }

    /**
     * Save the given topology and commit to the local git repository.
     *
     * @param topologyId The id of the topology/archive under edition to save.
     * @param lastOperationId The id of the last operation from editor client point of view (for optimistic locking).
     * @return A topology DTO with the updated topology.
     */
    @ApiIgnore
    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/{topologyId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TopologyDTO> save(@PathVariable String topologyId, @RequestParam("lastOperationId") String lastOperationId) {
        if (lastOperationId != null && "null".equals(lastOperationId)) {
            lastOperationId = null;
        }
        // Call the service that will save and commit
        TopologyDTO topologyDTO = editorService.save(topologyId, lastOperationId);
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyDTO).build();
    }

    @ApiIgnore
    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/{topologyId}/history", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<List<SimpleGitHistoryEntry>> history(@PathVariable String topologyId, @RequestParam("from") int from,
            @RequestParam("count") int count) {
        List<SimpleGitHistoryEntry> historyEntries = editorService.history(topologyId, from, count);
        return RestResponseBuilder.<List<SimpleGitHistoryEntry>> builder().data(historyEntries).build();
    }

    @ApiOperation(value = "Override the topology archive with the one provided as a parameter.", notes = "This operation will fail if the topology is under edition (meaning a context with some operations exists). The topology will be fully overriden with the new archive content (if valid) and a local commit will be dispatched.")
    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/{topologyId}/override", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> updateTopologyArchive(@PathVariable String topologyId, @RequestParam(value = "file") MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            editorService.override(topologyId, inputStream);
        }
        return RestResponseBuilder.<Void> builder().build();
    }
}