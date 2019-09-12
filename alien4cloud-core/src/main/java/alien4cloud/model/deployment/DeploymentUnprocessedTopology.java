package alien4cloud.model.deployment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.alien4cloud.tosca.model.templates.Topology;
import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;

/**
 * Stores element of the original topology of a deployment
 */
@Getter
@Setter
@ESObject
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeploymentUnprocessedTopology {
    @Id
    private String id;

    private Topology unprocessedTopology;
}
