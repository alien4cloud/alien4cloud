package org.alien4cloud.tosca.model.types;

import java.util.List;
import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.tosca.model.definitions.PolicyTrigger;
import org.elasticsearch.annotation.ESObject;

/**
 * Represents a TOSCA policy type.
 */
@Getter
@Setter
@EqualsAndHashCode(of = {}, callSuper = true)
@ESObject
public class PolicyType extends AbstractInheritableToscaType {
    private List<String> targets;
    private Map<String, PolicyTrigger> triggers;
}