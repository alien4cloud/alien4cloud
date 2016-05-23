package alien4cloud.model.topology;

import java.awt.*;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * Model the layout for a given topology.
 *
 * Note that we allow manual placing only for root nodes, all nodes hosted on or attached to other nodes are automatically stacked on the hosting nodes.
 * We also don't allow manual locations for network nodes.
 */
@Getter
@Setter
public class TopologyLayout {
    // locations of the root node templates in the layout.
    Map<String, Point> nodeLocations;
}
