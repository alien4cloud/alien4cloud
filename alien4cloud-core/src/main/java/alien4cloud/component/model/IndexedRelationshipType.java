package alien4cloud.component.model;

import static alien4cloud.dao.model.FetchContext.COMPONENT_SUMMARY;
import static alien4cloud.dao.model.FetchContext.QUICK_SEARCH;
import static alien4cloud.dao.model.FetchContext.TAG_SUGGESTION;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.query.FetchContext;
import org.elasticsearch.annotation.query.TermsFacet;

import alien4cloud.tosca.container.model.type.AttributeDefinition;
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

    @FetchContext(contexts = { COMPONENT_SUMMARY, QUICK_SEARCH, TAG_SUGGESTION }, include = { false, false, false })
    private Map<String, AttributeDefinition> attributes;

    private Map<String, Interface> interfaces;
}