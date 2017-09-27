package org.alien4cloud.tosca.model.definitions;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a policy event filter.
 */
@Getter
@Setter
public class PolicyEventFilter {
    private String node;
    private String requirement;
    private String capability;
}
