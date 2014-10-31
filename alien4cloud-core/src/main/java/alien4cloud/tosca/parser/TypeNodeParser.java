package alien4cloud.tosca.parser;

import java.util.Map;

import lombok.Getter;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import com.google.common.collect.Maps;

@Getter
public class TypeNodeParser<T> extends AbstractTypeNodeParser implements INodeParser<T> {
    private final Class<T> type;
    private final String toscaType;
    private final Map<String, MappingTarget> yamlToObjectMapping = Maps.newHashMap();
    private final Map<Integer, MappingTarget> yamlOrderedToObjectMapping = Maps.newHashMap();

    public TypeNodeParser(Class<T> type, String toscaType) {
        this.type = type;
        this.toscaType = toscaType;
    }

    @Override
    public boolean isDeffered() {
        return false;
    }

    public T parse(Node node, ParsingContext context) {
        if (node instanceof MappingNode) {
            return doParse((MappingNode) node, context);
        } else if (node instanceof ScalarNode) {
            String scalarValue = ((ScalarNode) node).getValue();
            if (scalarValue == null || scalarValue.trim().isEmpty()) {
                // node is just not defined, return null.
                return null;
            }
        }
        context.getParsingErrors().add(
                new ParsingError("Invalid Yaml node type.", node.getStartMark(), "Expected the type to match tosca type", node.getEndMark(), toscaType));
        return null;
    }

    private T doParse(MappingNode node, ParsingContext context) {
        T instance;
        try {
            instance = type.newInstance();

            BeanWrapper instanceWrapper = new BeanWrapperImpl(instance);

            if (context.getRoot() == null) {
                context.setRoot(instanceWrapper);
            }

            for (int i = 0; i < node.getValue().size(); i++) {
                // lets proceed with node mapping.
                mapTuple(instanceWrapper, node.getValue().get(i), i, context);
            }

            return instance;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ParsingTechnicalException("Unexpected error while parsing tosca definition.", e);
        }
    }

    private void mapTuple(BeanWrapper instance, NodeTuple nodeTuple, int nodeTupleIndex, ParsingContext context) {
        String key = ParserUtils.getScalar(nodeTuple.getKeyNode(), context.getParsingErrors());
        if (key == null) {
            return;
        }

        // get the field that matches the tuple based on property index in the object definition.
        MappingTarget target = yamlOrderedToObjectMapping.get(nodeTupleIndex);
        // get the field that matches the given key.
        if (target == null) {
            target = yamlToObjectMapping.get(key);
        }

        if (target == null) {
            context.getParsingErrors().add(
                    new ParsingError(ParsingErrorLevel.WARNING, "Ignored field during import", nodeTuple.getKeyNode().getStartMark(),
                            "tosca key is not recognized", nodeTuple.getKeyNode().getEndMark(), key));
        } else {
            // set the value to the required path
            BeanWrapper targetBean = target.isRootPath() ? context.getRoot() : instance;
            if (target.getParser().isDeffered()) {
                context.getDefferedParsers().add(new DefferedParsingValueExecutor(key, targetBean, context, target, nodeTuple.getValueNode()));
            } else {
                parseAndSetValue(targetBean, key, nodeTuple.getValueNode(), context, target);
            }
        }
    }
}