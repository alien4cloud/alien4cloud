package alien4cloud.tosca.parser.impl.advanced;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;

import alien4cloud.tosca.model.ImplementationArtifact;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.utils.MapUtil;

import com.google.common.io.Files;

@Component
public class ImplementationArtifactParser implements INodeParser<ImplementationArtifact> {
    // TODO manage that from repository....
    private static final Map<String, String> EXTENTIONS_TO_ARTIFACT_TYPES = MapUtil.newHashMap(new String[] { "sh", "war", "zip" }, new String[] {
            "tosca.artifacts.ShellScript", "tosca.artifacts.WarFile", "tosca.artifacts.ZipFile" });

    @Override
    public ImplementationArtifact parse(Node node, ParsingContextExecution context) {
        if (node instanceof ScalarNode) {
            String artifactReference = ((ScalarNode) node).getValue();

            Path artifactPath = Paths.get(artifactReference);
            String extension = Files.getFileExtension(artifactPath.getFileName().toString());
            String type = EXTENTIONS_TO_ARTIFACT_TYPES.get(extension);

            ImplementationArtifact artifact = new ImplementationArtifact();
            artifact.setArtifactRef(artifactReference);
            artifact.setArtifactType(type);
            return artifact;
        } else {
            ParserUtils.addTypeError(node, context.getParsingErrors(), "Artifact definition");
        }
        return null;
    }

    @Override
    public boolean isDeferred() {
        return true;
    }
}