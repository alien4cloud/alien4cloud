package alien4cloud.paas.model;

import java.nio.file.Path;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.model.topology.AbstractTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.paas.IPaaSTemplate;

@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
public class PaaSRelationshipTemplate implements IPaaSTemplate<IndexedRelationshipType> {
    private String id;
    private String source;
    private RelationshipTemplate relationshipTemplate;
    private IndexedRelationshipType indexedToscaElement;
    private Path csarPath;

    public PaaSRelationshipTemplate(String id, RelationshipTemplate wrapped, String source) {
        this.id = id;
        this.relationshipTemplate = wrapped;
        this.source = source;
    }

    /**
     * Check if the relationship is an instance of the given type.
     *
     * @param type The type we want to check.
     * @return True if the current relationship template is of the required type.
     */
    public boolean instanceOf(String type) {
        if (indexedToscaElement.getDerivedFrom() == null) {
            return type.equals(indexedToscaElement.getElementId());
        }
        return type.equals(indexedToscaElement.getElementId()) || indexedToscaElement.getDerivedFrom().contains(type);
    }

    @Override
    public AbstractTemplate getTemplate() {
        return relationshipTemplate;
    }
}