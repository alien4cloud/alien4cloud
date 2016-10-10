package org.alien4cloud.tosca.editor.services;

import java.nio.file.Path;

import org.alien4cloud.tosca.catalog.AbstractArchivePostProcessor;
import org.alien4cloud.tosca.editor.processors.FileProcessorHelper;
import org.springframework.stereotype.Component;

import alien4cloud.exception.NotFoundException;

/**
 * Archive post processor that checks if local artifacts exists based on the edition context in memory tree node as some pending file operations may be in
 * undo/redo queue.
 */
@Component
public class EditorArchivePostProcessor extends AbstractArchivePostProcessor {

    @Override
    protected ArchivePathChecker createPathChecker(Path archive) {

        return new ArchivePathChecker() {
            @Override
            public boolean exists(String artifactReference) {
                try {
                    FileProcessorHelper.getFileTreeNode(artifactReference);
                } catch (NotFoundException e) {
                    return false;
                }
                return true;
            }

            @Override
            public void close() {
                // Do nothing.
            }
        };
    }
}
