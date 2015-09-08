package alien4cloud.topology.matching.nodes.model;

import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.model.topology.NodeTemplate;

import java.util.List;

/**
 * Result of a node matching that defines multiple ordered proposal elements for a matching.
 */
public class NodeMatchingResult {
    /** The node template against which the node has been matched. */
    private List<LocationResourceTemplate> matchedTemplates;
}