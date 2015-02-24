package alien4cloud.model.components;

import static alien4cloud.dao.model.FetchContext.COMPONENT_SUMMARY;
import static alien4cloud.dao.model.FetchContext.QUICK_SEARCH;
import static alien4cloud.dao.model.FetchContext.TAG_SUGGESTION;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.query.FetchContext;

import alien4cloud.json.deserializer.AbstractAttributeDeserializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
@JsonInclude(Include.NON_NULL)
public class IndexedArtifactToscaElement extends IndexedInheritableToscaElement {
    @FetchContext(contexts = { COMPONENT_SUMMARY, QUICK_SEARCH, TAG_SUGGESTION }, include = { false, false, false })
    private Map<String, DeploymentArtifact> artifacts;

    @FetchContext(contexts = { COMPONENT_SUMMARY, QUICK_SEARCH, TAG_SUGGESTION }, include = { false, false, false })
    @JsonDeserialize(contentUsing = AbstractAttributeDeserializer.class)
    private Map<String, IAttributeValue> attributes;

    @FetchContext(contexts = { COMPONENT_SUMMARY, QUICK_SEARCH, TAG_SUGGESTION }, include = { false, false, false })
    private Map<String, Interface> interfaces;
}