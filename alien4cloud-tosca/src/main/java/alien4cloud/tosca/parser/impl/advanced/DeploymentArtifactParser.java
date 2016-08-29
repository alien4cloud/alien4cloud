package alien4cloud.tosca.parser.impl.advanced;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.model.components.DeploymentArtifact;
import alien4cloud.tosca.parser.ParsingContextExecution;

@Component
public class DeploymentArtifactParser extends ArtifactParser<DeploymentArtifact> {

    @Override
    public DeploymentArtifact parse(Node node, ParsingContextExecution context) {
        return doParse(new DeploymentArtifact(), node, context);
    }
}
