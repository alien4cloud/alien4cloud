package alien4cloud.model.runtime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.elasticsearch.annotation.*;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

import java.util.Date;

/**
 * An execution is related to a given deployment and concerns a given workflow.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
@ESObject
public class Execution {

    /** Unique id of the execution, provided by the orchestrator. */
    @Id
    private String id;

    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String deploymentId;

    /** The ID of the workflow provided by the orchestrator. **/
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String workflowId;

    /** The name of the workflow in A4C (can be different than the ID). **/
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String workflowName;

    /** The name of workflow that should be used to represent this executionn (some
     * workflow like scale should be viewed by the install or uninstall workflow graph). **/
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String displayWorkflowName;

    /** Start date of the execution */
    @TimeStamp(format = "", index = IndexType.not_analyzed)
    private Date startDate;

    /** End date of the execution. */
    @TimeStamp(format = "", index = IndexType.not_analyzed)
    private Date endDate;

    @StringField(indexType = IndexType.not_analyzed)
    private ExecutionStatus status;

    /** Indicates if this execution has failed tasks. */
    @TermFilter
    @BooleanField
    private boolean hasFailedTasks;
}