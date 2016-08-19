package org.alien4cloud.tosca.editor.operations;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.InputStream;

/**
 * Upload/Update a file in an archive.
 *
 * Note that this operation is generated through a classical rest call as we need the input stream to manage file upload.
 */
@Getter
@Setter
@NoArgsConstructor
public class UpdateFileOperation extends AbstractUpdateFileOperation {
    /**
     * Create a new operation to upload a file.
     * 
     * @param path The path in which to save the file in the topology.
     * @param artifactStream The input stream of the file content.
     */
    public UpdateFileOperation(String path, InputStream artifactStream) {
        super(path, artifactStream);
    }
}