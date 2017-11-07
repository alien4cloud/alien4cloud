package alien4cloud.model.orchestrators.locations;

import lombok.Getter;
import lombok.Setter;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.mapping.IndexType;
import org.hibernate.validator.constraints.NotBlank;

/**
 * Reference to a location modifier bean.
 */
@Getter
@Setter
public class LocationModifierReference {
    @NotBlank
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String pluginId;
    @NotBlank
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String beanName;
    @StringField(indexType = IndexType.no, includeInAll = false)
    private String phase;
}