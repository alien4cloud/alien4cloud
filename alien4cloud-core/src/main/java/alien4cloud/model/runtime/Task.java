package alien4cloud.model.runtime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.TimeStamp;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

import java.util.Date;

/**
 * An task is the smaller stuff that can be executed by an orchestrator.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
@ESObject
public class Task {

    /** Unique id of the task, provided by the orchestrator. */
    @Id
    private String id;

    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String deploymentId;

    /** The ID of the execution. **/
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String executionId;

    /** Optionally, the id of the workflow step this task is related to, if exist. **/
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String workflowStepInstanceId;

    /** The name of the operation (provided by the orchestrator). **/
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String operationName;

    /** If exist, the id of the node concerned by this task. **/
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String nodeId;

    /** If exist, the id of the instance concerned by this task. **/
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String instanceId;

    /** Schedule date of the task (the date the task was submited). */
    @TimeStamp(format = "", index = IndexType.not_analyzed)
    private Date scheduleDate;

    @StringField(indexType = IndexType.not_analyzed)
    private TaskStatus status;

    @StringField(indexType = IndexType.not_analyzed)
    private String details;
}