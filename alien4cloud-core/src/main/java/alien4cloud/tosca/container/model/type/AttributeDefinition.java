package alien4cloud.tosca.container.model.type;

import alien4cloud.tosca.container.validation.ToscaPropertyType;
import alien4cloud.ui.form.annotation.FormValidValues;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 
 * @author 'Igor Ngouagna'
 * 
 */
@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class AttributeDefinition {
    @ToscaPropertyType
    @FormValidValues({ "boolean", "string", "float", "integer", "version" })
    private String type;
    private String description;
    private String value;
}
