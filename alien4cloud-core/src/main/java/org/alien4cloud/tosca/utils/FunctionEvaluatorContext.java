package org.alien4cloud.tosca.utils;

import java.util.Map;
import java.util.function.Function;

import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
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
    private Map<String, AbstractPropertyValue> inputs;

    /**
     * Function evaluation context for pre-deployment get_inputs and get_property resolving.
     *
     * @param topology The topology on which to evaluate the function.
     * @param inputs The inputs as provided by the user.
     */
    public FunctionEvaluatorContext(Topology topology, Map<String, AbstractPropertyValue> inputs) {
        this.topology = topology;
        this.inputs = inputs;
    }
}