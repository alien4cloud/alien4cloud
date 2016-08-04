package org.alien4cloud.tosca.editor.operations;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Delete a file from the archive.
 */
@Getter
@Setter
@NoArgsConstructor
public class DeleteFileOperation extends AbstractEditorOperation {
    private String path;

    @Override
    public String commitMessage() {
        return "deleted file <" + path + ">";
    }
}