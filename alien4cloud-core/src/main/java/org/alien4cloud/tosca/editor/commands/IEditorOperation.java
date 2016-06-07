package org.alien4cloud.tosca.editor.commands;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * A model operation is the operation that indeed affect the user.
 */
@JsonTypeInfo(use = Id.CLASS, include = As.PROPERTY, property = "type")
public interface IEditorOperation {
}
