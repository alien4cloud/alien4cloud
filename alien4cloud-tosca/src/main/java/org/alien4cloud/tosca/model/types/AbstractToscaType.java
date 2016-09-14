package org.alien4cloud.tosca.model.types;

import static alien4cloud.dao.model.FetchContext.TAG_SUGGESTION;

import java.util.Date;
import java.util.List;
import java.util.Set;

import alien4cloud.utils.version.Version;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.*;
import org.elasticsearch.annotation.query.FetchContext;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.annotation.query.TermsFacet;
import org.elasticsearch.mapping.IndexType;

import alien4cloud.exception.IndexingServiceException;
import alien4cloud.model.common.Tag;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@Getter
@Setter
@EqualsAndHashCode(of = { "elementId", "archiveName", "archiveVersion" })
@JsonInclude(Include.NON_NULL)
@ESAll(analyser = "simple")
public abstract class AbstractToscaType {
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
    private Version nestedVersion;

    @FetchContext(contexts = { TAG_SUGGESTION }, include = { false })
    @StringField(indexType = IndexType.not_analyzed)
    @TermFilter
    private String archiveHash;

    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private Set<String> workspaces;

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

        if (archiveHash == null) {
            return elementId + ":" + archiveVersion; // hash is optional in id
        }
        return elementId + ":" + archiveVersion + ":" + archiveHash;
    }

    public void setId(String id) {
        // Not authorized to set id as it's auto-generated
    }

    public void setArchiveVersion(String version) {
        this.archiveVersion = version;
        this.nestedVersion = new Version(version);
    }
}
