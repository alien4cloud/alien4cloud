package alien4cloud.model.common;

import java.util.Set;

import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.elasticsearch.annotation.query.FetchContext;

import static alien4cloud.dao.model.FetchContext.SUMMARY;

@ESObject
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SuggestionEntry {

    @Id
    @FetchContext(contexts = { SUMMARY }, include = { true })
    private String id;

    /**
     * elasticsearch index of suggestion, for example toscaelement
     */
    private String esIndex;

    /**
     * elasticsearch type of suggestion, for example indexcapabilitytype
     */
    private String esType;

    /**
     * id of the target's entity for suggestion (for example tosca.capabilities.OperatingSystem)
     */
    private String targetElementId;

    /**
     * property of the target's entity that needs suggestion for its values ( for example type, distribution, architecture etc ...)
     */
    private String targetProperty;

    /**
     * List of values that can be suggested for the property ( for example Windows, Linux, Mac OS etc ...)
     */
    private Set<String> suggestions;

    public void generateId() {
        this.id = esIndex + ":" + esType + ":" + targetElementId + ":" + targetProperty;
    }
}
