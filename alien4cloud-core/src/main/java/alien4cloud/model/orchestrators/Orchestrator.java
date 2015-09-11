package alien4cloud.model.orchestrators;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;
import org.hibernate.validator.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wordnik.swagger.annotations.ApiModel;

/**
 * Defines an orchestrator instance that alien will use to manage one or multiple locations.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@ESObject
@ApiModel(value = "Orchestrator.", description = "An orchestrator is alien 4 cloud is a software engine that alien 4 cloud connects to in order to orchestrate"
        + " a topology deployment. An orchestrator may manage one or multiple locations.")
public class Orchestrator {
    @Id
    private String id;

    @NotBlank
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String name;

    /** Information on the plugin used to communicate with the orchestrator. */
    @NotBlank
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String pluginId;

    @NotBlank
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String pluginBean;

    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String deploymentNamePattern = "application.name + '-' + environment.name";

    /** Orchestrator's last known status . */
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private OrchestratorState state;

    @StringField(indexType = IndexType.analyzed, includeInAll = true)
    private List<String> authorizedUsers;

    @StringField(indexType = IndexType.analyzed, includeInAll = true)
    private List<String> authorizedGroups;
}