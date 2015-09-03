package alien4cloud.orchestrators.rest.model;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;

@Getter
@Setter
public class LocationResources {

    private Map<String, IndexedNodeType> configurationTypes;

    private Map<String, IndexedNodeType> nodeTypes;

    private Map<String, IndexedCapabilityType> capabilityTypes;

    private List<LocationResourceTemplate> configurationTemplates;

    private List<LocationResourceTemplate> nodeTemplates;
}
