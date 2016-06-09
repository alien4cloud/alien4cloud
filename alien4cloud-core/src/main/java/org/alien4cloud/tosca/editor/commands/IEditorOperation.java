package org.alien4cloud.tosca.editor.commands;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * A model operation is the operation that indeed affect the user.
 */
@JsonTypeInfo(use = Id.CLASS, include = As.PROPERTY, property = "type")
public interface IEditorOperation {
    /**
     * To avoid concurrency issues we manage an Optimistic locking kind of mechanism and process operation only if the index of the operation matches the size
     * of the operations list, meaning that the client received and applied all the issued operations before actually sending the new one.
     * 
     * @return the index of the operation for optimistic locking.
     */
    int getIndex();
}
