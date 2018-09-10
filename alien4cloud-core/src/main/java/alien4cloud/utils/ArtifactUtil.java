package alien4cloud.utils;

import static alien4cloud.utils.AlienUtils.safe;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.AbstractArtifact;
import org.alien4cloud.tosca.model.templates.Topology;

public class ArtifactUtil {

    /**
     * Copy csars artifacts to a new location without tosca yaml and without meta data as .git
     * 
     * @param originalCSARPath original csar path
     * @param newCSARPath new csar path
     * @throws IOException if problem with underlying file system
     */
    public static void copyCsarArtifacts(Path originalCSARPath, Path newCSARPath) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(originalCSARPath)) {
            for (Path topologyResource : stream) {
                String fileName = topologyResource.getFileName().toString();
                if (!fileName.equals(".git") && !fileName.endsWith(".yml") && !fileName.endsWith(".yaml")) {
                    if (Files.isDirectory(topologyResource)) {
                        FileUtil.copy(topologyResource, newCSARPath.resolve(fileName));
                    } else {
                        Files.copy(topologyResource, newCSARPath.resolve(fileName));
                    }
                }
            }
        }
    }

    private static void setArtifactToCorrectArchiveReference(AbstractArtifact artifact, Topology topology, Csar csar) {
        if (topology.getArchiveName().equals(artifact.getArchiveName()) && topology.getArchiveVersion().equals(artifact.getArchiveVersion())) {
            // Must migrate to new reference
            artifact.setArchiveName(csar.getName());
            artifact.setArchiveVersion(csar.getVersion());
        }
    }

    public interface DoWithArtifact {
        void doWithArtifact(AbstractArtifact artifact);
    }

    public static void doWithTopologyArtifacts(Topology topology, DoWithArtifact doWithArtifact) {
        safe(topology.getInputArtifacts()).values().forEach(doWithArtifact::doWithArtifact);
        safe(topology.getNodeTemplates()).values().forEach(nodeTemplate -> {
            safe(nodeTemplate.getArtifacts()).values().forEach(doWithArtifact::doWithArtifact);
            safe(nodeTemplate.getRelationships()).values()
                    .forEach(relationshipTemplate -> safe(relationshipTemplate.getArtifacts()).values().forEach(doWithArtifact::doWithArtifact));
        });
    }

    /**
     * Change all artifact's archive reference in the given topology to the newly created CSAR
     * 
     * @param topology the topology which contains artifacts
     * @param csar the newly created csar
     */
    public static void changeTopologyArtifactReferences(Topology topology, Csar csar) {
        doWithTopologyArtifacts(topology, artifact -> setArtifactToCorrectArchiveReference(artifact, topology, csar));
    }
}
