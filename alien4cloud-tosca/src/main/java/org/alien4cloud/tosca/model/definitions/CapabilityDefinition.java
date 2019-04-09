package org.alien4cloud.tosca.model.definitions;

import java.util.Map;

import org.alien4cloud.tosca.model.types.CapabilityType;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.ObjectField;
import org.elasticsearch.annotation.query.TermsFacet;
import org.elasticsearch.mapping.IndexType;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import alien4cloud.json.deserializer.BoundDeserializer;
import alien4cloud.json.deserializer.PropertyValueDeserializer;
import alien4cloud.json.serializer.BoundSerializer;
import alien4cloud.ui.form.annotation.FormProperties;
import alien4cloud.ui.form.annotation.FormSuggestion;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Specifies the capabilities that the Node Type exposes.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = { "id" })
@FormProperties({ "type", "lowerBound", "upperBound" })
public class CapabilityDefinition implements UpperBoundedDefinition {
    private String id;
    private String description;
    /** Identifies the type of the capability. */
    @FormSuggestion(fromClass = CapabilityType.class, path = "elementId")
    @StringField(indexType = IndexType.analyzed)
    private String type;

    /**
     * Specifies the upper boundary of client requirements the defined capability can serve. The default value for this attribute is unbounded. A value of
     * 'unbounded' indicates that there is no upper boundary.
     */
    @JsonDeserialize(using = BoundDeserializer.class)
    @JsonSerialize(using = BoundSerializer.class)
    private int upperBound = Integer.MAX_VALUE;

    /** Map of properties value(s) to define the capability. */
    @ObjectField(enabled = false)
    @JsonDeserialize(contentUsing = PropertyValueDeserializer.class)
    private Map<String, AbstractPropertyValue> properties;

    @TermsFacet
    @StringField(indexType = IndexType.not_analyzed)
    private String[] validSources;

    /** Constructor for single line parsing definition based on type. */
    public CapabilityDefinition(String type) {
        this.type = type;
    }

    public CapabilityDefinition(String id, String type, int upperBound) {
        this.id = id;
        this.type = type;
        this.upperBound = upperBound;
    }
}
