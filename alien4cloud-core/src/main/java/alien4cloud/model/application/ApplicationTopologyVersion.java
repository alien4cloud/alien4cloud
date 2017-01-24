package alien4cloud.model.application;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

/**
 * Describe version for a specific topology of an application version.
 *
 * Applications 1 - n ApplicationVersion 1 - n ApplicationTopologyVersion
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApplicationTopologyVersion {
    // This is the id of the cloud service archive associated with the version (archiveName + version).
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String archiveId;
    @StringField(indexType = IndexType.no, includeInAll = false)
    private String description;
    @StringField(indexType = IndexType.no, includeInAll = false)
    private String qualifier;
}