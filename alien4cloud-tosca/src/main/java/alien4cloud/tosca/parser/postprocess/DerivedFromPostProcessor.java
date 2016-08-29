package alien4cloud.tosca.parser.postprocess;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import alien4cloud.tosca.normative.ToscaType;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.model.components.IndexedInheritableToscaElement;
import alien4cloud.model.components.IndexedModelUtils;
import alien4cloud.model.components.PrimitiveIndexedDataType;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.impl.ErrorCode;
import lombok.extern.slf4j.Slf4j;

/**
 * Derived from post processor checks that the defined parent
 */
@Slf4j
@Component
public class DerivedFromPostProcessor implements IPostProcessor<Map<String, ? extends IndexedInheritableToscaElement>> {
    @Override
    public void process(Map<String, ? extends IndexedInheritableToscaElement> instances) {
        // Detect cyclic derived from
        Map<IndexedInheritableToscaElement, String> processed = new IdentityHashMap<>();
        Map<IndexedInheritableToscaElement, String> processing = new IdentityHashMap<>();
        // Then process to get the list of derived from and merge instances
        for (IndexedInheritableToscaElement instance : safe(instances).values()) {
            process(processed, processing, instance, instances);
        }
    }

    private void process(Map<IndexedInheritableToscaElement, String> processed, Map<IndexedInheritableToscaElement, String> processing,
            IndexedInheritableToscaElement instance, Map<String, ? extends IndexedInheritableToscaElement> instances) {
        if (processed.containsKey(instance)) {
            // Already processed
            return;
        }
        if (processing.containsKey(instance)) {
            // Cyclic dependency as parent is currently being processed...
            Node node = ParsingContextExecution.getObjectToNodeMap().get(instance);
            ParsingContextExecution.getParsingErrors()
                    .add(new ParsingError(ErrorCode.CYCLIC_DERIVED_FROM, "Cyclic derived from has been detected", node.getStartMark(),
                            "The type specified as parent or one of it's parent type refers the current type as parent, invalid cycle detected.",
                            node.getEndMark(), instance.getElementId()));
            processing.remove(instance);
            return;
        }

        List<String> derivedFrom = instance.getDerivedFrom();
        if (derivedFrom == null || derivedFrom.isEmpty() || derivedFrom.size() > 1) {
            // Either the type has no parents, either it has been already processed.
            return;
        }

        String parentElementType = derivedFrom.get(0);

        // Merge the type with it's parent except for primitive data types.
        if (instance instanceof PrimitiveIndexedDataType && ToscaType.isSimple(parentElementType)) {
            log.debug("Do not merge data type instance with parent as it extends from a primitive type.");
            return;
        }

        IndexedInheritableToscaElement parent = instances.get(parentElementType);
        if (parent == null) {
            parent = ToscaContext.get(instance.getClass(), parentElementType);
        } else {
            // first process the parent type
            processing.put(instance, null);
            process(processed, processing, parent, instances);
            processing.remove(instance);
        }
        if (parent == null) {
            Node node = ParsingContextExecution.getObjectToNodeMap().get(derivedFrom);
            ParsingContextExecution.getParsingErrors().add(new ParsingError(ErrorCode.TYPE_NOT_FOUND, "Derived_from type not found", node.getStartMark(),
                    "The type specified as parent is not found neither in the archive or its dependencies.", node.getEndMark(), parentElementType));
            return;
        }

        // Merge with parent type
        IndexedModelUtils.mergeInheritableIndex(parent, instance);

        processed.put(instance, null);
    }
}
