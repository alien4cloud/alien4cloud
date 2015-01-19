package alien4cloud.model.topology;

import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.NestedObject;
import org.elasticsearch.annotation.query.TermFilter;

import alien4cloud.model.components.CSARDependency;
import alien4cloud.security.IManagedSecuredResource;
import alien4cloud.utils.JSonMapEntryArrayDeSerializer;
import alien4cloud.utils.JSonMapEntryArraySerializer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Sets;

@Getter
@Setter
@ESObject
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("PMD.UnusedPrivateField")
public class Topology implements IManagedSecuredResource {
    @Id
    private String id;

    /** Id of the application or topology template. */
    private String delegateId;
    /** Type of the delegate (application or topology template) */
    private String delegateType;

    /** The list of dependencies of this topology. */
    @TermFilter(paths = { "name", "version" })
    @NestedObject(nestedClass = CSARDependency.class)
    private Set<CSARDependency> dependencies = Sets.newHashSet();

    @TermFilter(paths = "value.type")
    @JsonDeserialize(using = JSonMapEntryArrayDeSerializer.class)
    @JsonSerialize(using = JSonMapEntryArraySerializer.class)
    private Map<String, NodeTemplate> nodeTemplates;
    private Map<String, ScalingPolicy> scalingPolicies;
    private Map<String, Set<String>> inputProperties;
    private Map<String, Set<String>> outputProperties;
    private Map<String, Set<String>> outputAttributes;
    private Map<String, Set<String>> inputArtifacts;
}