package alien4cloud.deployment.matching.services.location;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.deployment.matching.ILocationMatch;
import alien4cloud.orchestrators.locations.services.LocationSecurityService;
import alien4cloud.utils.AlienUtils;

@Component
public class LocationMatchAuthorizationFilter {

    @Resource
    private LocationSecurityService locationSecurityService;

    public void filter(List<ILocationMatch> toFilter, ApplicationEnvironment applicationEnvironment) {
        AlienUtils.safe(toFilter).removeIf(locationMatch -> !locationSecurityService.isAuthorised(locationMatch.getLocation(), applicationEnvironment));
    }

}
