package alien4cloud.model.components;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FilterDefinition {
    /** Property constraint list by property */
    private Map<String, List<PropertyConstraint>> properties;
}
