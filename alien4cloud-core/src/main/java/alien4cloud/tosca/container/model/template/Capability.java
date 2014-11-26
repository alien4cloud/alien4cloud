package alien4cloud.tosca.container.model.template;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.tosca.container.deserializer.PropertyValueDeserializer;
import alien4cloud.tosca.model.AbstractPropertyValue;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Capability for a node template. This should match a capability definition from the node's type.
 * 
 * @author luc boutier
 */
@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
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
    @JsonDeserialize(contentUsing = PropertyValueDeserializer.class)
    private Map<String, AbstractPropertyValue> properties;
}