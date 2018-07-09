package alien4cloud.model.runtime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.elasticsearch.annotation.BooleanField;
import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

/**
 * An instance of a workflow step for a given node instance.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
@ESObject
public class WorkflowStepInstance {

    /** Unique id of the step instance. */
    @Id
    private String id;

    /** The name if the step in the workflow. */
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String stepId;

    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String deploymentId;

    /** The ID of the execution. **/
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String executionId;

    /** The id of the node. **/
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String nodeId;

    /** The id of the node instance. **/
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String instanceId;

    /** The id of the target node (if this step is related to a relationship). **/
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String targetNodeId;

    /** The id of the target node instance (if this step is related to a relationship). **/
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String targetInstanceId;

    /** The name of the operation (provided by the orchestrator). **/
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String operationName;

    /** Indicates if this step has failed tasks. */
    @BooleanField
    private boolean hasFailedTasks;

    @StringField(indexType = IndexType.not_analyzed)
    private WorkflowStepInstanceStatus status;

}