package alien4cloud.plugin.mock;

import java.util.Iterator;
import java.util.List;

import lombok.AllArgsConstructor;
import alien4cloud.model.orchestrators.locations.ILocationMatchFilter;
import alien4cloud.model.orchestrators.locations.LocationMatch;
import alien4cloud.model.topology.Topology;
import alien4cloud.plugin.model.ManagedPlugin;

/**
 * Location match filter that will filter on pluginId.
 *
 */
@AllArgsConstructor
public class MockLocationMatchOrchestratorFilter implements ILocationMatchFilter {

    private ManagedPlugin selfContext;

    @Override
    public void filter(List<LocationMatch> toFilter, Topology topology) {
        for (Iterator<LocationMatch> it = toFilter.iterator(); it.hasNext();) {
            LocationMatch locationMatch = it.next();
            if (!locationMatch.getOrchestrator().getPluginId().equals(selfContext.getPlugin().getId())) {
                it.remove();
            }
        }
    }

}
