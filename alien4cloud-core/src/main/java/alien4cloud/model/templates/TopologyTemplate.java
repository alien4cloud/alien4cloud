package alien4cloud.model.templates;

import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

/**
 *
 * @author mourouvi
 *
 */
@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
@ESObject
public class TopologyTemplate {

    @Id
    private String id;
    @StringField(includeInAll = true, indexType = IndexType.not_analyzed)
    @TermFilter
    private String name;
    private String description;
    private String topologyId;

}
