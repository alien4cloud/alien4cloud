package org.alien4cloud.tosca.editor.processors;

import org.alien4cloud.tosca.editor.operations.UpdateFileOperation;
import org.springframework.stereotype.Component;

/**
 * Update file processor is just a simple processor implementation of the AbstractUpdateFileProcessor.
 * It is just used to specify the exact type of the operation supported.
 */
@Component
public class UpdateFileProcessor extends AbstractUpdateFileProcessor<UpdateFileOperation> {
}