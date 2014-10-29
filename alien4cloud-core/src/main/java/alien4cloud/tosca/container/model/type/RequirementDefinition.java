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
 * Specifies the requirements that the Node Type exposes.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = { "id" })
@SuppressWarnings("PMD.UnusedPrivateField")
@FormProperties({ "type", "lowerBound", "upperBound" })
public class RequirementDefinition {
    private String id;
    /**
     * <p>
     * Identifies the type of the requirement.
     * </p>
     * <p>
     * This must be a qualified name: Either namespace:type, either type only if the {@link capability type} is defined in the same namespace as the
     * {@link RequirementDefinition definition}.
     * </p>
     */
    @FormSuggestion(fromClass = IndexedCapabilityType.class, path = "elementId")
    private String type;
    /** Constraints to specify on the target node's properties. */
    private Map<String, PropertyConstraint> constraints;
    /** Specifies the default relationship type to be used for the relationship. This can be overriden by user but should be used as default. */
    private String relationshipType;
    /**
     * Specifies the lower boundary by which a requirement MUST be matched for Node Templates according to the current Node Type, or for instances created for
     * those Node Templates. The default value for this attribute is one. A value of zero would indicate that matching of the requirement is optional.
     */
    private int lowerBound = 1;
    /**
     * Specifies the upper boundary by which a requirement MUST be matched for Node Templates according to the current Node Type, or for instances created for
     * those Node Templates. The default value for this attribute is one. A value of “unbounded�? indicates that there is no upper boundary.
     */
    @JsonDeserialize(using = BoundDeserializer.class)
    @JsonSerialize(using = BoundSerializer.class)
    private int upperBound = 1;
}