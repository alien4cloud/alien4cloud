package alien4cloud.paas.model;

import alien4cloud.model.orchestrators.locations.Location;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

/**
 * The context of the deployment
 */
@Getter
@Setter
@ToString(callSuper = true)
public class PaaSTopologyDeploymentContext extends PaaSDeploymentContext {
    /** The parsed PaaS topology */
    private PaaSTopology paaSTopology;

    /** Locations map id of the location --> the location it-self */
    private Map<String, Location> locations;
}