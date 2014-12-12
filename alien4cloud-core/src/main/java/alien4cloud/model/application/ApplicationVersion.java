package alien4cloud.model.application;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.BooleanField;
import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

import alien4cloud.security.IManagedSecuredResource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@ESObject
@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class ApplicationVersion implements IManagedSecuredResource {
    @Id
    private String id;
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String version;
    private String description;
    @TermFilter
    @StringField(includeInAll = false, indexType = IndexType.not_analyzed)
    private String applicationId;
    @TermFilter
    @StringField(includeInAll = false, indexType = IndexType.not_analyzed)
    private String topologyId;
    private Map<String, String> properties;
    @BooleanField(index = IndexType.not_analyzed)
    private boolean released;
    @BooleanField(index = IndexType.not_analyzed)
    private boolean latest;
    @BooleanField(index = IndexType.not_analyzed)
    private boolean isSnapshot;

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
}