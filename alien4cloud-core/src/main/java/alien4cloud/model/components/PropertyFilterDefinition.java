package alien4cloud.model.components;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PropertyFilterDefinition {
    /** Property name */
    private String name;
    /** Property constraint list for this property */
    private List<PropertyConstraint> constraints;
}
