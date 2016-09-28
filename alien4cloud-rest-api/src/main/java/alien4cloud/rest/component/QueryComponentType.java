package alien4cloud.rest.component;

import org.alien4cloud.tosca.model.types.ArtifactType;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.model.types.AbstractToscaType;

/**
 * Enumeration of the components types available for search.
 */
public enum QueryComponentType {
    NODE_TYPE(NodeType.class), CAPABILITY_TYPE(CapabilityType.class), RELATIONSHIP_TYPE(RelationshipType.class), ARTIFACT_TYPE(
            ArtifactType.class);

    private final Class<? extends AbstractToscaType> indexedToscaElementClass;

    private QueryComponentType(Class<? extends AbstractToscaType> matchingClass) {
        this.indexedToscaElementClass = matchingClass;
    }

    public Class<? extends AbstractToscaType> getIndexedToscaElementClass() {
        return indexedToscaElementClass;
    }

    public String getMatchingClassName() {
        return indexedToscaElementClass.getName();
    }

    /**
     * Get the {@link QueryComponentType} from the indexed tosca element class.
     * 
     * @param componentClass The class for which to get the {@link QueryComponentType}.
     * @return The matching {@link QueryComponentType} if found or null if no type matches the given class.
     */
    public static QueryComponentType valueOf(Class<? extends AbstractToscaType> componentClass) {
        for (QueryComponentType type : QueryComponentType.values()) {
            if (type.getMatchingClassName().equals(componentClass.getName())) {
                return type;
            }
        }
        return null;
    }
}