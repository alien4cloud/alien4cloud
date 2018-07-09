package alien4cloud.rest.application.model;

import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.IDeploymentSource;
import alien4cloud.model.orchestrators.locations.Location;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * A wrapper for deployment that also bring data about workflow for monitoring concerns.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MonitoredDeploymentDTO {

    /**
     * The wrapped deployment.
     */
    private Deployment deployment;

    /**
     * Per workflow step name, the expected operation count (actually used for workflow progress bar).
     */
    private Map<String, Integer> workflowExpectedStepInstanceCount;
}
