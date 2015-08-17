package alien4cloud.model.orchestrators;

import static alien4cloud.dao.model.FetchContext.DEPLOYMENT;

import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.*;
import org.elasticsearch.annotation.query.FetchContext;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;
import org.hibernate.validator.constraints.NotBlank;

import alien4cloud.security.ISecuredResource;
import alien4cloud.security.model.CloudRole;
import alien4cloud.utils.jackson.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.wordnik.swagger.annotations.ApiModel;

/**
 * Defines an orchestrators instance that alien will use to manage one or multiple locations.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings("PMD.UnusedPrivateField")
@ESObject
@ApiModel(value = "Orchestrator.", description = "An orchestrators represents the basic.")
public class Orchestrator implements ISecuredResource {
    @Id
    private String id;
    @NotBlank
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String name;
    /** Informations on the plugin used to communicate with the orchestrators. */
    @NotBlank
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String pluginId;
    @NotBlank
    @StringField(indexType = IndexType.not_analyzed)
    private String pluginBean;
    @BooleanField(index = IndexType.no)
    private boolean isMultipleLocations;

    /** Last known status of the orchestrators. */
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private OrchestratorStatus status;

    @TermFilter(paths = { "key", "value" })
    @NestedObject(nestedClass = NotAnalyzedTextMapEntry.class)
    @ConditionalOnAttribute(ConditionalAttributes.ES)
    @JsonDeserialize(using = JSonMapEntryArrayDeSerializer.class)
    @JsonSerialize(using = JSonMapEntryArraySerializer.class)
    @FetchContext(contexts = { DEPLOYMENT }, include = { true })
    private Map<String, Set<String>> userRoles;

    @TermFilter(paths = { "key", "value" })
    @NestedObject(nestedClass = NotAnalyzedTextMapEntry.class)
    @ConditionalOnAttribute(ConditionalAttributes.ES)
    @JsonDeserialize(using = JSonMapEntryArrayDeSerializer.class)
    @JsonSerialize(using = JSonMapEntryArraySerializer.class)
    @FetchContext(contexts = { DEPLOYMENT }, include = { true })
    private Map<String, Set<String>> groupRoles;

    @Override
    public Class<CloudRole> roleEnum() {
        return CloudRole.class;
    }
}