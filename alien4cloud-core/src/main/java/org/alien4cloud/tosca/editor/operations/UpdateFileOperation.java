package org.alien4cloud.tosca.editor.operations;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Upload/Update a file in an archive.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFileOperation extends AbstractEditorOperation {
    private String path;
    private String tempFileId;
}