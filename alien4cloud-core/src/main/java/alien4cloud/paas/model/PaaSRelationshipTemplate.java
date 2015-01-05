package alien4cloud.paas.model;

import java.nio.file.Path;

import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.paas.IPaaSTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
public class PaaSRelationshipTemplate implements IPaaSTemplate<IndexedRelationshipType> {
    private String id;
    private String source;
    private RelationshipTemplate relationshipTemplate;
    private IndexedRelationshipType indexedRelationshipType;
    private Path csarPath;

    public PaaSRelationshipTemplate(String id, RelationshipTemplate wrapped, String source) {
        this.id = id;
        this.relationshipTemplate = wrapped;
        this.source = source;
    }

    @Override
    public void setIndexedToscaElement(IndexedRelationshipType indexedRelationshipType) {
        this.indexedRelationshipType = indexedRelationshipType;
    }

    /**
     * Check if the relationship is an instance of the given type.
     * 
     * @param type The type we want to check.
     * @return True if the current relationship template is of the required type.
     */
    public boolean instanceOf(String type) {
        if (indexedRelationshipType.getDerivedFrom() == null) {
            return type.equals(indexedRelationshipType.getElementId());
        }
        return type.equals(indexedRelationshipType.getElementId()) || indexedRelationshipType.getDerivedFrom().contains(type);
    }
}