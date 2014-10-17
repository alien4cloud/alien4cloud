package alien4cloud.tosca.container.model.topology;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
}
