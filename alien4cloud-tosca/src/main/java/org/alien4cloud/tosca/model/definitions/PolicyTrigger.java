package org.alien4cloud.tosca.model.definitions;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.elasticsearch.annotation.ESObject;

/**
 * Defines a trigger for a policy.
 */
@Getter
@Setter
public class PolicyTrigger {
    /** Description of the trigger. */
    private String description;
    /** Name of an attribute to be monitored. */
    private String eventType;
    /** This helps to locate the attribute to monitor. */
    private PolicyEventFilter eventFilter;
    /** Time interval during which the policy is active. */
    private TimeInterval timeInterval;
    private PolicyCondition condition;
    /** The action workflow to trigger when the policy condition is matched. */
    private String actionWorkflow;
    /** The operation to trigger when the condition is matched. Mutually exclusive with action workflow which should be prefered. */
    private Operation actionOperation;
}
