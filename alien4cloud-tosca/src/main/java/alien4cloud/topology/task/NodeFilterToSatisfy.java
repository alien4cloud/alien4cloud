package alien4cloud.topology.task;

import java.util.List;
import java.util.Map;

import alien4cloud.tosca.properties.constraints.ConstraintUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NodeFilterToSatisfy {
    private String relationshipName;
    private String targetName;
    // propertyName, list of violated constraints.
    private List<String> missingCapabilities;
    private Map<String, List<NodeFilterConstraintViolation>> violatedConstraints;
}