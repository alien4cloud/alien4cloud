package alien4cloud.model.application;

import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

import org.alien4cloud.tosca.model.templates.AbstractTopologyVersion;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@ESObject
@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class ApplicationVersion extends AbstractTopologyVersion {
    @TermFilter
    @StringField(includeInAll = false, indexType = IndexType.not_analyzed)
    private String applicationId;

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

    @Override
    public void setDelegateId(String id) {
        setApplicationId(id);
    }

}