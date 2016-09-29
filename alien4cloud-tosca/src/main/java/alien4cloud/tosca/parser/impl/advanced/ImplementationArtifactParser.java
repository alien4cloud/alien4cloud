package alien4cloud.tosca.parser.impl.advanced;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import org.alien4cloud.tosca.model.definitions.ImplementationArtifact;
import alien4cloud.tosca.parser.ParsingContextExecution;

@Component
public class ImplementationArtifactParser extends ArtifactParser<ImplementationArtifact> {
    @Override
    public ImplementationArtifact parse(Node node, ParsingContextExecution context) {
        return doParse(new ImplementationArtifact(), node, context);
    }
}
