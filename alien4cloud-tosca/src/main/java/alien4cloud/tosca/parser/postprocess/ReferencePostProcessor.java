package alien4cloud.tosca.parser.postprocess;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.model.components.IndexedInheritableToscaElement;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.impl.ErrorCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Post processor that performs validation of references in a tosca template.
 */
@Component
public class ReferencePostProcessor implements IPostProcessor<ReferencePostProcessor.TypeReference> {

    @Override
    public void process(TypeReference typeReference) {
        for (Class<? extends IndexedInheritableToscaElement> clazz : typeReference.classes) {
            IndexedInheritableToscaElement reference = ToscaContext.get(clazz, typeReference.getKey());
            if (reference != null) {
                return;
            }
        }
        Node node = ParsingContextExecution.getObjectToNodeMap().get(typeReference.getKey());
        ParsingContextExecution.getParsingErrors().add(new ParsingError(ErrorCode.TYPE_NOT_FOUND, "Type not found", node.getStartMark(),
                "The referenced type is not found neither in the archive or it's dependencies.", node.getEndMark(), typeReference.getKey()));
    }

    @Getter
    @Setter
    public static class TypeReference {
        private Class<? extends IndexedInheritableToscaElement>[] classes;
        private String key;

        public TypeReference(String key, Class<? extends IndexedInheritableToscaElement>... classes) {
            this.key = key;
            this.classes = classes;
        }
    }
}