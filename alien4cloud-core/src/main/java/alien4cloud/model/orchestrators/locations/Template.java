package alien4cloud.model.orchestrators.locations;

import alien4cloud.model.topology.NodeTemplate;

/**
 * A Location template is an element that is available on demand on a cloud and that a PaaS provider will be able to instanciate on demand.
 *
 * Note: template kind of resources may be pooled resources, if so a delimited configured list will have to be provided and won't be associated with more than one deployment.
 * Pooled resource can also be associated with specific security profiles in order to limit it's usage to defined team(s).
 */
public class Template {
    /** The node that represents the template resource. */
    private NodeTemplate templateNode;
}