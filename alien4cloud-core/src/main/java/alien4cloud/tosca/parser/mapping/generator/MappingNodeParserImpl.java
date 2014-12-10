package alien4cloud.tosca.parser.mapping.generator;

import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.yaml.snakeyaml.nodes.*;

import alien4cloud.tosca.parser.*;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.parser.impl.base.ScalarParser;
import alien4cloud.tosca.parser.impl.base.TypeNodeParser;

import com.google.common.collect.Lists;

@Slf4j
public class MappingNodeParserImpl implements INodeParser<List<INodeParser<?>>> {
    private final Map<String, IMappingBuilder> mappingBuilders;
    private final Map<String, INodeParser> parsers;

    public MappingNodeParserImpl(Map<String, INodeParser> parsers, Map<String, IMappingBuilder> mappingBuilders) {
        this.parsers = parsers;
        this.mappingBuilders = mappingBuilders;
    }

    public List<INodeParser<?>> parse(Node node, ParsingContextExecution context) {
        List<INodeParser<?>> parsers = Lists.newArrayList();
        if (node instanceof SequenceNode) {
            SequenceNode types = (SequenceNode) node;
            for (Node mapping : types.getValue()) {
                INodeParser parser = processTypeMapping(mapping, context);
                if (parser != null) {
                    parsers.add(parser);
                }
            }
        }
        return parsers;
    }

    private INodeParser<?> processTypeMapping(Node node, ParsingContextExecution context) {
        try {
            return doProcessTypeMapping(node, context);
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            log.error("Failed to load class while parsing mapping", e);
            context.getParsingErrors().add(
                    new ParsingError(ErrorCode.SYNTAX_ERROR, "Unable to load class", node.getStartMark(), e.getMessage(), node.getEndMark(), ""));
            return null;
        }
    }

    private INodeParser<?> doProcessTypeMapping(Node node, ParsingContextExecution context) throws ClassNotFoundException, IllegalAccessException,
            InstantiationException {
        if (node instanceof MappingNode) {
            MappingNode mapping = (MappingNode) node;

            String yamlType = null;
            INodeParser<?> parser = null;
            for (NodeTuple tuple : mapping.getValue()) {
                if (yamlType == null) {
                    yamlType = ParserUtils.getScalar(tuple.getKeyNode(), context);
                    String javaType = ParserUtils.getScalar(tuple.getValueNode(), context);
                    Class<?> javaClass = Class.forName(javaType);
                    if (INodeParser.class.isAssignableFrom(javaClass)) {
                        log.debug("Mapping yaml type <" + yamlType + "> using parser <" + javaType + ">");
                        return (INodeParser<?>) javaClass.newInstance();
                    }
                    log.debug("Mapping yaml type <" + yamlType + "> to class <" + javaType + ">");
                    parser = new TypeNodeParser<>(javaClass, yamlType);
                } else {
                    // process a mapping
                    map(tuple, (TypeNodeParser) parser, context);
                }
            }
            return parser;
        } else {
            log.warn("Ignore node l:" + node.getStartMark().getLine() + " c:" + node.getStartMark().getColumn() + " to l:" + node.getEndMark().getLine()
                    + " c:" + node.getEndMark().getColumn());
        }
        return null;
    }

    private void map(NodeTuple tuple, TypeNodeParser<?> parser, ParsingContextExecution context) {
        String key = ParserUtils.getScalar(tuple.getKeyNode(), context);
        if (tuple.getValueNode() instanceof ScalarNode) {
            // create a scalar mapping
            String value = ParserUtils.getScalar(tuple.getValueNode(), context);
            parser.getYamlToObjectMapping().put(key, new MappingTarget(value, parsers.get(ScalarParser.class.getName())));
            log.debug("Mapping yaml key <" + key + "> using scalar parser into field <" + value + ">");
        } else if (tuple.getValueNode() instanceof MappingNode) {
            MappingTarget mappingTarget = mapMappingNode((MappingNode) tuple.getValueNode(), context);
            if (mappingTarget != null) {
                parser.getYamlToObjectMapping().put(key, mappingTarget);
            }
        }
    }

    private MappingTarget mapMappingNode(MappingNode mappingNode, ParsingContextExecution context) {
        String key = ParserUtils.getScalar(mappingNode.getValue().get(0).getKeyNode(), context);
        IMappingBuilder mappingBuilder = mappingBuilders.get(key);
        if (mappingBuilder != null) {
            return mappingBuilder.buildMapping(mappingNode, context);
        }
        return null;
    }

    @Override
    public boolean isDeferred(ParsingContextExecution context) {
        return false;
    }
}
