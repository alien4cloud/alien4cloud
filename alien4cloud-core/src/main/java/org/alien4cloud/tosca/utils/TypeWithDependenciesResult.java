package org.alien4cloud.tosca.utils;

import java.util.Map;
import java.util.Set;

import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.DataType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Result of the request of a type with all it's dependencies.
 */
@Getter
@Setter
@ApiModel(value = "Advanced API result that provides a TOSCA type with all related types used in the given type.")
public class TypeWithDependenciesResult {
    @ApiModelProperty(value = "The requested tosca type.")
    private AbstractToscaType toscaType;
    @ApiModelProperty(value = "The dependencies as defined in the archive that defines the requested TOSCA type with the addition of the actual archive that defines the type.")
    private Set<CSARDependency> dependencies = Sets.newHashSet();
    @ApiModelProperty(value = "The tosca node types used in the requested tosca type if any (capability types for node templates, data types etc.).")
    private Map<String, NodeType> nodeTypes = Maps.newHashMap();
    @ApiModelProperty(value = "The tosca relationship types used in the requested tosca type if any (capability types for node templates, data types etc.).")
    private Map<String, RelationshipType> relationshipTypes = Maps.newHashMap();
    @ApiModelProperty(value = "The tosca capability types used in the requested tosca type if any (capability types for node templates, data types etc.).")
    private Map<String, CapabilityType> capabilityTypes = Maps.newHashMap();
    @ApiModelProperty(value = "The tosca data types used in the requested tosca type if any (capability types for node templates, data types etc.).")
    private Map<String, DataType> dataTypes = Maps.newHashMap();

    /**
     * Add a node type.
     *
     * @param nodeType The node type to add.
     */
    public void add(NodeType nodeType) {
        nodeTypes.put(nodeType.getElementId(), nodeType);
    }

    /**
     * Add a relationship type.
     *
     * @param relationshipType The relationship type to add.
     */
    public void add(RelationshipType relationshipType) {
        relationshipTypes.put(relationshipType.getElementId(), relationshipType);
    }

    /**
     * Add a capability type.
     * 
     * @param capabilityType The capability type to add.
     */
    public void add(CapabilityType capabilityType) {
        capabilityTypes.put(capabilityType.getElementId(), capabilityType);
    }

    /**
     * Add a data type.
     *
     * @param dataType The data type to add.
     */
    public void add(DataType dataType) {
        dataTypes.put(dataType.getElementId(), dataType);
    }
}