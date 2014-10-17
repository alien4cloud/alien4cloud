package alien4cloud.tosca.container.model.type;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.component.model.IndexedCapabilityType;
import alien4cloud.tosca.container.deserializer.BoundDeserializer;
import alien4cloud.tosca.container.serializer.BoundSerializer;
import alien4cloud.ui.form.annotation.FormProperties;
import alien4cloud.ui.form.annotation.FormSuggestion;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Specifies the capabilities that the Node Type exposes.
 * 
 * @author luc boutier
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
    /**
     * <p>
     * Identifies the type of the capability.
     * </p>
     * <p>
     * This must be a qualified name: Either namespace:type, either type only if the {@link CapabilityType type} is defined in the same namespace as the
     * {@link RequirementDefinition definition}.
     * </p>
     */
    @FormSuggestion(fromClass = IndexedCapabilityType.class, path = "elementId")
    private String type;

    /**
     * Specifies the lower boundary of requiring nodes that the defined capability can serve. The default value for this attribute is one. A value of zero is
     * invalid, since this would mean that the capability cannot actually satisfy any requiring nodes.
     */
    private int lowerBound = 0;
    /**
     * Specifies the upper boundary of client requirements the defined capability can serve. The default value for this attribute is one. A value of
     * 'unbounded' indicates that there is no upper boundary.
     */
    @JsonDeserialize(using = BoundDeserializer.class)
    @JsonSerialize(using = BoundSerializer.class)
    private int upperBound = Integer.MAX_VALUE;

    /** Map of properties value(s) to define the capability. */
    private Map<String, String[]> properties;

    public CapabilityDefinition(String id, String type, int lowerBound, int upperBound) {
        this.id = id;
        this.type = type;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

}