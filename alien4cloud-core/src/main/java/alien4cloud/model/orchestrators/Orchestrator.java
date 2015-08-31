package alien4cloud.model.orchestrators;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;
import org.hibernate.validator.constraints.NotBlank;

import alien4cloud.model.common.IMetaProperties;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wordnik.swagger.annotations.ApiModel;

import javax.validation.constraints.AssertFalse;

/**
 * Defines an orchestrators instance that alien will use to manage one or multiple locations.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings("PMD.UnusedPrivateField")
@ESObject
@ApiModel(value = "Orchestrator.", description = "An orchestrators represents the basic.")
public class Orchestrator implements IMetaProperties {
    @Id
    private String id;

    @NotBlank
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String name;

    /** Informations on the plugin used to communicate with the orchestrators. */
    @NotBlank
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String pluginId;

    @NotBlank
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String pluginBean;

    /** Last known status of the orchestrators. */
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private OrchestratorState state;

    @StringField(indexType = IndexType.analyzed, includeInAll = true)
    private Map<String, String> metaProperties;

    @StringField(indexType = IndexType.analyzed, includeInAll = true)
    private List<String> authorizedUsers;

    @StringField(indexType = IndexType.analyzed, includeInAll = true)
    private List<String> authorizedGroups;
}