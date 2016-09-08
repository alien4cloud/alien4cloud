package alien4cloud.tosca.parser.postprocess;

import static alien4cloud.utils.AlienUtils.safe;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.model.components.IndexedInheritableToscaElement;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingContextExecution;

/**
 * Performs post processing for a TOSCA type:
 * - Set it's archive version and name
 */
@Component
public class ToscaTypePostProcessor implements IPostProcessor<IndexedInheritableToscaElement> {
    @Resource
    private PropertyDefinitionPostProcessor propertyDefinitionPostProcessor;

    @Override
    public void process(IndexedInheritableToscaElement instance) {
        ArchiveRoot archiveRoot = ParsingContextExecution.getRootObj();
        instance.setArchiveName(archiveRoot.getArchive().getName());
        instance.setArchiveVersion(archiveRoot.getArchive().getVersion());

        // FIXME we had a check for element duplication cross types, is it required, do we still want/need that ?
        // FIXME the real thing we may want to check is more on alien side and consider the fact that a type should not be duplicated in multiple archives.
//        String previous = globalElementsMap.put(element.getKey(), parsedArchive.getContext().getFileName());
//        if (previous != null) {
//            parsedArchive.getContext().getParsingErrors().add(new ParsingError(ErrorCode.DUPLICATED_ELEMENT_DECLARATION,
//                    "Type is defined twice in archive.", null, parsedArchive.getContext().getFileName(), null, previous));
//        }

        safe(instance.getProperties()).entrySet().stream().forEach(propertyDefinitionPostProcessor);
    }
}