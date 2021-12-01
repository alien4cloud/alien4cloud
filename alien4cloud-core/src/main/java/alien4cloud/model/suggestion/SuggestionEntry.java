package alien4cloud.model.suggestion;

import static alien4cloud.dao.model.FetchContext.SUMMARY;
import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.query.FetchContext;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A suggestion linked to a indexed element's property.
 */
@ESObject
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SuggestionEntry extends AbstractSuggestionEntry {

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



    public static String generateId(String esIndex, String esType, String targetElementId, String targetProperty) {
        return esIndex + ":" + esType + ":" + targetElementId + ":" + targetProperty;
    }

    public String getId() {
        return generateId(esIndex, esType, targetElementId, targetProperty);
    }

    public void setId(String id) {
        // Id is auto-generated
    }
}
