package org.alien4cloud.tosca.model.definitions;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.google.common.collect.Maps;

@Getter
@Setter
@NoArgsConstructor
public class FilterDefinition {
    /** Property constraint list by property */
    private Map<String, List<PropertyConstraint>> properties = Maps.newHashMap();
}