package alien4cloud.topology.task;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
public class NodeFilterToSatisfy {
    private String relationshipName;
    private String targetName;
    private List<String> missingCapabilities;
    // list of violated constraints.
    private List<Violations> violations;

    @NoArgsConstructor
    public static class Violations {
        public String propertyName;
        public String relatedInput;
        public List<NodeFilterConstraintViolation> violatedConstraints;

        public Violations(String propertyName) {
            this.propertyName = propertyName;
        }

    }

}