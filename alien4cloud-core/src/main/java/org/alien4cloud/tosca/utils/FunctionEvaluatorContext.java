package org.alien4cloud.tosca.utils;

import java.util.Map;

import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.templates.Topology;

import lombok.Getter;

/**
 * This context is used to evaluate a function on a topology.
 */
@Getter
public class FunctionEvaluatorContext {
    /** The topology on which to evaluate the function. */
    private Topology topology;
    /** The inputs as provided by the user. */
    private Map<String, PropertyValue> inputs;

    public FunctionEvaluatorContext(Topology topology, Map<String, PropertyValue> inputs) {
        this.topology = topology;
        this.inputs = inputs;
    }
}