package alien4cloud.model.orchestrators.locations;

import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.DeployerRole;

public class LocationMatchAuthorizationElector implements ILocationMatchElector {

    @Override
    public boolean isEligible(LocationMatch locationMatch) {
        return AuthorizationUtil.hasAuthorization(locationMatch.getLocation(), null, DeployerRole.values());
    }
}
