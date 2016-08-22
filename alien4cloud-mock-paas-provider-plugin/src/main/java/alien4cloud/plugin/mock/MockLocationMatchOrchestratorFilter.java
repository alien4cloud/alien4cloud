package alien4cloud.plugin.mock;

import java.util.Iterator;
import java.util.List;

import alien4cloud.deployment.matching.services.location.ILocationMatchFilter;
import alien4cloud.model.deployment.matching.ILocationMatch;
import alien4cloud.model.topology.Topology;
import alien4cloud.plugin.model.ManagedPlugin;
import lombok.AllArgsConstructor;

/**
 * Location match filter that will filter on pluginId.
 *
 */
@AllArgsConstructor(suppressConstructorProperties = true)
public class MockLocationMatchOrchestratorFilter implements ILocationMatchFilter {

    private ManagedPlugin selfContext;

    @Override
    public void filter(List<ILocationMatch> toFilter, Topology topology) {
        for (Iterator<ILocationMatch> it = toFilter.iterator(); it.hasNext();) {
            ILocationMatch locationMatch = it.next();
            if (!locationMatch.getOrchestrator().getPluginId().equals(selfContext.getPlugin().getId())) {
                it.remove();
            }
        }
    }

}
