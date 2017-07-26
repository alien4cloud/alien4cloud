package alien4cloud.deployment;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.List;
import java.util.stream.Collectors;

import org.alien4cloud.alm.deployment.configuration.model.DeploymentInputs;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import alien4cloud.topology.task.ArtifactTaskCode;
import alien4cloud.topology.task.InputArtifactTask;

@Service
public class DeploymentInputArtifactValidationService {
    public List<InputArtifactTask> validate(DeploymentInputs deploymentInputs) {
        return safe(deploymentInputs.getInputArtifacts()).entrySet().stream()
                .filter(deploymentArtifactEntry -> StringUtils.isBlank(deploymentArtifactEntry.getValue().getArtifactRef())
                        && !safe(deploymentInputs.getInputArtifacts()).containsKey(deploymentArtifactEntry.getKey()))
                .map(deploymentArtifactEntry -> new InputArtifactTask(deploymentArtifactEntry.getKey(), ArtifactTaskCode.MISSING)).collect(Collectors.toList());
    }
}
