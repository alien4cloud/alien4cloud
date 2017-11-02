package alien4cloud.orchestrators.locations.services;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.LocationModifierReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Set;

import static alien4cloud.utils.AlienUtils.safe;

/**
 * Manages locations modifiers.
 */
@Slf4j
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
     * Add a locationModifierReference to an location at the specific index
     * 
     * @param location
     * @param locationModifierReference to add
     * @param index
     */
    public void add(Location location, LocationModifierReference locationModifierReference, int index) {
        if (location.getModifiers() == null) {
            location.setModifiers(Lists.newArrayList());
        }
        // TODO check index => Not found if index incorrect.
        location.getModifiers().add(index, locationModifierReference);
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
        if (location.getModifiers() == null) { // TODO && check index => Not found if index incorrect.
            // Throw not found
        }
        location.getModifiers().remove(index);
        alienDAO.save(location);
    }

    /**
     * Move a locationModifierReference to a position to another
     * 
     * @param location
     * @param from
     * @param to
     */
    public void move(Location location, int from, int to) {
        LocationModifierReference modifier = location.getModifiers().remove(from);
        if (modifier == null) { // TODO && check index => throw not found

        }
        location.getModifiers().add(to, modifier);
        alienDAO.save(location);
    }
}
