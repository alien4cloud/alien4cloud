package alien4cloud.model.deployment;

import java.util.Date;
import java.util.Map;

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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.collect.Maps;

/**
 * Represents a deployment in ALIEN.
 */
@Getter
@Setter
@AllArgsConstructor(suppressConstructorProperties = true)
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
@ESObject
public class Deployment {
    /** Unique id of the deployment as stored in Alien */
    @Id
    private String id;

    /**
     * Unique id of the deployment on the orchestration technology.
     * This is unique for deployments that have a null end date but you may have multiple completed deployments that share the same paasId.
     */
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String orchestratorDeploymentId;

    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private DeploymentSourceType sourceType;

    /** Id of the orchestrator that manages the deployment. */
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String orchestratorId;

    /** Id of the locations on which it is deployed. */
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String[] locationIds;

    /** Id of the application that has been deployed */
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String sourceId;

    /** Name of the application. This is used as backup if application is deleted. */
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String sourceName;

    /** Id of the environment on which this deployment has been created */
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String environmentId;

    /** Id of the version of the topology on which this deployment has been created */
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String versionId;

    /** Start date of the deployment */
    @TimeStamp(format = "")
    private Date startDate;

    /** End date of the deployment. */
    @TermFilter
    private Date endDate;

    /**
     * The last PaaS execution id per workflow.
     */
    private Map<String, String> workflowExecutions = Maps.newHashMap();
}