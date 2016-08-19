package alien4cloud.model.repository;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.ObjectField;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.annotation.query.TermsFacet;
import org.elasticsearch.mapping.IndexType;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ESObject
public class Repository {
    @Id
    private String id;
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String name;
    /** Id of the plugin. */
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String pluginId;
    @TermFilter
    @TermsFacet
    @StringField(indexType = IndexType.not_analyzed)
    private String repositoryType;
    /** Configuration object. */
    @ObjectField(enabled = false)
    private Object configuration;
}
