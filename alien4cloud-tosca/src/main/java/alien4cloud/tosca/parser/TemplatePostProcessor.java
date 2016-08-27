package alien4cloud.tosca.parser;

import java.util.Map;

import org.springframework.stereotype.Component;

import alien4cloud.model.components.*;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.tosca.model.ArchiveRoot;

@Component
public class TemplatePostProcessor {

    /**
     * Post process the archive: For every definition of the model it fills the id fields in the TOSCA elements from the key of the elements map.
     *
     * @param parsedArchive The archive to post process
     */
    public ParsingResult<ArchiveRoot> process(ParsingResult<ArchiveRoot> parsedArchive) {
        postProcessArchive(parsedArchive.getResult().getArchive().getName(), parsedArchive.getResult().getArchive().getVersion(), parsedArchive);
        return parsedArchive;
    }

    private final void postProcessArchive(String archiveName, String archiveVersion, ParsingResult<ArchiveRoot> parsedArchive) {
        postProcessIndexedArtifactToscaElement(parsedArchive.getResult(), parsedArchive.getResult().getNodeTypes());
        postProcessIndexedArtifactToscaElement(parsedArchive.getResult(), parsedArchive.getResult().getRelationshipTypes());

        postProcessTopology(archiveName, archiveVersion, parsedArchive, parsedArchive.getResult().getTopology());
    }

    private void postProcessTopology(String archiveName, String archiveVersion, ParsingResult<ArchiveRoot> parsedArchive, Topology topology) {
        if (topology == null) {
            return;
        }
        for (NodeTemplate nodeTemplate : topology.getNodeTemplates().values()) {
            postProcessNodeTemplate(archiveName, archiveVersion, parsedArchive, nodeTemplate);
        }
    }

    private void postProcessNodeTemplate(String archiveName, String archiveVersion, ParsingResult<ArchiveRoot> parsedArchive, NodeTemplate nodeTemplate) {
        postProcessInterfaces(parsedArchive.getResult(), nodeTemplate.getInterfaces());
        if (nodeTemplate.getRelationships() != null) {
            for (RelationshipTemplate relationship : nodeTemplate.getRelationships().values()) {
                postProcessInterfaces(parsedArchive.getResult(), relationship.getInterfaces());
            }
        }
    }

    private void postProcessIndexedArtifactToscaElement(ArchiveRoot archive, Map<String, ? extends IndexedArtifactToscaElement> elements) {
        if (elements == null) {
            return;
        }
        for (IndexedArtifactToscaElement element : elements.values()) {
            postProcessDeploymentArtifacts(archive, element);
            postProcessInterfaces(archive, element.getInterfaces());
        }
    }

    private void postProcessDeploymentArtifacts(ArchiveRoot archive, IndexedArtifactToscaElement element) {
        if (element.getArtifacts() == null) {
            return;
        }

        for (DeploymentArtifact artifact : element.getArtifacts().values()) {
            postProcessDeploymentArtifact(archive, artifact);
        }
    }

    private void postProcessInterfaces(ArchiveRoot archive, Map<String, Interface> interfaces) {
        if (interfaces == null) {
            return;
        }

        for (Interface interfaz : interfaces.values()) {
            for (Operation operation : interfaz.getOperations().values()) {
                postProcessImplementationArtifact(archive, operation.getImplementationArtifact());
            }
        }
    }

    private void postProcessDeploymentArtifact(ArchiveRoot archive, DeploymentArtifact artifact) {
        if (artifact != null) {
            artifact.setArchiveName(archive.getArchive().getName());
            artifact.setArchiveVersion(archive.getArchive().getVersion());
        }
    }

    private void postProcessImplementationArtifact(ArchiveRoot archive, ImplementationArtifact artifact) {
        if (artifact != null) {
            artifact.setArchiveName(archive.getArchive().getName());
            artifact.setArchiveVersion(archive.getArchive().getVersion());
        }
    }
}
