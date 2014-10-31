package alien4cloud.tosca.parser.mapping;

import org.springframework.stereotype.Component;

import alien4cloud.tosca.container.model.template.DeploymentArtifact;

@Component
public class Wd03DeploymentArtifactDefinition extends Wd03AbstractArtifactDefinition<DeploymentArtifact> {
    public Wd03DeploymentArtifactDefinition() {
        super(DeploymentArtifact.class);
    }
}