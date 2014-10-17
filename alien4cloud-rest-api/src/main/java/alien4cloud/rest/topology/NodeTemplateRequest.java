package alien4cloud.rest.topology;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.hibernate.validator.constraints.NotBlank;

/**
 * a nodetemplate request object
 * 
 * @author 'Igor Ngouagna'
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class NodeTemplateRequest {
    /** the name of the node template */
    @NotBlank
    private String name;
    /** related NodeType id */
    @NotBlank
    private String indexedNodeTypeId;

    public NodeTemplateRequest(String name, String indexedNodeTypeId) {
        this.name = name;
        this.indexedNodeTypeId = indexedNodeTypeId;
    }
}