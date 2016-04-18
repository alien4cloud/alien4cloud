package alien4cloud.tosca;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import alien4cloud.topology.TopologyService;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import alien4cloud.model.components.DeploymentArtifact;
import alien4cloud.model.components.ImplementationArtifact;
import alien4cloud.model.components.IndexedArtifactToscaElement;
import alien4cloud.model.components.IndexedInheritableToscaElement;
import alien4cloud.model.components.Interface;
import alien4cloud.model.components.Operation;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.tosca.parser.impl.ErrorCode;

import com.google.common.collect.Maps;

import javax.annotation.Resource;

@Component
public class ArchivePostProcessor {

    @Resource
    TopologyService topologyService;

    /**
     * Post process the archive: For every definition of the model it fills the id fields in the TOSCA elements from the key of the elements map.
     * 
     * @param parsedArchive The archive to post process
     */
    public void postProcess(ParsingResult<ArchiveRoot> parsedArchive) {
        doPostProcess(parsedArchive);
    }

    @SuppressWarnings("unchecked")
    private void doPostProcess(ParsingResult<ArchiveRoot> parsedArchive) {
        Map<String, String> globalElementsMap = Maps.newHashMap();
        postProcessArchive(parsedArchive.getResult().getArchive().getName(), parsedArchive.getResult().getArchive().getVersion(), parsedArchive,
                globalElementsMap);
        // TODO manage sub-archives
        // for (ParsingResult<?> subParsingResult : parsedArchive.getContext().getSubResults()) {
        // if (subParsingResult.getResult() instanceof ArchiveRoot) {
        // postProcessArchive(parsedArchive.getResult().getArchive().getName(), parsedArchive.getResult().getArchive().getVersion(),
        // (ParsingResult<ArchiveRoot>) subParsingResult, globalElementsMap);
        // }
        // }
    }

    private final void postProcessArchive(String archiveName, String archiveVersion, ParsingResult<ArchiveRoot> parsedArchive,
            Map<String, String> globalElementsMap) {
        postProcessElements(archiveName, archiveVersion, parsedArchive, parsedArchive.getResult().getNodeTypes(), globalElementsMap);
        postProcessIndexedArtifactToscaElement(parsedArchive.getResult(), parsedArchive.getResult().getNodeTypes());
        postProcessElements(archiveName, archiveVersion, parsedArchive, parsedArchive.getResult().getRelationshipTypes(), globalElementsMap);
        postProcessIndexedArtifactToscaElement(parsedArchive.getResult(), parsedArchive.getResult().getRelationshipTypes());
        postProcessElements(archiveName, archiveVersion, parsedArchive, parsedArchive.getResult().getCapabilityTypes(), globalElementsMap);
        postProcessElements(archiveName, archiveVersion, parsedArchive, parsedArchive.getResult().getArtifactTypes(), globalElementsMap);
        postProcessTopology(archiveName, archiveVersion, parsedArchive, parsedArchive.getResult().getTopology(), globalElementsMap);
    }

    private void postProcessTopology(String archiveName, String archiveVersion, ParsingResult<ArchiveRoot> parsedArchive, Topology topology,
            Map<String, String> globalElementsMap) {
        if (topology == null) {
            return;
        }
        for (NodeTemplate nodeTemplate : topology.getNodeTemplates().values()) {
            postProcessNodeTemplate(archiveName, archiveVersion, parsedArchive, nodeTemplate, globalElementsMap);
        }
        postProcessNodeTemplateName(topology, parsedArchive);
    }

    /**
     * Rename the node template with an invalid name on the topology.
     * @param topology
     * @param parsedArchive
     */
    private void postProcessNodeTemplateName(Topology topology, ParsingResult<ArchiveRoot> parsedArchive) {
        if (topology.getNodeTemplates() != null && !topology.getNodeTemplates().isEmpty()) {
            Map<String, NodeTemplate> nodeTemplates = Maps.newHashMap(topology.getNodeTemplates());
            for (Entry<String, NodeTemplate> nodeEntry : nodeTemplates.entrySet()) {
                String nodeName = nodeEntry.getKey();
                if (!topologyService.isValidNodeName(nodeName)) {
                    String newName = nodeName.toString().replaceAll("-", "_").replaceAll("\\.", "_");
                    newName = StringUtils.stripAccents(newName);
                    if (nodeTemplates.containsKey(newName)) {
                        int i = 1;
                        while (nodeTemplates.containsKey(newName + "_" + i)) {
                            i++;
                        }
                        newName = newName + "_" + i;
                    }
                    topologyService.renameNodeTemplate(topology, nodeName, newName);
                    parsedArchive.getContext().getParsingErrors().add(
                            new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.INVALID_NODE_TEMPLATE_NAME, nodeName, null, nodeName, null, newName));
                }
            }
        }
    }

    private void postProcessNodeTemplate(String archiveName, String archiveVersion, ParsingResult<ArchiveRoot> parsedArchive, NodeTemplate nodeTemplate,
            Map<String, String> globalElementsMap) {
        postProcessInterfaces(parsedArchive.getResult(), nodeTemplate.getInterfaces());
        if (nodeTemplate.getRelationships() != null) {
            for (RelationshipTemplate relationship : nodeTemplate.getRelationships().values()) {
                postProcessInterfaces(parsedArchive.getResult(), relationship.getInterfaces());
            }
        }
    }

    private final void postProcessElements(String archiveName, String archiveVersion, ParsingResult<ArchiveRoot> parsedArchive,
            Map<String, ? extends IndexedInheritableToscaElement> elements, Map<String, String> globalElementsMap) {
        if (elements == null) {
            return;
        }
        for (Entry<String, ? extends IndexedInheritableToscaElement> element : elements.entrySet()) {
            element.getValue().setId(element.getKey());
            element.getValue().setArchiveName(archiveName);
            element.getValue().setArchiveVersion(archiveVersion);
            String previous = globalElementsMap.put(element.getKey(), parsedArchive.getContext().getFileName());
            if (previous != null) {
                parsedArchive
                        .getContext()
                        .getParsingErrors()
                        .add(new ParsingError(ErrorCode.DUPLICATED_ELEMENT_DECLARATION, "Type is defined twice in archive.", null, parsedArchive.getContext()
                                .getFileName(), null, previous));
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