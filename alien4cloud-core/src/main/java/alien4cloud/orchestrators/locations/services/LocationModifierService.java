package alien4cloud.orchestrators.locations.services;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.LocationModifierReference;
import com.google.common.collect.Lists;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * Manages locations modifiers.
 */
@Service
public class LocationModifierService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    /**
     * Add a locationModifierReference to an location
     * 
     * @param location
     * @param locationModifierReference to add
     */
    public void add(Location location, LocationModifierReference locationModifierReference) {
        if (location.getModifiers() == null) {
            location.setModifiers(Lists.newArrayList());
        }
        location.getModifiers().add(locationModifierReference);
        alienDAO.save(location);
    }

    /**
     * Remove a locationModifierReference
     * 
     * @param location
     * @param index of the locationModifierReference to remove
     * @return
     */
    public void remove(Location location, int index) {
        if (index < 0 || location.getModifiers() == null || index > location.getModifiers().size()) {
            throw new NotFoundException("Cannot found location modifier at the index "+ index);
        }
        location.getModifiers().remove(index);
        alienDAO.save(location);
    }

    /**
     * Move a location modifier from a position to another
     * 
     * @param location
     * @param from
     * @param to
     */
    public void move(Location location, int from, int to) {
        if (from == to) {
            return;
        }
        if (from < 0 || to < 0 || location.getModifiers() == null || from > location.getModifiers().size() || to > location.getModifiers().size()) {
            throw new NotFoundException("Cannot move location modifier from " + from + "to index " + to);
        }
        LocationModifierReference modifier = location.getModifiers().remove(from);
        location.getModifiers().add(to, modifier);
        alienDAO.save(location);
    }
}
