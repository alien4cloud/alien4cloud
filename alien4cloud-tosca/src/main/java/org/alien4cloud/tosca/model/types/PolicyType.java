package org.alien4cloud.tosca.model.types;

import java.util.Map;
import java.util.Set;

import org.alien4cloud.tosca.model.definitions.PolicyTrigger;
import org.elasticsearch.annotation.ESObject;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a TOSCA policy type.
 */
@Getter
@Setter
@EqualsAndHashCode(of = {}, callSuper = true)
@ESObject
public class PolicyType extends AbstractInheritableToscaType {
    private Set<String> targets;
    private Map<String, PolicyTrigger> triggers;
}