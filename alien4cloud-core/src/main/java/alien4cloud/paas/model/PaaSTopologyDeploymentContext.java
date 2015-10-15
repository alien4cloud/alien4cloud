package alien4cloud.paas.model;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.elasticsearch.annotation.ESObject;

import alien4cloud.model.orchestrators.locations.Location;

/**
 * The context of the deployment
 */
@Getter
@Setter
@ESObject
@ToString(callSuper = true)
@SuppressWarnings("PMD.UnusedPrivateField")
public class PaaSTopologyDeploymentContext extends PaaSDeploymentContext {

    /**
     * The parsed PaaS topology
     */
    private PaaSTopology paaSTopology;

    /**
     * Locations map id of the location --> the location it-self
     */
    private Map<String, Location> locations;
}