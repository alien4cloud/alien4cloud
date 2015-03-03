package alien4cloud.tosca.parser.mapping.generator;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

import alien4cloud.tosca.parser.IChecker;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.KeyValueMappingTarget;
import alien4cloud.tosca.parser.MappingTarget;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.tosca.parser.YamlSimpleParser;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.parser.impl.base.CheckedTypeNodeParser;
import alien4cloud.tosca.parser.impl.base.ScalarParser;
import alien4cloud.tosca.parser.impl.base.TypeNodeParser;
import alien4cloud.tosca.parser.mapping.DefaultParser;

import com.google.common.collect.Maps;

/**
 * Load type mapping definition from yaml and add it to the type mapping registry.
 */
@Slf4j
@Component
public class MappingGenerator extends DefaultParser<Map<String, INodeParser>> {
    @Resource
    private ApplicationContext applicationContext;

    private Map<String, INodeParser> parsers = Maps.newHashMap();
    private Map<String, IMappingBuilder> mappingBuilders = Maps.newHashMap();
    private Map<String, IChecker> checkers = Maps.newHashMap();

    @PostConstruct
    public void initialize() {
        Map<String, INodeParser> contextParsers = applicationContext.getBeansOfType(INodeParser.class);
        // register parsers based on their class name.
        for (INodeParser parser : contextParsers.values()) {
            parsers.put(parser.getClass().getName(), parser);
        }
        Map<String, IMappingBuilder> contextMappingBuilders = applicationContext.getBeansOfType(IMappingBuilder.class);
        for (IMappingBuilder mappingBuilder : contextMappingBuilders.values()) {
            mappingBuilders.put(mappingBuilder.getKey(), mappingBuilder);
        }
        Map<String, IChecker> contextCheckers = applicationContext.getBeansOfType(IChecker.class);
        for (IChecker checker : contextCheckers.values()) {
            checkers.put(checker.getName(), checker);
        }
    }

    public Map<String, INodeParser> process(String resourceLocation) throws ParsingException {
        org.springframework.core.io.Resource resource = applicationContext.getResource(resourceLocation);
        YamlSimpleParser<Map<String, INodeParser>> nodeParser = new YamlSimpleParser<>(this);
        try {
            ParsingResult<Map<String, INodeParser>> result = nodeParser.parseFile(resource.getURI().toString(), resource.getFilename(),
                    resource.getInputStream(), null);
            if (result.getContext().getParsingErrors().isEmpty()) {
                return result.getResult();
            }
            throw new ParsingException(resource.getFilename(), result.getContext().getParsingErrors());
        } catch (IOException e) {
            log.error("Failed to open stream", e);
            throw new ParsingException(resource.getFilename(), new ParsingError(ErrorCode.MISSING_FILE, "Unable to load file.", null, e.getMessage(), null,
                    resourceLocation));
        }
    }

    public Map<String, INodeParser> parse(Node node, ParsingContextExecution context) {
        Map<String, INodeParser> parsers = Maps.newHashMap();
        if (node instanceof SequenceNode) {
            SequenceNode types = (SequenceNode) node;
            for (Node mapping : types.getValue()) {
                Map.Entry<String, INodeParser<?>> entry = processTypeMapping(mapping, context);
                if (entry != null) {
                    parsers.put(entry.getKey(), entry.getValue());
                }
            }
        } else {
            context.getParsingErrors().add(
                    new ParsingError(ErrorCode.SYNTAX_ERROR, "Mapping should be a sequence of type mappings", node.getStartMark(), "Actually was "
                            + node.getClass().getSimpleName(), node.getEndMark(), ""));
        }
        return parsers;
    }

    private Map.Entry<String, INodeParser<?>> processTypeMapping(Node node, ParsingContextExecution context) {
        try {
            return doProcessTypeMapping(node, context);
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            log.error("Failed to load class while parsing mapping", e);
            context.getParsingErrors().add(
                    new ParsingError(ErrorCode.SYNTAX_ERROR, "Unable to load class", node.getStartMark(), e.getMessage(), node.getEndMark(), ""));
            return null;
        }
    }

    private Map.Entry<String, INodeParser<?>> doProcessTypeMapping(Node node, ParsingContextExecution context) throws ClassNotFoundException,
            IllegalAccessException, InstantiationException {
        if (node instanceof MappingNode) {
            MappingNode mapping = (MappingNode) node;
            String yamlType = null;
            INodeParser<?> parser = null;
            for (NodeTuple tuple : mapping.getValue()) {
                if (yamlType == null) {
                    yamlType = ParserUtils.getScalar(tuple.getKeyNode(), context);
                    String type = ParserUtils.getScalar(tuple.getValueNode(), context);
                    if (type.startsWith("__")) {
                        parser = getWrapperParser(type, mapping, context);
                        return new AbstractMap.SimpleEntry<String, INodeParser<?>>(yamlType, parser);
                    }
                    parser = this.parsers.get(type);
                    if (parser != null) {
                        log.debug("Mapping yaml type <" + yamlType + "> using parser <" + type + ">");
                        return new AbstractMap.SimpleEntry<String, INodeParser<?>>(yamlType, parser);
                    }
                    parser = buildTypeNodeParser(yamlType, type);
                    // log.debug("Mapping yaml type <" + yamlType + "> to class <" + type + ">");
                    // Class<?> javaClass = Class.forName(type);
                    // parser = new TypeNodeParser<>(javaClass, yamlType);
                } else {
                    // process a mapping
                    map(tuple, (TypeNodeParser) parser, context);
                }
            }
            return new AbstractMap.SimpleEntry<String, INodeParser<?>>(yamlType, parser);
        } else {
            context.getParsingErrors().add(
                    new ParsingError(ErrorCode.SYNTAX_ERROR, "Unable to process type mapping.", node.getStartMark(),
                            "Mapping must be defined using a mapping node.", node.getEndMark(), ""));
        }
        return null;
    }

    private TypeNodeParser<?> buildTypeNodeParser(String yamlType, String javaType) throws ClassNotFoundException {
        String realJavaType = javaType;
        IChecker checker = null;
        if (javaType.contains("|")) {
            realJavaType = javaType.substring(0, javaType.indexOf("|"));
            String checkerName = javaType.substring(javaType.indexOf("|") + 1);
            log.debug(String.format("After parsing <%s>, realJavaType is <%s>, checkerName is <%s>", javaType, realJavaType, checkerName));
            checker = checkers.get(checkerName);
            if (checker == null) {
                log.warn(String.format("Can not find checker <%s>, using a standard TypeNodeParser", checkerName));
            }
        }
        Class<?> javaClass = Class.forName(realJavaType);
        if (checker == null) {
            log.debug("Mapping yaml type <" + yamlType + "> to class <" + realJavaType + ">");
            return new TypeNodeParser<>(javaClass, yamlType);
        } else {
            // TODO check that the type are compatible
            log.debug("Mapping yaml type <" + yamlType + "> to class <" + realJavaType + "> using checker " + checker.toString());
            return new CheckedTypeNodeParser<>(javaClass, yamlType, checker);
        }
    }

    private INodeParser<?> getWrapperParser(String wrapperKey, MappingNode mapping, ParsingContextExecution context) {
        IMappingBuilder builder = this.mappingBuilders.get(wrapperKey.substring(2));
        return builder.buildMapping(mapping, context).getParser();
    }

    private void map(NodeTuple tuple, TypeNodeParser<?> parser, ParsingContextExecution context) {
        String key = ParserUtils.getScalar(tuple.getKeyNode(), context);
        int positionMappingIndex = positionMappingIndex(key);
        if (positionMappingIndex > -1) {
            mapPositionMapping(positionMappingIndex, tuple.getValueNode(), parser, context);
        } else {
            MappingTarget mappingTarget = getMappingTarget(tuple.getValueNode(), context);
            if (mappingTarget != null) {
                parser.getYamlToObjectMapping().put(key, mappingTarget);
            }
        }
    }

    private MappingTarget getMappingTarget(Node mappingNode, ParsingContextExecution context) {
        if (mappingNode instanceof ScalarNode) {
            // create a scalar mapping
            String value = ParserUtils.getScalar(mappingNode, context);
            return new MappingTarget(value, parsers.get(ScalarParser.class.getName()));
        } else if (mappingNode instanceof MappingNode) {
            return mapMappingNode((MappingNode) mappingNode, context);
        }
        return null;
    }

    private int positionMappingIndex(String key) {
        if (key.startsWith("__")) {
            try {
                int position = Integer.valueOf(key.substring(2));
                return position;
            } catch (NumberFormatException e) {
                // not a position mapping
                return -1;
            }
        }
        return -1;
    }

    private void mapPositionMapping(Integer index, Node positionMapping, TypeNodeParser<?> parser, ParsingContextExecution context) {
        if (positionMapping instanceof MappingNode) {
            MappingNode mappingNode = (MappingNode) positionMapping;
            String key = null;
            MappingTarget valueMappingTarget = null;
            for (NodeTuple tuple : mappingNode.getValue()) {
                String tupleKey = ParserUtils.getScalar(tuple.getKeyNode(), context);
                if (tupleKey.equals("key")) {
                    key = ParserUtils.getScalar(tuple.getValueNode(), context);
                } else if (tupleKey.equals("value")) {
                    valueMappingTarget = getMappingTarget(tuple.getValueNode(), context);
                } else {
                    context.getParsingErrors().add(
                            new ParsingError(ErrorCode.SYNTAX_ERROR, "Unknown key for position mapping.", tuple.getKeyNode().getStartMark(), tupleKey, tuple
                                    .getKeyNode().getEndMark(), ""));
                }
            }
            if (valueMappingTarget == null) {
                return;
            }
            if (key == null) {
                parser.getYamlOrderedToObjectMapping().put(index, valueMappingTarget);
            } else {
                parser.getYamlOrderedToObjectMapping().put(index, new KeyValueMappingTarget(key, valueMappingTarget.getPath(), valueMappingTarget.getParser()));
            }
        } else {
            context.getParsingErrors().add(
                    new ParsingError(ErrorCode.SYNTAX_ERROR, "Position mapping must be a mapping node with key and value fields.", positionMapping
                            .getStartMark(), "", positionMapping.getEndMark(), ""));
        }
    }

    private MappingTarget mapMappingNode(MappingNode mappingNode, ParsingContextExecution context) {
        String key = ParserUtils.getScalar(mappingNode.getValue().get(0).getKeyNode(), context);
        IMappingBuilder mappingBuilder = mappingBuilders.get(key);
        if (mappingBuilder != null) {
            log.debug("Mapping yaml key <" + key + "> using mapping builder " + mappingBuilder.getClass().getName());
            return mappingBuilder.buildMapping(mappingNode, context);
        }
        context.getParsingErrors().add(
                new ParsingError(ErrorCode.SYNTAX_ERROR, "No mapping target found for key", mappingNode.getValue().get(0).getKeyNode().getStartMark(), key,
                        mappingNode.getValue().get(0).getKeyNode().getEndMark(), ""));
        return null;
    }

}