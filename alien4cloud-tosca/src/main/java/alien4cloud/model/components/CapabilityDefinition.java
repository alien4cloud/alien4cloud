package alien4cloud.model.components;

import java.util.List;
import java.util.Map;

import org.elasticsearch.annotation.query.TermsFacet;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import alien4cloud.json.deserializer.BoundDeserializer;
import alien4cloud.json.serializer.BoundSerializer;
import alien4cloud.ui.form.annotation.FormProperties;
import alien4cloud.ui.form.annotation.FormSuggestion;
import lombok.*;

/**
 * Specifies the capabilities that the Node Type exposes.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
@EqualsAndHashCode(of = { "id" })
@FormProperties({ "type", "lowerBound", "upperBound" })
public class CapabilityDefinition implements UpperBoundedDefinition {
    private String id;
    private String description;
    /** Identifies the type of the capability. */
    @FormSuggestion(fromClass = IndexedCapabilityType.class, path = "elementId")
    private String type;

    /**
     * Specifies the upper boundary of client requirements the defined capability can serve. The default value for this attribute is unbounded. A value of
     * 'unbounded' indicates that there is no upper boundary.
     */
    @JsonDeserialize(using = BoundDeserializer.class)
    @JsonSerialize(using = BoundSerializer.class)
    private int upperBound = Integer.MAX_VALUE;

    /** Map of properties value(s) to define the capability. */
    private Map<String, List<String>> properties;

    @TermsFacet
    private String[] validSources;

    public CapabilityDefinition(String id, String type, int upperBound) {
        this.id = id;
        this.type = type;
        this.upperBound = upperBound;
    }
}