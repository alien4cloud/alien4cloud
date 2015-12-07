package alien4cloud.orchestrators.locations.services;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedNodeType;

import com.google.common.collect.Maps;

@Getter
@Setter
public class LocationResourceTypes {
    private Map<String, IndexedNodeType> nodeTypes = Maps.newHashMap();
    private Map<String, IndexedCapabilityType> capabilityTypes = Maps.newHashMap();
}