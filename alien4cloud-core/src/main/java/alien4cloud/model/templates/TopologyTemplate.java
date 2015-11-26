package alien4cloud.model.templates;

import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

@Getter
@ESObject
public class TopologyTemplate {
    @Id
    @Setter
    private String id;
    @StringField(includeInAll = true, indexType = IndexType.not_analyzed)
    @TermFilter
    @Setter
    private String name;
    @Setter
    private String description;
    private String topologyId;
}
