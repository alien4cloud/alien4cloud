package org.alien4cloud.tosca.editor.operations;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Update the content of a file in the archive.
 */
@Getter
@Setter
public class UpdateFileContentOperation extends AbstractUpdateFileOperation {
    private String content;
}
