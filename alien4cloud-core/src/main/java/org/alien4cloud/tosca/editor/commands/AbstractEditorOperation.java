package org.alien4cloud.tosca.editor.commands;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import lombok.Getter;
import lombok.Setter;

/**
 * A model operation is the operation that indeed affect the user.
 */
@Getter
@Setter
@JsonTypeInfo(use = Id.CLASS, include = As.PROPERTY, property = "type")
public abstract class AbstractEditorOperation {
    /** The id of the operation (generated server side). */
    private String id;
    /** This is used for optimistic locking and validating that operations are done in the right order. */
    private String previousOperationId;
}