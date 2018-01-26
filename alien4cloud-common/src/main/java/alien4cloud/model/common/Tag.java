package alien4cloud.model.common;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "name")
public class Tag {
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String name;
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String value;
}
