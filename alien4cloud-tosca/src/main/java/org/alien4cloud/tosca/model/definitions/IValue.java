package org.alien4cloud.tosca.model.definitions;

/**
 * An interface to be implemented by all tosca component value ( being a definition, a simple scalar or a function).
 *
 */
public interface IValue {
    /**
     * Allow to know if the value has a definition (property definition type ) or not (other types like scalar or functions).
     *
     * @return true if has a property definition and false if not.
     */
    boolean isDefinition();
}