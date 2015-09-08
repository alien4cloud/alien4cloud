package alien4cloud.model.application;

import static alien4cloud.dao.model.FetchContext.DEPLOYMENT;

import java.util.Map;
import java.util.Set;

import alien4cloud.security.model.ApplicationEnvironmentRole;
import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.NestedObject;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.FetchContext;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

import alien4cloud.model.deployment.IDeploymentSource;
import alien4cloud.security.ISecuredResource;
import alien4cloud.utils.jackson.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@ESObject
@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class ApplicationEnvironment implements ISecuredResource, IDeploymentSource {

    @Id
    private String id;
    @TermFilter
    @StringField(includeInAll = false, indexType = IndexType.not_analyzed)
    private String name;
    @TermFilter
    private String description;
    @TermFilter
    @StringField(includeInAll = false, indexType = IndexType.not_analyzed)
    private String applicationId;
    @StringField(includeInAll = true, indexType = IndexType.not_analyzed)
    private EnvironmentType environmentType;
    @TermFilter
    @StringField(includeInAll = false, indexType = IndexType.not_analyzed)
    private String currentVersionId;

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
    public Class<ApplicationEnvironmentRole> roleEnum() {
        return ApplicationEnvironmentRole.class;
    }
}