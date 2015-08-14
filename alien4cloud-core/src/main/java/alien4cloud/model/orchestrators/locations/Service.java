package alien4cloud.model.orchestrators.locations;

/**
 * A cloud service is an element running on a cloud and that other deployments can use.
 *
 * They are modeled as TOSCA nodes and can match nodes from a topology.
 */
public class Service {
    /** unique id of the service. */
    private String id;
    /** Id of the tosca node type that model the service. */
    private String nodeTypeId;

    // A service may be configured or may be directly backed by an application topology.

}
