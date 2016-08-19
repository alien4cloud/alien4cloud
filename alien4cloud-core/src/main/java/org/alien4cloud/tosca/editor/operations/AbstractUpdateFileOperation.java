package org.alien4cloud.tosca.editor.operations;

import java.io.InputStream;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Abstract operation to update a file content.
 */
@Getter
@Setter
@NoArgsConstructor
public abstract class AbstractUpdateFileOperation extends AbstractEditorOperation {
    private String path;
    private String tempFileId;
    @JsonIgnore
    private InputStream artifactStream;

    /**
     * Create a new operation to upload a file.
     *
     * @param path The path in which to save the file in the topology.
     * @param artifactStream The input stream of the file content.
     */
    public AbstractUpdateFileOperation(String path, InputStream artifactStream) {
        this.path = path;
        this.artifactStream = artifactStream;
    }

    @Override
    public String commitMessage() {
        return "updated content of file <" + path + ">";
    }
}