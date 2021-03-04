package alien4cloud.model.orchestrators.locations;

import java.util.List;
import java.util.Map;

import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.templates.AbstractTemplate;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;
import org.hibernate.validator.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonInclude;

import alien4cloud.security.AbstractSecurityEnabledResource;
import lombok.Getter;
import lombok.Setter;

/**
 * A Location template is a location resource that has been defined and can be matched against matchable elements in a topology.
 *
 * A location template is defined with a {@link AbstractTemplate} that specifies values
 */
@Getter
@Setter
public abstract class AbstractLocationResourceTemplate<T extends AbstractTemplate> extends AbstractSecurityEnabledResource {

    @Id
    private String id;
    @NotBlank
    private String name;
    @NotBlank
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String locationId;
    /** Flag to know if the administrator allow this resource to be matched. */
    private boolean enabled;
    /**
     * Flag to know if the resource template defines a service or a resource. In case of a resource it's type must be one of the orchestrator resource types.
     */
    private boolean isService;

    // TODO service may be related to an application.
    /** Array of types the template derives from - type of the template and all parents types. */
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private List<String> types;

    /** For this template, the possible {@link PropertyDefinition}s that can be used in portability edition. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, PropertyDefinition> portabilityDefinitions;

    /** Get the wrapped Template that describe the location resource (it's type must be a type derived from one of the orchestrator location exposed types). */
    abstract public T getTemplate();

    /** Set the wrapped Template that describe the location resource (it's type must be a type derived from one of the orchestrator location exposed types). */
    abstract public void setTemplate(T template);
}
