package alien4cloud.tosca.parser.postprocess;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;
import org.alien4cloud.tosca.model.types.ArtifactType;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.DataType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.PrimitiveDataType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.normative.constants.NormativeCapabilityTypes;
import org.alien4cloud.tosca.normative.constants.NormativeTypesConstant;
import org.alien4cloud.tosca.normative.types.ToscaTypes;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.model.components.IndexedModelUtils;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.impl.ErrorCode;
import lombok.extern.slf4j.Slf4j;

/**
 * Derived from post processor checks that the defined parent
 */
@Slf4j
@Component
public class DerivedFromPostProcessor implements IPostProcessor<Map<String, ? extends AbstractInheritableToscaType>> {
    @Override
    public void process(Map<String, ? extends AbstractInheritableToscaType> instances) {
        // Detect cyclic derived from
        Map<AbstractInheritableToscaType, String> processed = new IdentityHashMap<>();
        Map<AbstractInheritableToscaType, String> processing = new IdentityHashMap<>();
        // Then process to get the list of derived from and merge instances
        for (AbstractInheritableToscaType instance : safe(instances).values()) {
            process(processed, processing, instance, instances);
        }
    }

    private void process(Map<AbstractInheritableToscaType, String> processed, Map<AbstractInheritableToscaType, String> processing,
            AbstractInheritableToscaType instance, Map<String, ? extends AbstractInheritableToscaType> instances) {
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
        if (derivedFrom != null && derivedFrom.size() > 1) {
            // The type has been already processed.
            return;
        }
        if (derivedFrom == null || derivedFrom.isEmpty()) {
            // If the user forgot to derive from Root, automatically do it but make an alert
            String defaultDerivedFrom = null;
            if (instance instanceof NodeType && !NormativeTypesConstant.ROOT_NODE_TYPE.equals(instance.getElementId())) {
                defaultDerivedFrom = NormativeTypesConstant.ROOT_NODE_TYPE;
            } else if (instance instanceof RelationshipType && !NormativeTypesConstant.ROOT_RELATIONSHIP_TYPE.equals(instance.getElementId())) {
                defaultDerivedFrom = NormativeTypesConstant.ROOT_RELATIONSHIP_TYPE;
            } else if (instance instanceof DataType && !NormativeTypesConstant.ROOT_DATA_TYPE.equals(instance.getElementId())) {
                defaultDerivedFrom = NormativeTypesConstant.ROOT_DATA_TYPE;
            } else if (instance instanceof CapabilityType && !NormativeCapabilityTypes.ROOT.equals(instance.getElementId())) {
                defaultDerivedFrom = NormativeCapabilityTypes.ROOT;
            } else if (instance instanceof ArtifactType && !NormativeTypesConstant.ROOT_ARTIFACT_TYPE.equals(instance.getElementId())) {
                defaultDerivedFrom = NormativeTypesConstant.ROOT_ARTIFACT_TYPE;
            }
            if (defaultDerivedFrom != null) {
                derivedFrom = new ArrayList<>();
                derivedFrom.add(defaultDerivedFrom);
                instance.setDerivedFrom(derivedFrom);
                Node node = ParsingContextExecution.getObjectToNodeMap().get(instance);
                ParsingContextExecution.getParsingErrors()
                        .add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.DERIVED_FROM_NOTHING, defaultDerivedFrom,
                                node.getStartMark(), "The " + instance.getClass().getSimpleName() + " " + instance.getElementId()
                                        + " derives from nothing, default " + defaultDerivedFrom + " will be set as parent type.",
                                node.getEndMark(), instance.getElementId()));
            } else {
                // Non managed default parent type then returns
                return;
            }
        }

        String parentElementType = derivedFrom.get(0);

        // Merge the type with it's parent except for primitive data types.
        if (instance instanceof DataType && ToscaTypes.isSimple(parentElementType)) {
            if (instance instanceof PrimitiveDataType) {
                log.debug("Do not merge data type instance with parent as it extends from a primitive type.");
            } else {
                Node node = ParsingContextExecution.getObjectToNodeMap().get(instance);
                // type has not been parsed as primitive because it has some properties
                ParsingContextExecution.getParsingErrors()
                        .add(new ParsingError(ErrorCode.SYNTAX_ERROR, "Primitive types cannot define properties.", node.getStartMark(),
                                "The defined type inherit from a primitive type but defines some properties.", node.getEndMark(), parentElementType));
            }
            return;
        }

        AbstractInheritableToscaType parent = instances.get(parentElementType);
        if (parent == null) {
            parent = ToscaContext.get(instance.getClass(), parentElementType);
        } else {
            // first process the parent type
            processing.put(instance, null);
            process(processed, processing, parent, instances);
            processing.remove(instance);
        }
        if (parent == null) {
            Node node = ParsingContextExecution.getObjectToNodeMap().get(instance);
            ParsingContextExecution.getParsingErrors().add(new ParsingError(ErrorCode.TYPE_NOT_FOUND, "Derived_from type not found", node.getStartMark(),
                    "The type specified as parent is not found neither in the archive or its dependencies.", node.getEndMark(), parentElementType));
            return;
        }

        // Merge with parent type
        IndexedModelUtils.mergeInheritableIndex(parent, instance);

        processed.put(instance, null);
    }
}
