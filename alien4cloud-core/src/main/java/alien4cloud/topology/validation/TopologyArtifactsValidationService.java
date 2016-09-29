package alien4cloud.topology.validation;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;
import org.alien4cloud.tosca.model.templates.AbstractTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import alien4cloud.topology.task.ArtifactTask;
import alien4cloud.topology.task.ArtifactTaskCode;

/**
 * This validate all artifacts references are available for all nodes in the topology.
 */
@Service
public class TopologyArtifactsValidationService {

    private <T extends AbstractTemplate, U extends AbstractInheritableToscaType> Stream<ArtifactTask> validateTemplate(String name, T template) {
        return safe(template.getArtifacts()).entrySet().stream().filter(artifactEntry -> StringUtils.isBlank(artifactEntry.getValue().getArtifactRef()))
                .map(artifactEntry -> new ArtifactTask(name, artifactEntry.getKey(), ArtifactTaskCode.MISSING));
    }

    public List<ArtifactTask> validate(Topology topology) {
        // First validate all artifact for all node template then validate artifact for all relationships
        return Stream.concat(
                safe(topology.getNodeTemplates()).values().stream().flatMap(nodeTemplate -> validateTemplate(nodeTemplate.getName(), nodeTemplate)),
                safe(topology.getNodeTemplates()).values().stream().flatMap(nodeTemplate -> safe(nodeTemplate.getRelationships()).entrySet().stream())
                        .flatMap(relationshipTemplateEntry -> validateTemplate(relationshipTemplateEntry.getKey(), relationshipTemplateEntry.getValue())))
                .collect(Collectors.toList());
    }
}
