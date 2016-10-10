package org.alien4cloud.tosca.model.types;

import static alien4cloud.dao.model.FetchContext.QUICK_SEARCH;
import static alien4cloud.dao.model.FetchContext.SUMMARY;
import static alien4cloud.dao.model.FetchContext.TAG_SUGGESTION;

import java.util.Date;
import java.util.List;

import alien4cloud.model.common.IUpdatedDate;
import org.elasticsearch.annotation.DateField;
import org.elasticsearch.annotation.ESAll;
import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.ObjectField;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.StringFieldMulti;
import org.elasticsearch.annotation.query.FetchContext;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.annotation.query.TermsFacet;
import org.elasticsearch.mapping.IndexType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import alien4cloud.exception.IndexingServiceException;
import alien4cloud.model.common.Tag;
import alien4cloud.utils.version.Version;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(of = { "elementId", "archiveName", "archiveVersion" })
@JsonInclude(Include.NON_NULL)
@ESObject
@ESAll(analyser = "simple")
public abstract class AbstractToscaType implements IUpdatedDate {
    @FetchContext(contexts = { TAG_SUGGESTION }, include = { false })
    @StringField(indexType = IndexType.not_analyzed)
    @TermFilter
    private String archiveName;

    @FetchContext(contexts = { TAG_SUGGESTION }, include = { false })
    @StringField(indexType = IndexType.not_analyzed)
    @TermFilter
    private String archiveVersion;

    @ObjectField
    @TermFilter(paths = { "majorVersion", "minorVersion", "incrementalVersion", "buildNumber", "qualifier" })
    @FetchContext(contexts = { TAG_SUGGESTION, QUICK_SEARCH, SUMMARY }, include = { false, false, false })
    private Version nestedVersion;

    @StringField(indexType = IndexType.not_analyzed)
    @FetchContext(contexts = { TAG_SUGGESTION }, include = { false })
    @TermsFacet
    @TermFilter
    private String workspace;

    @FetchContext(contexts = { TAG_SUGGESTION }, include = { false })
    @StringFieldMulti(main = @StringField(indexType = IndexType.analyzed), multiNames = "rawElementId", multi = @StringField(includeInAll = false, indexType = IndexType.not_analyzed))
    @TermFilter
    private String elementId;

    @FetchContext(contexts = { TAG_SUGGESTION }, include = { false })
    @DateField(includeInAll = false, index = IndexType.no)
    private Date creationDate;

    @FetchContext(contexts = { TAG_SUGGESTION }, include = { false })
    @DateField(includeInAll = false, index = IndexType.no)
    private Date lastUpdateDate;

    /* Normative element */
    @StringField(indexType = IndexType.no)
    @FetchContext(contexts = { TAG_SUGGESTION }, include = { false })
    private String description;

    /* DSL extension */
    private List<Tag> tags;

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

    public void setArchiveVersion(String version) {
        this.archiveVersion = version;
        this.nestedVersion = new Version(version);
    }
}
