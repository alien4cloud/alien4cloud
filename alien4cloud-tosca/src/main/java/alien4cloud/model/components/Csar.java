package alien4cloud.model.components;

import static alien4cloud.dao.model.FetchContext.SUMMARY;

import alien4cloud.exception.IndexingServiceException;
import alien4cloud.model.common.Tag;
import java.util.Date;
import java.util.List;
import java.util.Set;

import alien4cloud.tosca.parser.ParsingContextExecution;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.NestedObject;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.FetchContext;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

@Getter
@Setter
@EqualsAndHashCode(of = { "name", "version" })
@ESObject
public class Csar {
    @FetchContext(contexts = { SUMMARY }, include = { true })
    private String name;

    @TermFilter
    @FetchContext(contexts = { SUMMARY }, include = { true })
    private String version;

    private String toscaDefinitionsVersion;

    private String toscaDefaultNamespace;

    private String templateAuthor;

    private String description;

    @TermFilter(paths = { "name", "version" })
    @NestedObject(nestedClass = CSARDependency.class)
    private Set<CSARDependency> dependencies;

    private String topologyId;

    private String cloudId;

    private String license;

    /** Archive metadata. */
    private List<Tag> tags;

    private String importSource;
    private Date importDate;

    /**
     * When the CSAR is created from a topology template (substitution), contains the topology id.
     */
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String substitutionTopologyId;

    /**
     * Hash of the main yaml file included in the csar
     */
    @FetchContext(contexts = { SUMMARY }, include = { true })
    private String hash;

    /** Default constructor */
    public Csar() {
    }

    /** Argument constructor */
    public Csar(String name, String version) {
        this.name = name;
        this.version = version;
    }

    @Id
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    @FetchContext(contexts = { SUMMARY }, include = { true })
    public String getId() {
        if (name == null) {
            throw new IndexingServiceException("Csar name is mandatory");
        }
        if (version == null) {
            throw new IndexingServiceException("Csar version is mandatory");
        }
        return name + ":" + version;
    }

    public void setId(String id) {
        // Not authorized to set id as it's auto-generated from name and version
    }

    public void setDependencies(Set<CSARDependency> dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * In the context of parsing you can not override name when already provided (in case of tosca meta).
     * 
     * @param name The new name of the archive.
     */
    public void setName(String name) {
        if (this.name == null || !ParsingContextExecution.exist()) {
            this.name = name;
        }
    }

    /**
     * In the context of parsing you can not override version when already provided (in case of tosca meta).
     *
     * @param version The new version of the archive.
     */
    public void setVersion(String version) {
        if (this.version == null || !ParsingContextExecution.exist()) {
            this.version = version;
        }
    }

    /**
     * In the context of parsing you can not override template author when already provided (in case of tosca meta).
     *
     * @param templateAuthor The new template author of the archive.
     */
    public void setTemplateAuthor(String templateAuthor) {
        if (this.templateAuthor == null || !ParsingContextExecution.exist()) {
            this.templateAuthor = templateAuthor;
        }
    }
}