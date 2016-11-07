package alien4cloud.tosca.parser.postprocess;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.impl.ErrorCode;

/**
 * Specific post processor that manages errors for node template deployment artifacts.
 */
@Component
public class TemplateDeploymentArtifactPostProcessor extends AbstractArtifactPostProcessor {
    @Override
    protected void postProcessArtifactRef(Node node, String artifactReference) {
        if (artifactReference == null) {
            Node referenceNode = ParsingContextExecution.getObjectToNodeMap().get(artifactReference);
            if (referenceNode == null) {
                referenceNode = node;
            }
            ParsingContextExecution.getParsingErrors().add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.UNRESOLVED_ARTIFACT, "Deployment artifact",
                    node.getStartMark(), "No artifact reference is defined, user will have to define / override in order to make ", node.getEndMark(), null));
        }
    }
}