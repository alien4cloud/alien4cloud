package alien4cloud.model.components;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.json.deserializer.BoundDeserializer;
import alien4cloud.json.serializer.BoundSerializer;
import alien4cloud.ui.form.annotation.FormProperties;
import alien4cloud.ui.form.annotation.FormSuggestion;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Specifies the capabilities that the Node Type exposes.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = { "id" })
@FormProperties({ "type", "lowerBound", "upperBound" })
@SuppressWarnings("PMD.UnusedPrivateField")
public class CapabilityDefinition {
    private String id;
    private String description;
    /** Identifies the type of the capability. */
    @FormSuggestion(fromClass = IndexedCapabilityType.class, path = "elementId")
    private String type;

    /**
     * Specifies the upper boundary of client requirements the defined capability can serve. The default value for this attribute is one. A value of
     * 'unbounded' indicates that there is no upper boundary.
     */
    @JsonDeserialize(using = BoundDeserializer.class)
    @JsonSerialize(using = BoundSerializer.class)
    private int upperBound = Integer.MAX_VALUE;

    /** Map of properties value(s) to define the capability. */
    private Map<String, List<String>> properties;

    public CapabilityDefinition(String id, String type, int upperBound) {
        this.id = id;
        this.type = type;
        this.upperBound = upperBound;
    }
}