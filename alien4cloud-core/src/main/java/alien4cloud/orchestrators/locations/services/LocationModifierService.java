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
     * @param location
     * @param locationModifierReference to add
     * @return true if added
     */
    public boolean add(Location location, LocationModifierReference locationModifierReference) {
        if (location.getModifiers() == null) {
            location.setModifiers(Lists.newArrayList());
        }
        boolean added = location.getModifiers().add(locationModifierReference);
        if (added) {
            alienDAO.save(location);
        }
        return added;
    }

    /**
     * Add a locationModifierReference to an location at the specific index
     * @param location
     * @param locationModifierReference to add
     * @param index
     * @return true if added or throw an IndexOutOfBoundsException
     */
    public boolean add(Location location, LocationModifierReference locationModifierReference, int index) {
        if (location.getModifiers() == null) {
            location.setModifiers(Lists.newArrayList());
        }
        location.getModifiers().add(index, locationModifierReference);
        alienDAO.save(location);
        return true;
    }

    /**
     * Remove a locationModifierReference
     * @param location
     * @param index of the locationModifierReference to remove
     * @return
     */
    public boolean remove(Location location, int index) {
        if (location.getModifiers() == null) {
            return false;
        }
        boolean removed = location.getModifiers().remove(location.getModifiers().get(index));
        if (removed) {
            alienDAO.save(location);
        }
        return removed;
    }

    /**
     * Move a locationModifierReference to a position to another
     * @param location
     * @param from
     * @param to
     * @return true if moved or throw an IndexOutOfBoundsException
     */
    public boolean move(Location location, int from, int to) {
        LocationModifierReference modifier = location.getModifiers().remove(from);
        if (modifier == null) {
            return false;
        }
        location.getModifiers().add(to, modifier);
        alienDAO.save(location);
        return true;
    }

    /**
     * Get all supported bean names by modifiers for a given location
     * @param location
     * @return
     */
    public Set<String> getAllBeanNames(Location location) {
        Set<String> beanNames = Sets.newHashSet();
        for (LocationModifierReference modifier : safe(location.getModifiers())) {
            beanNames.add(modifier.getBeanName());
        }
        return beanNames;
    }
}
