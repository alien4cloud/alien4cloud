package alien4cloud.model.common;

import static alien4cloud.dao.model.FetchContext.SUMMARY;

import java.util.Set;

import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.query.FetchContext;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Sets;

@ESObject
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SuggestionEntry {

    /**
     * elasticsearch index of suggestion, for example toscaelement
     */
    @FetchContext(contexts = { SUMMARY }, include = { true })
    private String esIndex;

    /**
     * elasticsearch type of suggestion, for example indexcapabilitytype
     */
    @FetchContext(contexts = { SUMMARY }, include = { true })
    private String esType;

    /**
     * id of the target's entity for suggestion (for example tosca.capabilities.OperatingSystem)
     */
    @FetchContext(contexts = { SUMMARY }, include = { true })
    private String targetElementId;

    /**
     * property of the target's entity that needs suggestion for its values ( for example type, distribution, architecture etc ...)
     */
    @FetchContext(contexts = { SUMMARY }, include = { true })
    private String targetProperty;

    /**
     * List of values that can be suggested for the property ( for example Windows, Linux, Mac OS etc ...)
     */
    @FetchContext(contexts = { SUMMARY }, include = { false })
    private Set<String> suggestions = Sets.newHashSet();

    public static String generateId(String esIndex, String esType, String targetElementId, String targetProperty) {
        return esIndex + ":" + esType + ":" + targetElementId + ":" + targetProperty;
    }

    @Id
    public String getId() {
        return generateId(esIndex, esType, targetElementId, targetProperty);
    }

    public void setId(String id) {
        // Id is auto-generated
    }
}
