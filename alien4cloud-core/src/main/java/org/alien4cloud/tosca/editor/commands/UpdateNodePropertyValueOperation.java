package org.alien4cloud.tosca.editor.commands;

import lombok.Getter;
import lombok.Setter;

/**
 * Update the value of a property of a node template.
 */
@Getter
@Setter
public class UpdateNodePropertyValueOperation extends AbstractNodeOperation {
    private String propertyName;
    private Object propertyValue;
}
