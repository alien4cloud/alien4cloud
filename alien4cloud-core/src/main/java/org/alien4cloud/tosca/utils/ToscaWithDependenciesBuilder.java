package org.alien4cloud.tosca.utils;

import java.util.Collections;

import org.alien4cloud.tosca.model.definitions.CapabilityDefinition;
import org.alien4cloud.tosca.model.definitions.RequirementDefinition;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;

import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.context.ToscaContextual;

/**
 * Utility to fetch tosca types, topologies etc with all their dependencies (other types etc.).
 */
@Service
public class ToscaWithDependenciesBuilder {
    public final static String NODE_TYPES_KEY = "nodeTypes";
    public final static String CAPABILITY_TYPES_KEY = "capabilityTypes";
    public final static String DATA_TYPES_KEY = "dataTypes";
    public final static String RELATIONSHIP_TYPES_KEY = "relationshipTypes";

    /**
     * Return the given tosca type with all related dependencies. Note if a tosca context exists it will reuse it, if not it will create a new Tosca Context
     * based on the dependencies of the type's archive.
     * 
     * @param toscaType The tosca type from which to extract the dependencies.
     * @return a TypeWithDependenciesResult instance that contains the type and all related types.
     */
    @ToscaContextual
    public TypeWithDependenciesResult buildTypeWithDependencies(AbstractToscaType toscaType) {
        if (toscaType instanceof NodeType) {
            return buildNodeTypeWithDependencies((NodeType) toscaType);
        }
        throw new NotImplementedException("Type not currently supported.");
    }

    private TypeWithDependenciesResult buildNodeTypeWithDependencies(NodeType nodeType) {
        TypeWithDependenciesResult result = new TypeWithDependenciesResult();
        result.setToscaType(nodeType);

        for (CapabilityDefinition capabilityDefinition : nodeType.getCapabilities()) {
            result.add(ToscaContext.getOrFail(CapabilityType.class, capabilityDefinition.getType()));
        }
        for (RequirementDefinition requirementDefinition : nodeType.getRequirements()) {
            CapabilityType capabilityType = ToscaContext.get(CapabilityType.class, requirementDefinition.getType());
            if (capabilityType != null) {
                result.add(capabilityType);
            } else {
                // requirements are authorized to be a node type rather than a capability type TODO is it still possible in TOSCA ?
                result.add(ToscaContext.get(NodeType.class, requirementDefinition.getType()));
            }
        }
        result.setDependencies(ToscaContext.get().getDependencies());
        // Fetch data types for the given type
        DataTypesFetcher.getDataTypesDependencies(Collections.singleton(nodeType), ToscaContext::get).values().forEach(result::add);
        return result;
    }
}
