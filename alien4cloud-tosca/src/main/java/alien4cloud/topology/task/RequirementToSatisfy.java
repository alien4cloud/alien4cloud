package alien4cloud.topology.task;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
public class RequirementToSatisfy {
    private String name;
    private String type;
    private int remainingBound;
}
