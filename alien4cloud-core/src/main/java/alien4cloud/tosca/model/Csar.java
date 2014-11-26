package alien4cloud.tosca.model;

import static alien4cloud.dao.model.FetchContext.DEPLOYMENT;

import java.util.Set;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.FetchContext;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

import alien4cloud.exception.IndexingServiceException;
import alien4cloud.model.deployment.IDeploymentSource;
import alien4cloud.tosca.container.model.CSARDependency;

@Getter
@Setter
@EqualsAndHashCode(of = { "name", "version" })
@SuppressWarnings("PMD.UnusedPrivateField")
@ESObject
public class Csar implements IDeploymentSource {
    @FetchContext(contexts = { DEPLOYMENT }, include = { true })
    private String name;

    @TermFilter
    private String version;

    private String toscaDefinitionsVersion;

    private String toscaDefaultNamespace;

    private String templateAuthor;

    private String description;

    private Set<CSARDependency> dependencies;

    private String topologyId;
    private String cloudId;

    private String license;

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
    @FetchContext(contexts = { DEPLOYMENT }, include = { true })
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
}