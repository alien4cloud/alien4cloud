package alien4cloud.tosca.parser.impl.advanced;

import org.springframework.stereotype.Component;

@Deprecated
@Component
public class TypeDeploymentArtifactParser extends DeploymentArtifactParser {

    public TypeDeploymentArtifactParser() {
        super(ArtifactReferenceMissingMode.NONE);
    }
}
