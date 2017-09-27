package org.alien4cloud.tosca.model.templates;

import java.util.List;
import java.util.Map;

import org.alien4cloud.tosca.model.definitions.PolicyTrigger;

import lombok.Getter;
import lombok.Setter;

/**
 * Referred as policy definition in TOSCA.
 */
@Getter
@Setter
public class PolicyTemplate extends AbstractTemplate {
    private List<String> targets;
    private Map<String, PolicyTrigger> triggers;
}
