package alien4cloud.tosca.container.model;

import java.util.Map;

import javax.validation.Valid;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.component.model.IndexedNodeType;
import alien4cloud.tosca.container.model.type.AttributeDefinition;
import alien4cloud.tosca.model.PropertyDefinition;
import alien4cloud.ui.form.annotation.FormSuggestion;

/**
 * A tosca element that supports inheritance.
 * 
 * @author luc boutier
 */
@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
public class ToscaInheritableElement extends ToscaElement {
    private boolean isAbstract = false;
    private boolean isFinal = false;
    @FormSuggestion(fromClass = IndexedNodeType.class, path = "elementId")
    private String derivedFrom;
    /**
     * This element specifies the structure of the observable properties of the Requirement Type, such as its configuration and state.
     */
    @Valid
    private Map<String, PropertyDefinition> properties;

    /**
     * This element specifies the structure of the attributes of a tosca element.
     */
    @Valid
    private Map<String, AttributeDefinition> attributes;
}