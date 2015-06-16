package alien4cloud.model.components;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Property filter value
 * 
 * @author cem
 *
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PropertyNodeFilterValue implements INodeFilterValue {

    private List<PropertyFilterDefinition> propertiesFilter;

    @Override
    public boolean isCapabilityFilter() {
        return false;
    }
}
