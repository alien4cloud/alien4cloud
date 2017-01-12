package alien4cloud.deployment.matching.services.location;

import org.springframework.security.access.AccessDeniedException;

import alien4cloud.model.deployment.matching.ILocationMatch;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.User;

public class LocationMatchAuthorizationElector implements ILocationMatchElector {

    @Override
    public boolean isEligible(ILocationMatch locationMatch) {
        User user = AuthorizationUtil.getCurrentUser();
        if (user == null) {
            throw new AccessDeniedException("Session expired");
        }
        if (true)
            throw new AccessDeniedException("To be implemented");
        return false;
        // return AuthorizationUtil.hasAuthorization(user, locationMatch.getLocation(), null, DeployerRole.values());
    }
}
