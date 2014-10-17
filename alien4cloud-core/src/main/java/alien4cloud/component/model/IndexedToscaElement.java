package alien4cloud.component.model;

import static alien4cloud.dao.model.FetchContext.TAG_SUGGESTION;

import java.util.Date;
import java.util.List;
import java.util.Set;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.BooleanField;
import org.elasticsearch.annotation.DateField;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.FetchContext;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

import alien4cloud.exception.IndexingServiceException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@Getter
@Setter
@EqualsAndHashCode(of = { "elementId", "archiveName", "archiveVersion" })
@SuppressWarnings("PMD.UnusedPrivateField")
@JsonInclude(Include.NON_NULL)
public abstract class IndexedToscaElement {

    @BooleanField
    @TermFilter
    private boolean isHighestVersion;

    private Set<String> olderVersions;

    @FetchContext(contexts = { TAG_SUGGESTION }, include = { false })
    @StringField(indexType = IndexType.analyzed)
    @TermFilter
    private String elementId;

    @FetchContext(contexts = { TAG_SUGGESTION }, include = { false })
    @StringField(indexType = IndexType.not_analyzed)
    @TermFilter
    private String archiveName;

    @FetchContext(contexts = { TAG_SUGGESTION }, include = { false })
    @StringField(indexType = IndexType.not_analyzed)
    private String archiveVersion;

    @FetchContext(contexts = { TAG_SUGGESTION }, include = { false })
    private String description;

    private List<Tag> tags;

    @FetchContext(contexts = { TAG_SUGGESTION }, include = { false })
    @DateField(includeInAll = false, index = IndexType.no)
    private Date creationDate;

    @FetchContext(contexts = { TAG_SUGGESTION }, include = { false })
    @DateField(includeInAll = false, index = IndexType.no)
    private Date lastUpdateDate;

    @Id
    public String getId() {
        if (elementId == null) {
            throw new IndexingServiceException("Element id is mandatory");
        }
        if (archiveVersion == null) {
            throw new IndexingServiceException("Archive version is mandatory");
        }
        return elementId + ":" + archiveVersion;
    }

    public void setId(String id) {
        // Not authorized to set id as it's auto-generated
    }
}
