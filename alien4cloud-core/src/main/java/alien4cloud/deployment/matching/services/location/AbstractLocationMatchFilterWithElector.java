package alien4cloud.deployment.matching.services.location;

import alien4cloud.model.deployment.matching.LocationMatch;

import java.util.Iterator;
import java.util.List;

public abstract class AbstractLocationMatchFilterWithElector implements ILocationMatchFilter {

    protected void filterWith(List<LocationMatch> toFilter, ILocationMatchElector elector) {
        for (Iterator<LocationMatch> it = toFilter.iterator(); it.hasNext();) {
            LocationMatch locationMatch = it.next();
            if (!elector.isEligible(locationMatch)) {
                it.remove();
            }
        }
    }
}
