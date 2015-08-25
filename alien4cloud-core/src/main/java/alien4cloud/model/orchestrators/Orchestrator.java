package alien4cloud.model.orchestrators;

import static alien4cloud.dao.model.FetchContext.DEPLOYMENT;

import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.NestedObject;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.FetchContext;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;
import org.hibernate.validator.constraints.NotBlank;

import alien4cloud.model.common.IMetaProperties;
import alien4cloud.security.ISecuredResource;
import alien4cloud.security.model.OrchestratorRole;
import alien4cloud.utils.jackson.ConditionalAttributes;
import alien4cloud.utils.jackson.ConditionalOnAttribute;
import alien4cloud.utils.jackson.JSonMapEntryArrayDeSerializer;
import alien4cloud.utils.jackson.JSonMapEntryArraySerializer;
import alien4cloud.utils.jackson.NotAnalyzedTextMapEntry;

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
public class Orchestrator implements ISecuredResource, IMetaProperties {
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

    @StringField(indexType = IndexType.analyzed, includeInAll = true)
    private Map<String, String> metaProperties;

    @Override
    public Class<OrchestratorRole> roleEnum() {
        return OrchestratorRole.class;
    }
}