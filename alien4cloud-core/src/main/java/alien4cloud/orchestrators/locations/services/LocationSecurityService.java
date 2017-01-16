package alien4cloud.orchestrators.locations.services;

import org.springframework.stereotype.Service;

import alien4cloud.model.orchestrators.locations.Location;

@Service
public class LocationSecurityService {

    public boolean isAuthorised(Location location) {
        return false;
    }
}
