package alien4cloud.orchestrators.locations.services;

import java.util.Map;
import java.util.Set;

import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.DataType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.PolicyType;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LocationResourceTypes {
    @ApiModelProperty(value = "Map of node types id, node type used to configure a given location.")
    private Map<String, NodeType> configurationTypes = Maps.newHashMap();
    @ApiModelProperty(value = "Map of node types id, node type used to configure the templates of on-demand resources in a location.")
    private Map<String, NodeType> nodeTypes = Maps.newHashMap();
    @ApiModelProperty(value = "Map of data types id, data type used to configure the templates of on-demand resources in a location.")
    private Map<String, DataType> dataTypes = Maps.newHashMap();
    @ApiModelProperty(value = "List of recommended node types ID, e.g. defined at the orchestrator level")
    private Set<String> providedTypes = Sets.newHashSet();
    @ApiModelProperty(value = "Map that contains the capability types used by the configuration types or node types.")
    private Map<String, CapabilityType> capabilityTypes = Maps.newHashMap();
    @ApiModelProperty(value = "Map that contains all node types.")
    private Map<String, NodeType> allNodeTypes = Maps.newHashMap();
    @ApiModelProperty(value = "Map that contains the on demdand types.")
    private Map<String, NodeType> onDemandTypes = Maps.newHashMap();
    @ApiModelProperty(value = "Map of policy types id, policy type used to configure the templates of policies in a location.")
    private Map<String, PolicyType> policyTypes = Maps.newHashMap();

    public LocationResourceTypes(LocationResourceTypes locationResourceTypes) {
        this.configurationTypes = locationResourceTypes.getConfigurationTypes();
        this.nodeTypes = locationResourceTypes.getNodeTypes();
        this.dataTypes = locationResourceTypes.getDataTypes();
        this.capabilityTypes = locationResourceTypes.getCapabilityTypes();
        this.allNodeTypes = locationResourceTypes.getAllNodeTypes();
        this.onDemandTypes = locationResourceTypes.getOnDemandTypes();
        this.policyTypes = locationResourceTypes.getPolicyTypes();
    }
}
