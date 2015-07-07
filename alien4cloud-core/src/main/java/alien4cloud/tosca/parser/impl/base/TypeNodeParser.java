package alien4cloud.tosca.parser.impl.base;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import alien4cloud.tosca.parser.AbstractTypeNodeParser;
import alien4cloud.tosca.parser.DefferedParsingValueExecutor;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.MappingTarget;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ParsingTechnicalException;
import alien4cloud.tosca.parser.impl.ErrorCode;

import com.google.common.collect.Maps;

@Slf4j
@Getter
public class TypeNodeParser<T> extends AbstractTypeNodeParser implements INodeParser<T> {
    private final Class<T> type;
    private final Map<String, MappingTarget> yamlToObjectMapping;
    private final Map<Integer, MappingTarget> yamlOrderedToObjectMapping;

    public TypeNodeParser(Class<T> type, String toscaType) {
        super(toscaType);
        this.type = type;
        yamlToObjectMapping = Maps.newHashMap();
        yamlOrderedToObjectMapping = Maps.newHashMap();
    }

    @Override
    public boolean isDeferred(ParsingContextExecution context) {
        return false;
    }

    @Override
    public int getDefferedOrder(ParsingContextExecution context) {
        return 0;
    }

    @Override
    public T parse(Node node, ParsingContextExecution context) {
        return parse(node, context, null);
    }

    /**
     * Do the node parsing using the given instance rather than creating a new one.
     * 
     * @param node The node to parse.
     * @param context The parsing context.
     * @param instance The instance in which to parse the node (or null to create a new instance).
     * @return The given instance or a new one if none was provided.
     */
    public T parse(Node node, ParsingContextExecution context, T instance) {
        if (node instanceof MappingNode) {
            return doParse((MappingNode) node, context, instance);
        } else if (node instanceof ScalarNode) {
            String scalarValue = ((ScalarNode) node).getValue();
            if (scalarValue == null || scalarValue.trim().isEmpty()) {
                // node is just not defined, return null.
                return null;
            } else {
                // try to use instance default constructor based on string if any
                Constructor<T> constructor;
                try {
                    constructor = type.getConstructor(String.class);
                } catch (NoSuchMethodException e) {
                    // scalar value is not allowed to parse the node.
                    ParserUtils.addTypeError(node, context.getParsingErrors(), getToscaType());
                    return null;
                }
                try {
                    return constructor.newInstance(scalarValue);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    log.error("Error while parsing Yaml, scalar value is not valid.", e);
                    context.getParsingErrors().add(
                            new ParsingError(ErrorCode.SYNTAX_ERROR, "Invalid scalar value.", node.getStartMark(),
                                    "Tosca type cannot be expressed with the given scalar value.", node.getEndMark(), getToscaType()));
                    return null;
                }
            }
        }
        ParserUtils.addTypeError(node, context.getParsingErrors(), getToscaType());
        return null;
    }

    private T doParse(MappingNode node, ParsingContextExecution context, T instance) {
        try {
            if (instance == null) {
                instance = type.newInstance();
            }

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

    private void mapTuple(BeanWrapper instance, NodeTuple nodeTuple, int nodeTupleIndex, ParsingContextExecution context) {
        String key = ParserUtils.getScalar(nodeTuple.getKeyNode(), context);
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
                    new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.UNRECOGNIZED_PROPERTY, "Ignored field during import", nodeTuple.getKeyNode()
                            .getStartMark(), "tosca key is not recognized", nodeTuple.getValueNode().getEndMark(), key));
        } else {
            // set the value to the required path
            BeanWrapper targetBean = target.isRootPath() ? context.getRoot() : instance;
            if (target.getParser().isDeferred(context)) {
                context.addDeferredParser(new DefferedParsingValueExecutor(key, targetBean, context, target, nodeTuple.getValueNode(), target.getParser()
                        .getDefferedOrder(context)));
            } else {
                parseAndSetValue(targetBean, key, nodeTuple.getValueNode(), context, target);
            }
        }
    }

}