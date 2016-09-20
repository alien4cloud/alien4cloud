package org.alien4cloud.tosca.model;

import static alien4cloud.dao.model.FetchContext.SUMMARY;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.NestedObject;
import org.elasticsearch.annotation.ObjectField;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.FetchContext;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.annotation.query.TermsFacet;
import org.elasticsearch.mapping.IndexType;

import alien4cloud.exception.IndexingServiceException;
import alien4cloud.model.common.Tag;
import alien4cloud.security.IManagedSecuredResource;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.utils.version.Version;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(of = { "name", "version" })
@ESObject
public class Csar implements IManagedSecuredResource {
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    @FetchContext(contexts = { SUMMARY }, include = { true })
    private String name;

    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    @FetchContext(contexts = { SUMMARY }, include = { true })
    private String version;

    @ObjectField
    @TermFilter(paths = { "majorVersion", "minorVersion", "incrementalVersion", "buildNumber", "qualifier" })
    private Version nestedVersion;

    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    @TermsFacet
    private String workspace;

    /** This is the hashcode of all files in the archive. */
    @StringField(indexType = IndexType.not_analyzed)
    @FetchContext(contexts = { SUMMARY }, include = { true })
    private String hash;

    /** This is the hashcode of the definition file only (yaml). */
    @StringField(indexType = IndexType.not_analyzed)
    @FetchContext(contexts = { SUMMARY }, include = { true })
    private String definitionHash;

    /** Eventually the id of the application. */
    private String delegateId;
    /** Type of delegate if the archive is an application archive. */
    private String delegateType;

    /** Path of the yaml file in the archive (relative to the root). */
    private String yamlFilePath;

    private String toscaDefinitionsVersion;

    private String toscaDefaultNamespace;

    @TermsFacet
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String templateAuthor;

    private String description;

    @TermFilter(paths = { "name", "version" })
    @NestedObject(nestedClass = CSARDependency.class)
    private Set<CSARDependency> dependencies;

    private String license;

    /** Archive metadata. */
    private List<Tag> tags;

    /** Alien 4 Cloud meta-data to know how the archive has been imported. */
    private String importSource;
    /** Date on which the archive has been imported or updated in alien4cloud. */
    private Date importDate;

    /** Default constructor */
    public Csar() {
    }

    /** Argument constructor */
    public Csar(String name, String version) {
        this.name = name;
        this.version = version;
        this.nestedVersion = new Version(version);
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

    /**
     * Merge the given dependencies with the current ones.
     * 
     * @param dependencies
     */
    public void setDependencies(Set<CSARDependency> dependencies) {
        if (this.dependencies == null) {
            this.dependencies = dependencies;
        } else {
            this.dependencies.addAll(dependencies);
        }
    }

    /**
     * Merge the given dependencies with the current ones.
     *
     * @param dependencies
     */
    public void setDependencies(Set<CSARDependency> dependencies, boolean override) {
        if (override) {
            this.dependencies = dependencies;
        } else {
            setDependencies(dependencies);
        }
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
            this.nestedVersion = new Version(version);
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