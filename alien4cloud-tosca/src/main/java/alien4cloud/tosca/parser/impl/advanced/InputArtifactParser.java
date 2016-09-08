package alien4cloud.tosca.parser.impl.advanced;

import org.springframework.stereotype.Component;

@Component
public class InputArtifactParser extends DeploymentArtifactParser {

    public InputArtifactParser() {
        super(ArtifactReferenceMissingMode.NONE);
    }
}
