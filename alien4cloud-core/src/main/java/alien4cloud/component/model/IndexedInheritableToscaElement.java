package alien4cloud.component.model;

import static alien4cloud.dao.model.FetchContext.*;

import java.util.Map;
import java.util.Set;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.query.FetchContext;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.annotation.query.TermsFacet;

import alien4cloud.tosca.container.model.type.AttributeDefinition;
import alien4cloud.tosca.container.model.type.PropertyDefinition;

@Getter
@Setter
@EqualsAndHashCode(of = {}, callSuper = true)
@SuppressWarnings("PMD.UnusedPrivateField")
public class IndexedInheritableToscaElement extends IndexedToscaElement {

    @TermsFacet
    private boolean isAbstract;

    @FetchContext(contexts = { QUICK_SEARCH, TAG_SUGGESTION }, include = { false, false })
    @TermsFacet
    private Set<String> derivedFrom;

    @FetchContext(contexts = { COMPONENT_SUMMARY, QUICK_SEARCH, TAG_SUGGESTION }, include = { false, false, false })
    private Map<String, PropertyDefinition> properties;

    @FetchContext(contexts = { COMPONENT_SUMMARY, QUICK_SEARCH, TAG_SUGGESTION }, include = { false, false, false })
    private Map<String, AttributeDefinition> attributes;
}