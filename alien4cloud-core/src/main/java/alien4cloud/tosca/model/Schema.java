package alien4cloud.tosca.model;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * Schema allows to create new types that can be used along TOSCA definitions.
 */
@Getter
@Setter
public class Schema {
    private String derivedFrom;
    private List<PropertyConstraint> constraints;
    private Map<String, PropertyDefinition> properties;
}