package alien4cloud.model.topology;

import java.util.Map;

import alien4cloud.model.components.DeploymentArtifact;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.json.deserializer.PropertyValueDeserializer;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.utils.jackson.ConditionalAttributes;
import alien4cloud.utils.jackson.ConditionalOnAttribute;
import alien4cloud.utils.jackson.JSonMapEntryArrayDeSerializer;
import alien4cloud.utils.jackson.JSonMapEntryArraySerializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Abstract template is parent of {@link NodeTemplate} and {@link RelationshipTemplate}.
 *
 * @author luc boutier
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public abstract class AbstractTemplate {

    /**
     * The QName value of this attribute refers to the Node Type providing the type of the Node Template.
     *
     * Note: If the Node Type referenced by the type attribute of a Node Template is declared as abstract, no instances of the specific Node Template can be
     * created. Instead, a substitution of the Node Template with one having a specialized, derived Node Type has to be done at the latest during the
     * instantiation time of the Node Template.
     */
    private String type;

    /**
     * Properties of the template
     */
    @ConditionalOnAttribute(ConditionalAttributes.REST)
    @JsonDeserialize(using = JSonMapEntryArrayDeSerializer.class, contentUsing = PropertyValueDeserializer.class)
    @JsonSerialize(using = JSonMapEntryArraySerializer.class)
    private Map<String, AbstractPropertyValue> properties;

    /**
     * The deployment artifacts
     */
    private Map<String, DeploymentArtifact> artifacts;
}
