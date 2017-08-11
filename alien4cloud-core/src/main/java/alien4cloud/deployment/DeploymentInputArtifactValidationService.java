package alien4cloud.deployment;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.List;
import java.util.stream.Collectors;

import org.alien4cloud.alm.deployment.configuration.model.DeploymentInputs;
import org.alien4cloud.tosca.model.templates.Topology;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import alien4cloud.topology.task.ArtifactTaskCode;
import alien4cloud.topology.task.InputArtifactTask;

@Service
public class DeploymentInputArtifactValidationService {
    /**
     * Validate that all input artifacts are filled
     * 
     * @param topology The topology in process
     * @param deploymentInputs Input values as specified by a deployer user. Uploaded artifacts are stored in {@link DeploymentInputs#getInputArtifacts()}.
     * @return
     */
    public List<InputArtifactTask> validate(Topology topology, DeploymentInputs deploymentInputs) {
        return safe(topology.getInputArtifacts()).entrySet().stream()
                .filter(inputArtifactEntry -> StringUtils.isBlank(inputArtifactEntry.getValue().getArtifactRef())
                        && !safe(deploymentInputs.getInputArtifacts()).containsKey(inputArtifactEntry.getKey()))
                .map(deploymentArtifactEntry -> new InputArtifactTask(deploymentArtifactEntry.getKey(), ArtifactTaskCode.MISSING)).collect(Collectors.toList());
    }
}
