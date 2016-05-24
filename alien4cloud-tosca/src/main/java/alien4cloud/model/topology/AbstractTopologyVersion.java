package alien4cloud.model.topology;

import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.BooleanField;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

import alien4cloud.security.IManagedSecuredResource;

@Getter
@Setter
public abstract class AbstractTopologyVersion implements IManagedSecuredResource {
    @Id
    private String id;
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String version;
    private String description;
    @TermFilter
    @StringField(includeInAll = false, indexType = IndexType.not_analyzed)
    private String topologyId;
    @BooleanField(index = IndexType.not_analyzed)
    private boolean released;
    @BooleanField(index = IndexType.not_analyzed)
    private boolean latest;
    @BooleanField(index = IndexType.not_analyzed)
    private boolean isSnapshot;

    public abstract void setDelegateId(String id);

}