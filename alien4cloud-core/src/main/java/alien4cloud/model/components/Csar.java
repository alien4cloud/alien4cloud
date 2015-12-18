package alien4cloud.model.components;

import static alien4cloud.dao.model.FetchContext.SUMMARY;

import java.util.Set;

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

import alien4cloud.exception.IndexingServiceException;
import alien4cloud.model.deployment.IDeploymentSource;

@Getter
@Setter
@EqualsAndHashCode(of = { "name", "version" })
@ESObject
public class Csar implements IDeploymentSource {
	@FetchContext(contexts = { SUMMARY }, include = { true })
	private String name;

	@TermFilter
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

    /**
     * When the CSAR is created from a topology template (substitution), contains the topology id.
     */
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String substitutionTopologyId;

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
}