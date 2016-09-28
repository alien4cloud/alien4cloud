package alien4cloud.paas.model;

import java.nio.file.Path;

import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;

@Getter
@Setter
public class PaaSRelationshipTemplate extends AbstractPaaSTemplate<RelationshipType, RelationshipTemplate> {
    private String source;
    private Path csarPath;

    public PaaSRelationshipTemplate(String id, RelationshipTemplate wrapped, String source) {
        super(id, wrapped);
        this.source = source;
    }

    /**
     * Check if the relationship is an instance of the given type.
     *
     * @param type The type we want to check.
     * @return True if the current relationship template is of the required type.
     */
    public boolean instanceOf(String type) {
        if (getIndexedToscaElement().getDerivedFrom() == null) {
            return type.equals(getIndexedToscaElement().getElementId());
        }
        return type.equals(getIndexedToscaElement().getElementId()) || getIndexedToscaElement().getDerivedFrom().contains(type);
    }

    /**
     * @deprecated use {@link #getTemplate()} instead.
     */
    public RelationshipTemplate getRelationshipTemplate() {
        return getTemplate();
    }

}