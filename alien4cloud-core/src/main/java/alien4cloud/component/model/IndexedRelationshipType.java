package alien4cloud.component.model;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.query.TermsFacet;

import alien4cloud.tosca.container.model.type.Interface;

@Getter
@Setter
@ESObject
@SuppressWarnings("PMD.UnusedPrivateField")
public class IndexedRelationshipType extends IndexedArtifactToscaElement {
    @TermsFacet
    private String[] validSources;

    @TermsFacet
    private String[] validTargets;

    private Map<String, Interface> interfaces;
}