package alien4cloud.model.components;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CapabilityNodeFilterValue implements INodeFilterValue {

    /** The identifier could be a property name or a type */
    private String identifier;
    private List<PropertyFilterDefinition> constraints;

    @Override
    public boolean isCapabilityFilter() {
        return true;
    }

}
