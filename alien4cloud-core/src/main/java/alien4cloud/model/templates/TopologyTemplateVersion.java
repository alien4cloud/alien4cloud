package alien4cloud.model.templates;

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
public class TopologyTemplateVersion extends AbstractTopologyVersion {
    @TermFilter
    @StringField(includeInAll = false, indexType = IndexType.not_analyzed)
    private String topologyTemplateId;

    @JsonIgnore
    @Override
    public String getDelegateId() {
        return topologyTemplateId;
    }

    @JsonIgnore
    @Override
    public String getDelegateType() {
        return TopologyTemplate.class.getSimpleName().toLowerCase();
    }

    @Override
    public void setDelegateId(String id) {
        setTopologyTemplateId(id);
    }

}