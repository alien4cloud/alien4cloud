package alien4cloud.model.components;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NodeFilter extends FilterDefinition {
    /** properties field filters from FilterDefinition */
    /** capabilities field filters */
    private Map<String, List<FilterDefinition>> capabilities;
}
