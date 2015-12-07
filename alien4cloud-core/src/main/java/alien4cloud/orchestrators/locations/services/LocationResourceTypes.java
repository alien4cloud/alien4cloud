package alien4cloud.orchestrators.locations.services;

import java.util.Map;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedNodeType;

import com.google.common.collect.Maps;

@Getter
@Setter
@NoArgsConstructor
public class LocationResourceTypes {
    @ApiModelProperty(value = "Map of node types id, node type used to configure a given location.")
    private Map<String, IndexedNodeType> configurationTypes = Maps.newHashMap();
    @ApiModelProperty(value = "Map of node types id, node type used to configure the templates of on-demand resources in a location.")
    private Map<String, IndexedNodeType> nodeTypes = Maps.newHashMap();
    @ApiModelProperty(value = "Map that contains the capability types used by the configuration types or node types.")
    private Map<String, IndexedCapabilityType> capabilityTypes = Maps.newHashMap();
    @ApiModelProperty(value = "Map that contains all node types.")
    private Map<String, IndexedNodeType> allNodeTypes = Maps.newHashMap();
    @ApiModelProperty(value = "Map that contains the on demdand types.")
    private Map<String, IndexedNodeType> onDemandTypes = Maps.newHashMap();

    public LocationResourceTypes(LocationResourceTypes locationResourceTypes) {
        this.configurationTypes = locationResourceTypes.getConfigurationTypes();
        this.nodeTypes = locationResourceTypes.getNodeTypes();
        this.capabilityTypes = locationResourceTypes.getCapabilityTypes();
        this.allNodeTypes = locationResourceTypes.getAllNodeTypes();
        this.onDemandTypes = locationResourceTypes.getOnDemandTypes();
    }
}
