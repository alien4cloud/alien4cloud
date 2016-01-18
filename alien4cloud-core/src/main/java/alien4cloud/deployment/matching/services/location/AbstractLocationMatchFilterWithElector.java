package alien4cloud.deployment.matching.services.location;

import java.util.Iterator;
import java.util.List;

import alien4cloud.model.deployment.matching.ILocationMatch;

public abstract class AbstractLocationMatchFilterWithElector implements ILocationMatchFilter {

    protected void filterWith(List<ILocationMatch> toFilter, ILocationMatchElector elector) {
        for (Iterator<ILocationMatch> it = toFilter.iterator(); it.hasNext();) {
            ILocationMatch locationMatch = it.next();
            if (!elector.isEligible(locationMatch)) {
                it.remove();
            }
        }
    }
}
