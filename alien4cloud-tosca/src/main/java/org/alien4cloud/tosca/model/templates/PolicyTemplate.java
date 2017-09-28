package org.alien4cloud.tosca.model.templates;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.alien4cloud.tosca.model.definitions.PolicyTrigger;

import lombok.Getter;
import lombok.Setter;

/**
 * Referred as policy definition in TOSCA.
 */
@Getter
@Setter
public class PolicyTemplate extends AbstractTemplate {
    private List<String> targets = Lists.newArrayList();
    private Map<String, PolicyTrigger> triggers = Maps.newLinkedHashMap();
}
