package alien4cloud.model.components;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.google.common.collect.Maps;

/**
 * Property constraints list by property
 * 
 * @author mourouvi
 *
 */
@Getter
@Setter
@NoArgsConstructor
public class FilterDefinition {
    private Map<String, List<PropertyConstraint>> properties = Maps.newHashMap();
}
