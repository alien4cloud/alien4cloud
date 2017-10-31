package alien4cloud.model.orchestrators.locations;

import org.elasticsearch.annotation.StringField;
import org.elasticsearch.mapping.IndexType;

import lombok.Getter;
import lombok.Setter;

/**
 * Reference to a location modifier bean.
 */
@Getter
@Setter
public class LocationModifierReference {
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String pluginId;
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String beanName;
    @StringField(indexType = IndexType.no, includeInAll = false)
    private String phase;
}