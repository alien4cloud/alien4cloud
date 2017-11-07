package alien4cloud.model.orchestrators.locations;

import static alien4cloud.dao.model.FetchContext.SUMMARY;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alien4cloud.tosca.model.CSARDependency;
import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.NestedObject;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.FetchContext;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;
import org.hibernate.validator.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Sets;

import alien4cloud.model.common.IDatableResource;
import alien4cloud.model.common.IMetaProperties;
import alien4cloud.security.AbstractSecurityEnabledResource;
import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@ESObject
@ApiModel(value = "Location", description = "A location represents a cloud, a region of a cloud, a set of machines and resources."
        + "basically any location on which alien will be allowed to perform deployment. Locations are managed by orchestrators.")
public class Location extends AbstractSecurityEnabledResource implements IMetaProperties, IDatableResource {
    @Id
    @FetchContext(contexts = SUMMARY, include = true)
    private String id;
    @NotBlank
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    @FetchContext(contexts = SUMMARY, include = true)
    private String name;
    @NotBlank
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    @FetchContext(contexts = SUMMARY, include = true)
    private String orchestratorId;
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String infrastructureType;
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String environmentType;

    /**
     * A Location defines and uses some types, it thus basically have a set of CSARDependency
     */
    @NestedObject(nestedClass = CSARDependency.class)
    private Set<CSARDependency> dependencies = Sets.newHashSet();

    @StringField(indexType = IndexType.analyzed, includeInAll = true)
    private Map<String, String> metaProperties;

    private List<LocationModifierReference> modifiers;

    private Date creationDate;

    private Date lastUpdateDate = new Date();
}