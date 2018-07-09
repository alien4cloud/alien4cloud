package alien4cloud.model.application;

import java.util.LinkedHashMap;
import java.util.Map;

import org.alien4cloud.tosca.model.Csar;
import org.elasticsearch.annotation.BooleanField;
import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.MapKeyValue;
import org.elasticsearch.annotation.ObjectField;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.FetchContext;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import alien4cloud.security.IManagedSecuredResource;
import alien4cloud.utils.jackson.ConditionalAttributes;
import alien4cloud.utils.jackson.ConditionalOnAttribute;
import alien4cloud.utils.jackson.JSonMapEntryArrayDeSerializer;
import alien4cloud.utils.jackson.JSonMapEntryArraySerializer;
import alien4cloud.utils.version.Version;
import lombok.Getter;
import lombok.Setter;

import static alien4cloud.dao.model.FetchContext.SUMMARY;

@ESObject
@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class ApplicationVersion implements IManagedSecuredResource {
    // Prior to 1.4 this used to be both the id of the version and of the archive that back this version.
    private String id;
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String version;
    // FIXME ignore this in API
    @ObjectField
    @TermFilter(paths = { "majorVersion", "minorVersion", "incrementalVersion", "buildNumber", "qualifier" })
    private Version nestedVersion;
    private String description;
    @BooleanField(includeInAll = false, index = IndexType.not_analyzed)
    private boolean released;
    @TermFilter
    @StringField(includeInAll = false, indexType = IndexType.not_analyzed)
    private String applicationId;

    @MapKeyValue(indexType = IndexType.no)
    @ConditionalOnAttribute(ConditionalAttributes.ES)
    @JsonDeserialize(using = JSonMapEntryArrayDeSerializer.class)
    @JsonSerialize(using = JSonMapEntryArraySerializer.class)
    private LinkedHashMap<String, ApplicationTopologyVersion> topologyVersions;

    @StringField(includeInAll = false, indexType = IndexType.no)
    @JsonIgnore
    @Override
    public String getDelegateId() {
        return applicationId;
    }

    @JsonIgnore
    @Override
    public String getDelegateType() {
        return Application.class.getSimpleName().toLowerCase();
    }

    public void setDelegateId(String id) {
        setApplicationId(id);
    }

    @Id
    public String getId() {
        return Csar.createId(applicationId, version);
    }

    public void setId(String id) {
        // Do nothing as id is generated from applicationId and version
    }
}