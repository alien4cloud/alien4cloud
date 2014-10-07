package alien4cloud.model.application;

import lombok.Getter;
import lombok.Setter;

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
@SuppressWarnings("PMD.UnusedPrivateField")
@JsonInclude(Include.NON_NULL)
public class ApplicationEnvironment implements IManagedSecuredResource {
    @Id
    private String id;
    private String name;
    private String description;
    @TermFilter
    @StringField(includeInAll = false, indexType = IndexType.not_analyzed)
    private String applicationId;
    @TermFilter
    @StringField(includeInAll = false, indexType = IndexType.not_analyzed)
    private String cloudId;
    @StringField(includeInAll = true, indexType = IndexType.not_analyzed)
    private EnvironmentType environmentType;

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