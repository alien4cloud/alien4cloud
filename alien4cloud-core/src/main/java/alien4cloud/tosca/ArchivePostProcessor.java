package alien4cloud.tosca;

import java.util.Map;
import java.util.Map.Entry;

import org.springframework.stereotype.Component;

import alien4cloud.component.model.IndexedInheritableToscaElement;
import alien4cloud.component.model.IndexedNodeType;
import alien4cloud.tosca.container.model.type.ImplementationArtifact;
import alien4cloud.tosca.container.model.type.Interface;
import alien4cloud.tosca.container.model.type.Operation;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingResult;

import com.google.common.collect.Maps;

@Component
public class ArchivePostProcessor {
    /**
     * Post process the archive: For every definition of the model it fills the id fields in the TOSCA elements from the key of the elements map.
     * 
     * @param archive The archive to post process
     */
    public void postProcess(ParsingResult<ArchiveRoot> parsedArchive) {
        doPostProcess(parsedArchive);
    }

    @SuppressWarnings("unchecked")
    private void doPostProcess(ParsingResult<ArchiveRoot> parsedArchive) {
        Map<String, String> globalElementsMap = Maps.newHashMap();
        postProcessArchive(parsedArchive, globalElementsMap);
        for (ParsingResult<?> subParsingResult : parsedArchive.getContext().getSubResults()) {
            postProcessArchive((ParsingResult<ArchiveRoot>) subParsingResult, globalElementsMap);
        }
        // postProcessNodeTypes(archive, archive.getNodeTypes());
    }

    private final void postProcessArchive(ParsingResult<ArchiveRoot> parsedArchive, Map<String, String> globalElementsMap) {
        postProcessElements(parsedArchive, parsedArchive.getResult().getNodeTypes(), globalElementsMap);
        postProcessNodeTypes(parsedArchive.getResult(), parsedArchive.getResult().getNodeTypes());
        postProcessElements(parsedArchive, parsedArchive.getResult().getRelationshipTypes(), globalElementsMap);
        postProcessElements(parsedArchive, parsedArchive.getResult().getCapabilityTypes(), globalElementsMap);
        postProcessElements(parsedArchive, parsedArchive.getResult().getArtifactTypes(), globalElementsMap);
    }

    private final void postProcessElements(ParsingResult<ArchiveRoot> parsedArchive, Map<String, ? extends IndexedInheritableToscaElement> elements,
            Map<String, String> globalElementsMap) {
        for (Entry<String, ? extends IndexedInheritableToscaElement> element : elements.entrySet()) {
            element.getValue().setId(element.getKey());
            String previous = globalElementsMap.put(element.getKey(), parsedArchive.getContext().getFileName());
            if (previous != null) {
                parsedArchive.getContext().getParsingErrors()
                        .add(new ParsingError("Type is defined twice in archive.", null, parsedArchive.getContext().getFileName(), null, previous));
            }
        }
    }

    private void postProcessNodeTypes(ArchiveRoot archive, Map<String, IndexedNodeType> nodeTypes) {
        for (IndexedNodeType nodeType : nodeTypes.values()) {
            for (Interface interfaz : nodeType.getInterfaces().values()) {
                for (Operation operation : interfaz.getOperations().values()) {
                    postProcessImplementationArtifact(archive, operation.getImplementationArtifact());
                }
            }
        }
    }

    private void postProcessImplementationArtifact(ArchiveRoot archive, ImplementationArtifact artifact) {
        if (artifact != null) {
            artifact.setArchiveName(archive.getArchive().getName());
            artifact.setArchiveVersion(archive.getArchive().getVersion());
        }
    }
}