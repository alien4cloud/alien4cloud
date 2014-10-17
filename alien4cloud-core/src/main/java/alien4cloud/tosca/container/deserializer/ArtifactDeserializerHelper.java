package alien4cloud.tosca.container.deserializer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import alien4cloud.tosca.container.exception.CSARTechnicalException;
import alien4cloud.tosca.container.model.template.DeploymentArtifact;
import alien4cloud.tosca.container.model.type.ImplementationArtifact;
import alien4cloud.utils.MapUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;

/**
 * Utility class to deserialize {@link ImplementationArtifact} or {@link DeploymentArtifact}.
 */
public final class ArtifactDeserializerHelper {
    private static final Map<String, String> EXTENTIONS_TO_ARTIFACT_TYPES = MapUtil.newHashMap(new String[] { "sh", "war", "zip" }, new String[] {
            "tosca.artifacts.ShellScript", "tosca.artifacts.WarFile", "tosca.artifacts.ZipFile" });

    private ArtifactDeserializerHelper() {
    }

    /**
     * Deserialize an artifact from a given tree node.
     * 
     * @param mapper The object mapper that is used for deserialization.
     * @param node The tree node that contains the object to deserialize.
     * @param artifactType The class of the artifact to deserialize, must be either {@link ImplementationArtifact} or {@link DeploymentArtifact}.
     * @return A deserialized instance of the artifact.
     * @throws JsonProcessingException In case the processing fails.
     */
    @SuppressWarnings("unchecked")
    public static <T> T deserializeArtifact(ObjectMapper mapper, TreeNode node, Class<T> artifactType) throws JsonProcessingException {
        String artifact = mapper.treeToValue(node, String.class);
        Path artifactPath = Paths.get(artifact);
        String extension = Files.getFileExtension(artifactPath.getFileName().toString());
        String type = EXTENTIONS_TO_ARTIFACT_TYPES.get(extension);

        if (artifactType.equals(ImplementationArtifact.class)) {
            return (T) getImplementationArtifact(type, artifact);
        } else if (artifactType.equals(DeploymentArtifact.class)) {
            return (T) getDeploymentArtifact(type, artifact);
        }
        throw new CSARTechnicalException("ArtifactDeserializerHelper can only process ImplementationArtifact or DeploymentArtifact, artifactType was "
                + artifactType);
    }

    private static DeploymentArtifact getDeploymentArtifact(String type, String ref) {
        DeploymentArtifact artifact = new DeploymentArtifact();
        artifact.setArtifactType(type);
        artifact.setArtifactRef(ref);
        return artifact;
    }

    private static ImplementationArtifact getImplementationArtifact(String type, String ref) {
        ImplementationArtifact artifact = new ImplementationArtifact();
        artifact.setArtifactType(type);
        artifact.setArtifactRef(ref);
        return artifact;
    }
}