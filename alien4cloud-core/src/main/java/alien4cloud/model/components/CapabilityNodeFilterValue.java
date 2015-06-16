package alien4cloud.model.components;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Simple capability filter value
 * 
 * @author cem
 *
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CapabilityNodeFilterValue implements INodeFilterValue {

    /** The string identifier could be a property name or a type */
    private Map<String, List<PropertyFilterDefinition>> capabilitiesFilter;

    @Override
    public boolean isCapabilityFilter() {
        return true;
    }
}
