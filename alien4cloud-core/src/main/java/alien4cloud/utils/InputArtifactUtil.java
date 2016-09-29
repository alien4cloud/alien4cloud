package alien4cloud.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import org.alien4cloud.tosca.model.definitions.AbstractArtifact;
import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;

public class InputArtifactUtil {

    public static final String INPUT_ARTIFACT_FN_NAME = "get_input_artifact";

    private static final Pattern P = Pattern.compile("\\{\\s*" + INPUT_ARTIFACT_FN_NAME + ":\\s*(\\w+)\\s*\\}");

    private InputArtifactUtil() {
        super();
    }

    public static void updateInputArtifactIdIfNeeded(DeploymentArtifact dArtifact, String oldInputArtifactId, String newInputArtifactId) {
        Matcher m = P.matcher(dArtifact.getArtifactRef());
        if (m.matches() && m.group(1).equals(oldInputArtifactId)) {
            dArtifact.setArtifactRef("{ " + INPUT_ARTIFACT_FN_NAME + ": " + newInputArtifactId + " }");
        }
    }

    public static void setInputArtifact(DeploymentArtifact dArtifact, String artifactId) {
        dArtifact.setArtifactRef("{ " + INPUT_ARTIFACT_FN_NAME + ": " + artifactId + " }");
        dArtifact.setArtifactName(null);
    }

    public static void unsetInputArtifact(DeploymentArtifact dArtifact) {
        dArtifact.setArtifactRef(null);
        dArtifact.setArtifactName(null);
    }

    /**
     * @return the id of the related input artifact or null if this {@link AbstractArtifact} (can be deployment or implementation artifact) is not related to an
     *         input artifact.
     */
    public static String getInputArtifactId(AbstractArtifact dArtifact) {
        if (StringUtils.isBlank(dArtifact.getArtifactRef())) {
            return null;
        }
        Matcher m = P.matcher(dArtifact.getArtifactRef());
        if (m.matches()) {
            return m.group(1);
        }
        return null;
    }

}
