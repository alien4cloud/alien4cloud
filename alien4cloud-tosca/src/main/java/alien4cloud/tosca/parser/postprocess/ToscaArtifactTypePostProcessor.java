package alien4cloud.tosca.parser.postprocess;

import static alien4cloud.utils.AlienUtils.safe;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.model.components.IndexedArtifactToscaElement;
import alien4cloud.model.components.Interface;

import java.util.Objects;

/**
 * Performs validation of a type with artifacts.
 */
@Component
public class ToscaArtifactTypePostProcessor implements IPostProcessor<IndexedArtifactToscaElement> {
    @Resource
    private ArtifactPostProcessor artifactPostProcessor;

    @Override
    public void process(IndexedArtifactToscaElement instance) {
        safe(instance.getArtifacts()).values().stream().forEach(artifactPostProcessor);

        // TODO Manage interfaces inputs to copy them to all operations.
        for (Interface anInterface : safe(instance.getInterfaces()).values()) {
            safe(anInterface.getOperations()).values().stream().map(operation -> operation.getImplementationArtifact()).filter(Objects::nonNull)
                    .forEach(artifactPostProcessor);
        }
    }
}