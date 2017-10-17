package org.alien4cloud.tosca.model.templates;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import org.alien4cloud.tosca.model.definitions.PolicyTrigger;
import org.elasticsearch.annotation.ObjectField;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.mapping.IndexType;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import lombok.Getter;
import lombok.Setter;

/**
 * Referred as policy definition in TOSCA.
 */
@Getter
@Setter
public class PolicyTemplate extends AbstractTemplate {
    @StringField(indexType = IndexType.no, includeInAll = false)
    private Set<String> targets = Sets.newLinkedHashSet();
    @ObjectField(enabled = false)
    private Map<String, PolicyTrigger> triggers = Maps.newLinkedHashMap();
}
