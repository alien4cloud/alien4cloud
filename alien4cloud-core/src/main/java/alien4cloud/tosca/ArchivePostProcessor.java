package alien4cloud.tosca;

import java.util.Map;
import java.util.Map.Entry;

import org.springframework.stereotype.Component;

import alien4cloud.component.model.IndexedArtifactToscaElement;
import alien4cloud.component.model.IndexedInheritableToscaElement;
import alien4cloud.tosca.container.model.type.ImplementationArtifact;
import alien4cloud.tosca.container.model.type.Interface;
import alien4cloud.tosca.container.model.type.Operation;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.tosca.parser.impl.ErrorCode;

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
        postProcessArchive(parsedArchive.getResult().getArchive().getName(), parsedArchive.getResult().getArchive().getVersion(), parsedArchive,
                globalElementsMap);
        for (ParsingResult<?> subParsingResult : parsedArchive.getContext().getSubResults()) {
            if (subParsingResult.getResult() instanceof ArchiveRoot) {
                postProcessArchive(parsedArchive.getResult().getArchive().getName(), parsedArchive.getResult().getArchive().getVersion(),
                        (ParsingResult<ArchiveRoot>) subParsingResult, globalElementsMap);
            }
        }
    }

    private final void postProcessArchive(String archiveName, String archiveVersion, ParsingResult<ArchiveRoot> parsedArchive,
            Map<String, String> globalElementsMap) {
        postProcessElements(archiveName, archiveVersion, parsedArchive, parsedArchive.getResult().getNodeTypes(), globalElementsMap);
        postProcessIndexedArtifactToscaElement(parsedArchive.getResult(), parsedArchive.getResult().getNodeTypes());
        postProcessElements(archiveName, archiveVersion, parsedArchive, parsedArchive.getResult().getRelationshipTypes(), globalElementsMap);
        postProcessIndexedArtifactToscaElement(parsedArchive.getResult(), parsedArchive.getResult().getRelationshipTypes());
        postProcessElements(archiveName, archiveVersion, parsedArchive, parsedArchive.getResult().getCapabilityTypes(), globalElementsMap);
        postProcessElements(archiveName, archiveVersion, parsedArchive, parsedArchive.getResult().getArtifactTypes(), globalElementsMap);
    }

    private final void postProcessElements(String archiveName, String archiveVersion, ParsingResult<ArchiveRoot> parsedArchive,
            Map<String, ? extends IndexedInheritableToscaElement> elements, Map<String, String> globalElementsMap) {
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
        for (IndexedArtifactToscaElement element : elements.values()) {
            postProcessInterfaces(archive, element);
        }
    }

    private void postProcessInterfaces(ArchiveRoot archive, IndexedArtifactToscaElement element) {
        if (element.getInterfaces() == null) {
            return;
        }

        for (Interface interfaz : element.getInterfaces().values()) {
            for (Operation operation : interfaz.getOperations().values()) {
                postProcessImplementationArtifact(archive, operation.getImplementationArtifact());
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