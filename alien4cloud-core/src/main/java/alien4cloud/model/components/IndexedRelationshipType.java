package alien4cloud.model.components;

import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.query.TermsFacet;

@Getter
@Setter
@ESObject
@SuppressWarnings("PMD.UnusedPrivateField")
public class IndexedRelationshipType extends IndexedArtifactToscaElement {
    @TermsFacet
    private String[] validSources;

    @TermsFacet
    private String[] validTargets;
}