package alien4cloud.orchestrators.locations.services;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.LocationModifierReference;
import com.google.common.collect.Lists;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.PluginModifierRegistry;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.alien4cloud.alm.deployment.configuration.flow.modifiers.FlowPhases.POST_INJECT_INPUT;
import static org.alien4cloud.alm.deployment.configuration.flow.modifiers.FlowPhases.POST_LOCATION_MATCH;
import static org.alien4cloud.alm.deployment.configuration.flow.modifiers.FlowPhases.POST_MATCHED_NODE_SETUP;
import static org.alien4cloud.alm.deployment.configuration.flow.modifiers.FlowPhases.POST_MATCHED_POLICY_SETUP;
import static org.alien4cloud.alm.deployment.configuration.flow.modifiers.FlowPhases.POST_NODE_MATCH;
import static org.alien4cloud.alm.deployment.configuration.flow.modifiers.FlowPhases.POST_POLICY_MATCH;
import static org.alien4cloud.alm.deployment.configuration.flow.modifiers.FlowPhases.PRE_INJECT_INPUT;
import static org.alien4cloud.alm.deployment.configuration.flow.modifiers.FlowPhases.PRE_MATCHED_NODE_SETUP;
import static org.alien4cloud.alm.deployment.configuration.flow.modifiers.FlowPhases.PRE_MATCHED_POLICY_SETUP;
import static org.alien4cloud.alm.deployment.configuration.flow.modifiers.FlowPhases.PRE_NODE_MATCH;
import static org.alien4cloud.alm.deployment.configuration.flow.modifiers.FlowPhases.PRE_POLICY_MATCH;

/**
 * Manages locations modifiers.
 */
@Service
public class LocationModifierService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Inject
    private PluginModifierRegistry pluginModifierRegistry;

    private static final List<String> validPhases = Arrays.asList(POST_LOCATION_MATCH, PRE_INJECT_INPUT, POST_INJECT_INPUT, PRE_POLICY_MATCH,POST_POLICY_MATCH,
            PRE_NODE_MATCH, POST_NODE_MATCH, PRE_MATCHED_POLICY_SETUP, POST_MATCHED_POLICY_SETUP, PRE_MATCHED_NODE_SETUP, POST_MATCHED_NODE_SETUP);

    private void sort(Location location) {
        Collections.sort(location.getModifiers(), new Comparator<LocationModifierReference>() {
            @Override
            public int compare(LocationModifierReference modifier1, LocationModifierReference modifier2) {
                return validPhases.indexOf(modifier1.getPhase()) - validPhases.indexOf(modifier2.getPhase());
            }
        });
    }

    /**
     * Add a locationModifierReference to an location
     * 
     * @param location
     * @param locationModifierReference to add
     */
    public void add(Location location, LocationModifierReference locationModifierReference) {
        pluginModifierRegistry.getPluginBean(locationModifierReference.getPluginId(), locationModifierReference.getBeanName());
        if (!validPhases.contains(locationModifierReference.getPhase())) {
            throw new InvalidArgumentException(locationModifierReference.getPhase() + " is not a valid phase for location matcher");
        }
        if (location.getModifiers() == null) {
            location.setModifiers(Lists.newArrayList());
        }
        location.getModifiers().add(locationModifierReference);
        sort(location);
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
        sort(location);
        alienDAO.save(location);
    }
}
