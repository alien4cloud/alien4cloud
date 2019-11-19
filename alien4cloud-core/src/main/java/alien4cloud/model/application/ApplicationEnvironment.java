package alien4cloud.model.application;

import static alien4cloud.dao.model.FetchContext.SUMMARY;

import java.util.Map;
import java.util.Set;

import org.elasticsearch.annotation.*;
import org.elasticsearch.annotation.query.FetchContext;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import alien4cloud.model.deployment.IDeploymentSource;
import alien4cloud.security.ISecuredResource;
import alien4cloud.security.model.ApplicationEnvironmentRole;
import alien4cloud.utils.jackson.ConditionalAttributes;
import alien4cloud.utils.jackson.ConditionalOnAttribute;
import alien4cloud.utils.jackson.JSonMapEntryArrayDeSerializer;
import alien4cloud.utils.jackson.JSonMapEntryArraySerializer;
import alien4cloud.utils.jackson.NotAnalyzedTextMapEntry;
import lombok.Getter;
import lombok.Setter;

@ESObject(analyzerDefinitions = @IndexAnalyserDefinition(name = "case_insensitive_sort", filters = "lowercase", tokenizer = "keyword", type = "custom"))
@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class ApplicationEnvironment implements ISecuredResource, IDeploymentSource {
    @Id
    private String id;
    @TermFilter
    @StringFieldMulti(
            main = @StringField(includeInAll = false, indexType = IndexType.not_analyzed),
            multiNames = "lower_case",
            multi = @StringField(includeInAll = false, indexType = IndexType.analyzed, analyser = "case_insensitive_sort" ))
    private String name;
    @TermFilter
    private String description;
    @TermFilter
    @StringField(includeInAll = false, indexType = IndexType.not_analyzed)
    private String applicationId;
    @StringField(indexType = IndexType.not_analyzed)
    private EnvironmentType environmentType;
    @TermFilter
    @StringField(includeInAll = false, indexType = IndexType.not_analyzed)
    private String version;
    @TermFilter
    @StringField(includeInAll = false, indexType = IndexType.not_analyzed)
    private String topologyVersion;

    @TermFilter(paths = { "key", "value" })
    @NestedObject(nestedClass = NotAnalyzedTextMapEntry.class)
    @ConditionalOnAttribute(ConditionalAttributes.ES)
    @JsonDeserialize(using = JSonMapEntryArrayDeSerializer.class)
    @JsonSerialize(using = JSonMapEntryArraySerializer.class)
    @FetchContext(contexts = { SUMMARY }, include = { true })
    private Map<String, Set<String>> userRoles;

    @TermFilter(paths = { "key", "value" })
    @NestedObject(nestedClass = NotAnalyzedTextMapEntry.class)
    @ConditionalOnAttribute(ConditionalAttributes.ES)
    @JsonDeserialize(using = JSonMapEntryArrayDeSerializer.class)
    @JsonSerialize(using = JSonMapEntryArraySerializer.class)
    @FetchContext(contexts = { SUMMARY }, include = { true })
    private Map<String, Set<String>> groupRoles;

    @Override
    public Class<ApplicationEnvironmentRole> roleEnum() {
        return ApplicationEnvironmentRole.class;
    }
}
