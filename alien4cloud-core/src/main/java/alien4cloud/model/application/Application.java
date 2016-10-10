package alien4cloud.model.application;

import static alien4cloud.dao.model.FetchContext.SUMMARY;

import java.util.Date;
import java.util.List;
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

import alien4cloud.model.common.IMetaProperties;
import alien4cloud.model.common.ITaggableResource;
import alien4cloud.model.common.IUpdatedDate;
import alien4cloud.model.common.Tag;
import alien4cloud.model.deployment.IDeploymentSource;
import alien4cloud.security.ISecuredResource;
import alien4cloud.security.model.ApplicationRole;
import alien4cloud.utils.jackson.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Model for an application in alien.
 *
 * @author luc boutier
 */
@ESObject
@Getter
@Setter
@JsonInclude(Include.NON_NULL)
@ESAll(analyser = "simple")
public class Application implements ISecuredResource, IDeploymentSource, ITaggableResource, IMetaProperties, IUpdatedDate {

    @Id
    @FetchContext(contexts = { SUMMARY }, include = { true })
    private String id;

    @FetchContext(contexts = { SUMMARY }, include = { true })
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String name;

    @StringField(indexType = IndexType.analyzed)
    private String description;

    @DateField(includeInAll = false, index = IndexType.no)
    private Date creationDate;

    @DateField(includeInAll = false, index = IndexType.no)
    private Date lastUpdateDate;

    @StringField(includeInAll = false, indexType = IndexType.no)
    private String imageId;

    @StringField(includeInAll = true, indexType = IndexType.analyzed)
    private List<Tag> tags;

    @StringField(includeInAll = true, indexType = IndexType.analyzed)
    private Map<String, String> metaProperties;

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
    public Class<ApplicationRole> roleEnum() {
        return ApplicationRole.class;
    }
}
