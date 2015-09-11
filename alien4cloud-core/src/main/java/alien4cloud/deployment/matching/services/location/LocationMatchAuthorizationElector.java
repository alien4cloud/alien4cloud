package alien4cloud.deployment.matching.services.location;

import alien4cloud.model.deployment.matching.LocationMatch;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.DeployerRole;

public class LocationMatchAuthorizationElector implements ILocationMatchElector {

    @Override
    public boolean isEligible(LocationMatch locationMatch) {
        return AuthorizationUtil.hasAuthorization(locationMatch.getLocation(), null, DeployerRole.values());
    }
}
