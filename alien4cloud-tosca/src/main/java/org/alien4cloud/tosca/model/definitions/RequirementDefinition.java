package org.alien4cloud.tosca.model.definitions;

import org.alien4cloud.tosca.model.types.CapabilityType;
import org.elasticsearch.annotation.ObjectField;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import alien4cloud.json.deserializer.BoundDeserializer;
import alien4cloud.json.serializer.BoundSerializer;
import alien4cloud.ui.form.annotation.FormProperties;
import alien4cloud.ui.form.annotation.FormSuggestion;
import lombok.*;

/**
 * Specifies the requirements that the Node Type exposes.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
@EqualsAndHashCode(of = { "id" })
@FormProperties({ "type", "lowerBound", "upperBound" })
public class RequirementDefinition implements LowerBoundedDefinition, UpperBoundedDefinition {
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
    @FormSuggestion(fromClass = CapabilityType.class, path = "elementId")
    private String type;
    private String description;
    /** Specifies the default relationship type to be used for the relationship. This can be overriden by user but should be used as default. */
    private String relationshipType;
    /** Can specify the optional target capability name on which to bind the relationship. */
    private String capabilityName;
    /**
     * Specifies the lower boundary by which a requirement MUST be matched for Node Templates according to the current Node Type, or for instances created for
     * those Node Templates. The default value for this attribute is one. A value of zero would indicate that matching of the requirement is optional.
     */
    private int lowerBound = 1;
    /**
     * Specifies the upper boundary by which a requirement MUST be matched for Node Templates according to the current Node Type, or for instances created for
     * those Node Templates. The default value for this attribute is one. A value of "unbounded" indicates that there is no upper boundary.
     */
    @JsonDeserialize(using = BoundDeserializer.class)
    @JsonSerialize(using = BoundSerializer.class)
    private int upperBound = 1;

    @ObjectField(enabled = false)
    /** Constraints to specify on the target capability or node's properties. */
    private NodeFilter nodeFilter;

    /**
     * Quick constructor to create a requirement definition from id and type.
     * 
     * @param id The requirement id.
     * @param type The requirement type.
     */
    public RequirementDefinition(String id, String type) {
        this.id = id;
        this.type = type;
    }
}