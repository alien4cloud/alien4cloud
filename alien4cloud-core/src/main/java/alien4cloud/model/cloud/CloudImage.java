package alien4cloud.model.cloud;

import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

@Getter
@Setter
@ESObject
@SuppressWarnings("PMD.UnusedPrivateField")
public class CloudImage {

    /**
     * Technical internal id
     */
    @Id
    @StringField(includeInAll = false, indexType = IndexType.not_analyzed)
    private String id;

    /**
     * Short descriptive name of the image
     */
    @StringField(includeInAll = true, indexType = IndexType.not_analyzed)
    private String name;

    /**
     * The icon representing this image
     */
    @StringField(includeInAll = false, indexType = IndexType.not_analyzed)
    private String iconId;

    /**
     * The architect x64 or x86
     */
    @StringField(includeInAll = true, indexType = IndexType.not_analyzed)
    @TermFilter
    private String osArch;

    /**
     * OS type
     */
    @StringField(includeInAll = true, indexType = IndexType.not_analyzed)
    @TermFilter
    private String osType;

    /**
     * OS Distribution
     */
    @StringField(includeInAll = true, indexType = IndexType.not_analyzed)
    @TermFilter
    private String osDistribution;

    /**
     * OS Version
     */
    @StringField(includeInAll = true, indexType = IndexType.not_analyzed)
    @TermFilter
    private String osVersion;

    /**
     * The requirement for the image (cpu, disk, mem)
     */
    private CloudImageRequirement requirement;
}
