package org.alien4cloud.tosca.model.definitions;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.ui.form.annotation.FormProperties;

import com.google.common.collect.Maps;

/**
 * Definition of the operations that can be performed on (instances of) a Node Type.
 */
@Getter
@Setter
@FormProperties({ "operations" })
public class Interface {
    /** The type of the interface. */
    private String type;
    /** Description of the interface. */
    private String description;
    /** Defines an operation available to manage particular aspects of the Node Type. */
    private Map<String, Operation> operations = Maps.newHashMap();
}