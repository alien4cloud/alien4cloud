package alien4cloud.tosca.parser.impl.advanced;

import org.yaml.snakeyaml.nodes.Node;

import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import alien4cloud.tosca.parser.ParsingContextExecution;

public abstract class DeploymentArtifactParser extends ArtifactParser<DeploymentArtifact> {

    public DeploymentArtifactParser(ArtifactReferenceMissingMode artifactReferenceMissingMode) {
        super(artifactReferenceMissingMode);
    }

    @Override
    public DeploymentArtifact parse(Node node, ParsingContextExecution context) {
        return doParse(new DeploymentArtifact(), node, context);
    }
}
