package alien4cloud.model.components;

import static alien4cloud.dao.model.FetchContext.COMPONENT_SUMMARY;
import static alien4cloud.dao.model.FetchContext.QUICK_SEARCH;
import static alien4cloud.dao.model.FetchContext.TAG_SUGGESTION;

import java.util.List;
import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.query.FetchContext;
import org.elasticsearch.annotation.query.TermsFacet;

@Getter
@Setter
@EqualsAndHashCode(of = {}, callSuper = true)
@SuppressWarnings("PMD.UnusedPrivateField")
public class IndexedInheritableToscaElement extends IndexedToscaElement {

    @TermsFacet
    private boolean isAbstract;

    @FetchContext(contexts = { QUICK_SEARCH, TAG_SUGGESTION }, include = { false, false })
    @TermsFacet
    private List<String> derivedFrom;

    @FetchContext(contexts = { COMPONENT_SUMMARY, QUICK_SEARCH, TAG_SUGGESTION }, include = { false, false, false })
    private Map<String, PropertyDefinition> properties;
}