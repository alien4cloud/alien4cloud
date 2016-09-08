package alien4cloud.topology.task;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ArtifactTask extends AbstractTask {

    private String nodeTemplateName;

    private String artifactName;

    private ArtifactTaskCode artifactTaskCode;

    public ArtifactTask(String nodeTemplateName, String artifactName, ArtifactTaskCode artifactTaskCode) {
        setCode(TaskCode.ARTIFACT_INVALID);
        this.nodeTemplateName = nodeTemplateName;
        this.artifactName = artifactName;
        this.artifactTaskCode = artifactTaskCode;
    }
}
