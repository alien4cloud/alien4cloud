package alien4cloud.model.common;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.mapping.IndexType;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
@EqualsAndHashCode(of = "name")
public class Tag {
    @StringField(indexType = IndexType.not_analyzed)
    private String name;
    private String value;
}
