package org.alien4cloud.tosca.model.definitions;

import java.util.Map;

import org.elasticsearch.annotation.ObjectField;

import com.google.common.collect.Maps;

import alien4cloud.ui.form.annotation.FormProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Definition of the operations that can be performed on (instances of) a Node Type.
 */
@Getter
@Setter
@NoArgsConstructor
@FormProperties({ "operations" })
public class Interface {
    /** The type of the interface. */
    private String type;
    /** Description of the interface. */
    private String description;
    /** Defines an operation available to manage particular aspects of the Node Type. */
    @ObjectField(enabled = false)
    private Map<String, Operation> operations = Maps.newHashMap();

    /**
     * Create a new interface from it's type.
     * 
     * @param type The interface type.
     */
    public Interface(String type) {
        this.type = type;
    }
}