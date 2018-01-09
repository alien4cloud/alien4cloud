package alien4cloud.model.common;

import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A suggestion not linked to a indexed element property (provided by a plugin for example).
 */
@ESObject
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SimpleSuggestionEntry extends AbstractSuggestionEntry {

    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
