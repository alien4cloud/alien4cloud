package alien4cloud.tosca.parser.mapping.generator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.aop.framework.Advised;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.*;

import com.google.common.collect.Maps;

import alien4cloud.tosca.parser.*;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.parser.impl.base.BaseParserFactory;
import alien4cloud.tosca.parser.impl.base.ScalarParser;
import alien4cloud.tosca.parser.impl.base.TypeNodeParser;
import lombok.extern.slf4j.Slf4j;

/**
 * Load type mapping definition from yaml and add it to the type mapping registry.
 */
@Slf4j
@Component
public class MappingGenerator implements INodeParser<Map<String, INodeParser>> {
    @Resource
    private ApplicationContext applicationContext;
    @Resource
    private BaseParserFactory baseParserFactory;

    private Map<String, INodeParser> parsers = Maps.newHashMap();
    private Map<String, IMappingBuilder> mappingBuilders = Maps.newHashMap();

    @PostConstruct
    public void initialize() {
        Map<String, INodeParser> contextParsers = applicationContext.getBeansOfType(INodeParser.class, false, true);
        // register parsers based on their class name.
        for (INodeParser parser : contextParsers.values()) {
            String className;
            if (parser instanceof Advised) {
                className = ((Advised) parser).getTargetSource().getTargetClass().getName();
            } else {
                className = parser.getClass().getName();
            }
            parsers.put(className, parser);
        }
        Map<String, IMappingBuilder> contextMappingBuilders = applicationContext.getBeansOfType(IMappingBuilder.class);
        for (IMappingBuilder mappingBuilder : contextMappingBuilders.values()) {
            mappingBuilders.put(mappingBuilder.getKey(), mappingBuilder);
        }
    }

    public Map<String, INodeParser> process(Path resourceLocation) throws ParsingException {
        org.springframework.core.io.Resource resource = new FileSystemResource(resourceLocation.toFile());
        return process(resource);
    }

    public Map<String, INodeParser> process(String resourceLocation) throws ParsingException {
        org.springframework.core.io.Resource resource = applicationContext.getResource(resourceLocation);
        return process(resource);
    }

    private Map<String, INodeParser> process(org.springframework.core.io.Resource resource) throws ParsingException {
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
            throw new ParsingException(resource.getFilename(),
                    new ParsingError(ErrorCode.MISSING_FILE, "Unable to load file.", null, e.getMessage(), null, resource.getFilename()));
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
            context.getParsingErrors().add(new ParsingError(ErrorCode.SYNTAX_ERROR, "Mapping should be a sequence of type mappings", node.getStartMark(),
                    "Actually was " + node.getClass().getSimpleName(), node.getEndMark(), ""));
        }
        return parsers;
    }

    private Map.Entry<String, INodeParser<?>> processTypeMapping(Node node, ParsingContextExecution context) {
        try {
            return doProcessTypeMapping(node, context);
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            log.error("Failed to load class while parsing mapping", e);
            context.getParsingErrors()
                    .add(new ParsingError(ErrorCode.SYNTAX_ERROR, "Unable to load class", node.getStartMark(), e.getMessage(), node.getEndMark(), ""));
            return null;
        }
    }

    private Map.Entry<String, INodeParser<?>> doProcessTypeMapping(Node node, ParsingContextExecution context)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        // process the mapping of a given type
        if (node instanceof MappingNode) {
            MappingNode mapping = (MappingNode) node;
            String yamlType = null;
            INodeParser<?> parser = null;
            for (NodeTuple tuple : mapping.getValue()) {
                // the first in a node mapping must be the yaml key for the type and the definition of the java type to map to or a direct reference to a
                // parser.
                if (yamlType == null) {
                    yamlType = ParserUtils.getScalar(tuple.getKeyNode(), context);
                    // it's value design the java parser to be used to build the java type.
                    String type = ParserUtils.getScalar(tuple.getValueNode(), context);
                    if (type.startsWith("__")) {
                        parser = getWrapperParser(type, mapping, context);
                        return new AbstractMap.SimpleEntry<String, INodeParser<?>>(yamlType, parser);
                    }
                    // try to find a registered parser for the type. Direct parser reference.
                    parser = this.parsers.get(type);
                    if (parser != null) {
                        log.debug("Mapping yaml type <" + yamlType + "> using parser <" + type + ">");
                        return new AbstractMap.SimpleEntry<String, INodeParser<?>>(yamlType, parser);
                    }
                    // The value for this mapping is not an exising parser, that may be either a java type either the reference to a mapping builder (a
                    // collection for example).
                    IMappingBuilder builder = mappingBuilders.get(type);
                    if (builder != null) {
                        mapping.getValue().add(0, new NodeTuple(new ScalarNode(new Tag(builder.getKey()), builder.getKey(), tuple.getKeyNode().getStartMark(),
                                tuple.getKeyNode().getEndMark(), 'c'), tuple.getValueNode()));

                        // there is a builder
                        parser = builder.buildMapping(mapping, context).getParser();
                        return new AbstractMap.SimpleEntry<String, INodeParser<?>>(yamlType, parser);
                    } else {
                        // If the type doesn't design a referenced parser then we should try to build it.
                        parser = buildTypeNodeParser(yamlType, type);
                    }
                } else {
                    // process a mapping
                    map(tuple, (TypeNodeParser) parser, context);
                }
            }
            return new AbstractMap.SimpleEntry<String, INodeParser<?>>(yamlType, parser);
        } else {
            context.getParsingErrors().add(new ParsingError(ErrorCode.SYNTAX_ERROR, "Unable to process type mapping.", node.getStartMark(),
                    "Mapping must be defined using a mapping node.", node.getEndMark(), ""));
        }
        return null;
    }

    private TypeNodeParser<?> buildTypeNodeParser(String yamlType, String javaType) throws ClassNotFoundException {
        String realJavaType = javaType;

        Class<?> javaClass = Class.forName(realJavaType);
        log.debug("Mapping yaml type <" + yamlType + "> to class <" + realJavaType + ">");
        return baseParserFactory.getTypeNodeParser(javaClass, yamlType);
    }

    private INodeParser<?> getWrapperParser(String wrapperKey, MappingNode mapping, ParsingContextExecution context) {
        IMappingBuilder builder = this.mappingBuilders.get(wrapperKey.substring(2));
        return builder.buildMapping(mapping, context).getParser();
    }

    private void map(NodeTuple tuple, TypeNodeParser<?> parser, ParsingContextExecution context) {
        String key = ParserUtils.getScalar(tuple.getKeyNode(), context);
        // position mapping allows handling of yaml mapping where the key and the value are both actually 'values' of some fields (mixing 2 fields in a single
        // one in yaml).
        int positionMappingIndex = positionMappingIndex(key);
        if (positionMappingIndex > -1) { // if the key is format __x where x is number process a position based mapping (__0 first element of the tupple)
            mapPositionMapping(positionMappingIndex, tuple.getValueNode(), parser, context);
        } else { // if not just process a standard mapping where key is the yaml key.
            MappingTarget mappingTarget = getMappingTarget(tuple.getValueNode(), context);
            if (mappingTarget != null) {
                parser.getYamlToObjectMapping().put(key, mappingTarget);
            }
        }
    }

    private MappingTarget getMappingTarget(Node mappingNode, ParsingContextExecution context) {
        if (mappingNode instanceof ScalarNode) {
            // if the mapping reference a scalar we just map it to a scalar definition.
            String value = ParserUtils.getScalar(mappingNode, context);
            return new MappingTarget(value, parsers.get(ScalarParser.class.getName()));
        } else if (mappingNode instanceof MappingNode) {
            // if this is a mapping node then it can be either a collection or reference to a complex object mapping (through reference parser).
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
                    context.getParsingErrors().add(new ParsingError(ErrorCode.SYNTAX_ERROR, "Unknown key for position mapping.",
                            tuple.getKeyNode().getStartMark(), tupleKey, tuple.getKeyNode().getEndMark(), ""));
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
            context.getParsingErrors().add(new ParsingError(ErrorCode.SYNTAX_ERROR, "Position mapping must be a mapping node with key and value fields.",
                    positionMapping.getStartMark(), "", positionMapping.getEndMark(), ""));
        }
    }

    private MappingTarget mapMappingNode(MappingNode mappingNode, ParsingContextExecution context) {
        String key = ParserUtils.getScalar(mappingNode.getValue().get(0).getKeyNode(), context);
        IMappingBuilder mappingBuilder = mappingBuilders.get(key);
        if (mappingBuilder != null) {
            log.debug("Mapping yaml key <" + key + "> using mapping builder " + mappingBuilder.getClass().getName());
            return mappingBuilder.buildMapping(mappingNode, context);
        }
        context.getParsingErrors().add(new ParsingError(ErrorCode.SYNTAX_ERROR, "No mapping target found for key",
                mappingNode.getValue().get(0).getKeyNode().getStartMark(), key, mappingNode.getValue().get(0).getKeyNode().getEndMark(), ""));
        return null;
    }

}