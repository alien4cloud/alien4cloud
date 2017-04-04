package alien4cloud.tosca.parser.postprocess;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.tosca.context.ToscaContext;
import lombok.Setter;

@Component
public class CapabilityMatcherService implements ICapabilityMatcherService {

    public interface IToscaContextFinder {
        <T extends AbstractToscaType> T find(Class<T> clazz, String id);
    }

    @Setter
    private IToscaContextFinder toscaContextFinder;

    public CapabilityMatcherService() {
        toscaContextFinder = ToscaContext::get;
    }

    @Override
    public Map<String, Capability> getCompatibleCapabilityByType(NodeTemplate nodeTemplate, String type) {
        Map<String, Capability> capabilities = nodeTemplate.getCapabilities();
        if (capabilities == null) {
            return Collections.emptyMap();
        }

        Map<String, Capability> targetCapabilitiesMatch = Maps.newHashMap();
        Set<String> allTypes = Sets.newHashSet();
        for (Map.Entry<String, Capability> capabilityEntry : capabilities.entrySet()) {
            CapabilityType capabilityType = toscaContextFinder.find(CapabilityType.class, capabilityEntry.getKey());
            allTypes.clear();
            allTypes.add(capabilityType.getElementId());
            allTypes.addAll(capabilityType.getDerivedFrom());

            if (allTypes.contains(type)) {
                targetCapabilitiesMatch.put(capabilityEntry.getKey(), capabilityEntry.getValue());
            }
        }
        return targetCapabilitiesMatch;
    }
}
