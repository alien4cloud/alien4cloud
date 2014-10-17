package alien4cloud.rest.topology.task;

import java.util.List;

import alien4cloud.rest.topology.RequirementToSatify;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class RequirementsTask extends TopologyTask {
    // list of requirements for which to satisfy lowerbound
    private List<RequirementToSatify> requirementsToImplement;
}
