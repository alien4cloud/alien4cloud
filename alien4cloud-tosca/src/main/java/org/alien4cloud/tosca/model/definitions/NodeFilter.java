package org.alien4cloud.tosca.model.definitions;

import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.google.common.collect.Maps;

@Getter
@Setter
@NoArgsConstructor
public class NodeFilter extends FilterDefinition {
    /** properties field filters from FilterDefinition */
    /** capabilities field filters */
    private Map<String, FilterDefinition> capabilities = Maps.newHashMap();
}
