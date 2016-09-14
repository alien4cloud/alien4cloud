package org.alien4cloud.tosca.model.templates;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.json.deserializer.PropertyValueDeserializer;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import alien4cloud.utils.jackson.ConditionalAttributes;
import alien4cloud.utils.jackson.ConditionalOnAttribute;
import alien4cloud.utils.jackson.JSonMapEntryArrayDeSerializer;
import alien4cloud.utils.jackson.JSonMapEntryArraySerializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.elasticsearch.annotation.ObjectField;

/**
 * Capability for a node template. This should match a capability definition from the node's type.
 * 
 * @author luc boutier
 */
@Getter
@Setter
public class Capability {
    /**
     * The QName value of this attribute refers to the Capability Type definition of the Capability. This Capability Type denotes the semantics and well as
     * potential properties of the Capability.
     */
    private String type;
    /**
     * This element specifies initial values for one or more of the Capability Properties according to the Capability Type providing the property definitions.
     * Properties are provided in the form of an XML fragment. The same rules as outlined for the Properties element of the Node Template apply.
     */
    @ObjectField(enabled = false)
    @ConditionalOnAttribute(ConditionalAttributes.REST)
    @JsonDeserialize(using = JSonMapEntryArrayDeSerializer.class, contentUsing = PropertyValueDeserializer.class)
    @JsonSerialize(using = JSonMapEntryArraySerializer.class)
    private Map<String, AbstractPropertyValue> properties;
}