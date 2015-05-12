package alien4cloud.paas.plan;

import java.nio.file.Path;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.model.components.IValue;
import alien4cloud.model.components.ImplementationArtifact;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A workflow step that trigger (remotely or not) a relationship operation.
 *
 * @author igor ngouagna
 */
@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
public class RelationshipTriggerEvent extends AbstractWorkflowStep {
    /** Id of the node tempalte for which the operation is related. */
    private String nodeTemplateId;
    /** If the operation is part of a relationship, the id of the relationship, null if not. */
    private String relationshipId;
    /** The source id of the relationship **/
    private String sourceRelationshipId;
    /** The name of the interface that contains the operation. */
    private String interfaceName;
    /** The name of the operation to call. */
    private String operationName;
    /** Path of the CSAR that contains the implementation artifact for the operation. **/
    @JsonIgnore
    private Path csarPath;
    /** Id of the side node template for which the operation is related. */
    private String sideNodeTemplateId;
    /** The name of the side operation. */
    private String sideOperationName;
    /** The artifact that implements the side operation. */
    private ImplementationArtifact sideOperationImplementationArtifact;
    /** The inputs parameters of the side operation. */
    private Map<String, IValue> sideInputParameters;
}