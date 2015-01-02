package alien4cloud.rest.component;

import alien4cloud.model.components.IndexedArtifactType;
import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.model.components.IndexedToscaElement;

/**
 * Enumeration of the components types available for search.
 */
public enum QueryComponentType {
    NODE_TYPE(IndexedNodeType.class), CAPABILITY_TYPE(IndexedCapabilityType.class), RELATIONSHIP_TYPE(IndexedRelationshipType.class), ARTIFACT_TYPE(
            IndexedArtifactType.class);

    private final Class<? extends IndexedToscaElement> indexedToscaElementClass;

    private QueryComponentType(Class<? extends IndexedToscaElement> matchingClass) {
        this.indexedToscaElementClass = matchingClass;
    }

    public Class<? extends IndexedToscaElement> getIndexedToscaElementClass() {
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
    public static QueryComponentType valueOf(Class<? extends IndexedToscaElement> componentClass) {
        for (QueryComponentType type : QueryComponentType.values()) {
            if (type.getMatchingClassName().equals(componentClass.getName())) {
                return type;
            }
        }
        return null;
    }
}