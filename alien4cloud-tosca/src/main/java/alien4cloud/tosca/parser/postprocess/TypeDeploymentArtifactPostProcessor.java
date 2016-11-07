package alien4cloud.tosca.parser.postprocess;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

/**
 * Type deployment artifact post processor.
 */
@Component
public class TypeDeploymentArtifactPostProcessor extends AbstractArtifactPostProcessor {
    @Override
    protected void postProcessArtifactRef(Node node, String artifactReference) {
        // Nothing has to be checked on types as reference can be specified in templates.
    }
}
