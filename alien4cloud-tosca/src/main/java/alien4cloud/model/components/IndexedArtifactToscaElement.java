package alien4cloud.model.components;

import static alien4cloud.dao.model.FetchContext.QUICK_SEARCH;
import static alien4cloud.dao.model.FetchContext.SUMMARY;
import static alien4cloud.dao.model.FetchContext.TAG_SUGGESTION;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.query.FetchContext;

import alien4cloud.json.deserializer.AttributeDeserializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class IndexedArtifactToscaElement extends IndexedInheritableToscaElement {
    @FetchContext(contexts = { SUMMARY, QUICK_SEARCH, TAG_SUGGESTION }, include = { false, false, false })
    private Map<String, DeploymentArtifact> artifacts;

    @FetchContext(contexts = { SUMMARY, QUICK_SEARCH, TAG_SUGGESTION }, include = { false, false, false })
    @JsonDeserialize(contentUsing = AttributeDeserializer.class)
    private Map<String, IValue> attributes;

    @FetchContext(contexts = { SUMMARY, QUICK_SEARCH, TAG_SUGGESTION }, include = { false, false, false })
    private Map<String, Interface> interfaces;
}