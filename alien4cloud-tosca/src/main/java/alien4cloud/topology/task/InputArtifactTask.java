package alien4cloud.topology.task;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InputArtifactTask extends AbstractTask {

    private String inputArtifactName;

    private ArtifactTaskCode artifactTaskCode;

    public InputArtifactTask(String inputArtifactName, ArtifactTaskCode artifactTaskCode) {
        this.inputArtifactName = inputArtifactName;
        this.artifactTaskCode = artifactTaskCode;
        this.setCode(TaskCode.INPUT_ARTIFACT_INVALID);
    }
}
