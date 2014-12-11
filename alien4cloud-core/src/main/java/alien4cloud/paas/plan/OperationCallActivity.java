package alien4cloud.paas.plan;

import java.nio.file.Path;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.tosca.model.IOperationParameter;
import alien4cloud.tosca.model.ImplementationArtifact;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A workflow step that calls an operation.
 *
 * @author luc boutier
 */
@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
public class OperationCallActivity extends AbstractWorkflowStep {
    /** Id of the node tempalte for which the operation is related. */
    private String nodeTemplateId;
    /** If the operation is part of a relationship, the id of the relationship, null if not. */
    private String relationshipId;
    /** The name of the interface that contains the operation. */
    private String interfaceName;
    /** The name of the operation to call. */
    private String operationName;
    /** Path of the CSAR that contains the implementation artifact for the operation. **/
    @JsonIgnore
    private Path csarPath;
    /** The artifact that implements the operation. */
    private ImplementationArtifact implementationArtifact;
    /** The inputs parameters of the the operation. */
    private Map<String, IOperationParameter> inputParameters;
    /** True if the artifact is related to the operation, false if the artifact is a single artifact for the whole interface (contains several methods) */
    private boolean isOperationArtifact = true;
}