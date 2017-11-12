package org.alien4cloud.tosca.model.definitions;

import java.util.List;
import java.util.Map;

import alien4cloud.json.deserializer.PropertyConstraintListDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Maps;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FilterDefinition {
    /** Property constraint list by property */
    @JsonDeserialize(contentUsing = PropertyConstraintListDeserializer.class)
    private Map<String, List<PropertyConstraint>> properties = Maps.newHashMap();
}