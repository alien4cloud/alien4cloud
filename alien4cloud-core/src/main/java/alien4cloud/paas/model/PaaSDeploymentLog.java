package alien4cloud.paas.model;

import java.util.Date;

import org.elasticsearch.annotation.ESObject;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

/**
 * This represents a log entry from orchestrator
 */
@Getter
@Setter
@ESObject
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaaSDeploymentLog {

    /**
     * Deployment PaaS id is unique for an application on a given environment (for ex : Alien-Prod)
     * It corresponds to {@link alien4cloud.model.deployment.Deployment#getOrchestratorDeploymentId}
     * This field is mandatory
     */
    private String deploymentPaaSId;

    /**
     * Log's level
     * This field is mandatory
     */
    private PaaSDeploymentLogLevel level;

    /**
     * The type of the log entry given by the PaaS
     * For cloudify it can be a4c_workflow_event, Task sent, Task started etc ...
     * For puccini it can be operation_output, operation_info, plugin, provider, general ...
     * This field is mandatory
     */
    private String type;

    /**
     * Log's timestamp
     * This field is mandatory
     */
    private Date timestamp;

    /**
     * Id of the workflow that generated the log
     * This field is optional
     */
    private String workflowId;

    /**
     * Id of the execution that generated the log
     * This field is optional
     */
    private String executionId;

    /**
     * Id of the node that generated the log
     * This field is optional
     */
    private String nodeId;

    /**
     * Id of the instance that generated the log
     * This field is optional
     */
    private String instanceId;

    /**
     * Interface on the node that generated the log
     * This field is optional
     */
    private String interfaceName;

    /**
     * Operation inside the interface on the node that generated the log
     * This field is optional
     */
    private String operationName;

    /**
     * Finally the log's content in free text
     * This field is mandatory
     */
    private String content;

    @Override
    public String toString() {
        return "PaaSDeploymentLog{" + "deploymentPaaSId='" + deploymentPaaSId + '\'' + ", level=" + level + ", type='" + type + '\'' + ", timestamp="
                + timestamp + ", workflowId='" + workflowId + '\'' + ", executionId='" + executionId + '\'' + ", nodeId='" + nodeId + '\'' + ", instanceId='"
                + instanceId + '\'' + ", interfaceName='" + interfaceName + '\'' + ", operationName='" + operationName + '\'' + ", content='" + content + '\''
                + '}';
    }
}
