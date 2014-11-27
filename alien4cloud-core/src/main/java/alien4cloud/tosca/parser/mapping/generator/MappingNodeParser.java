package alien4cloud.tosca.parser.mapping.generator;

import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.MappingTarget;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.impl.base.*;

import com.google.common.collect.Maps;

/**
 * Parse a yaml definition node to create an INodeParser.
 */
public class MappingNodeParser implements INodeParser<TypeNodeParser<?>> {
    private static final String TOSCA_TYPE_KEY = "yaml_type";
    private static final String CLASS_NAME_KEY = "java_type";
    private static final String POSITION_MAP_KEY = "position_mapping";
    private static final String NAMED_MAP_KEY = "named_mapping";

    private static final ScalarParser SCALAR_PARSER = new ScalarParser();

    @Override
    public TypeNodeParser<?> parse(Node node, ParsingContextExecution context) {
        if (node instanceof MappingNode) {
            try {
                return doParse((MappingNode) node, context);
            } catch (ClassNotFoundException e) {
                throw new MappingGenerationException("Failed to load one of the mapped class.", e);
            }
        }
        throw new MappingGenerationException("Mapping must be a mapping node.");
    }

    private TypeNodeParser<?> doParse(MappingNode mappingNode, ParsingContextExecution context) throws ClassNotFoundException {
        Map<String, Node> nodeMap = mappingNodeAsMap(mappingNode);

        String typeName = ParserUtils.getScalar(nodeMap.remove(TOSCA_TYPE_KEY), context.getParsingErrors());
        String className = ParserUtils.getScalar(nodeMap.remove(CLASS_NAME_KEY), context.getParsingErrors());

        Class<?> targetClass = Class.forName(className);
        TypeNodeParser<?> nodeParser = new TypeNodeParser<>(targetClass, typeName);

        for (Map.Entry<String, Node> entry : nodeMap.entrySet()) {
            processFieldMapping(entry.getKey(), entry.getValue(), nodeParser.getYamlToObjectMapping(), nodeParser.getYamlOrderedToObjectMapping(), context);
        }
        return nodeParser;
    }

    private void processFieldMapping(String keyStr, Node fields, Map<String, MappingTarget> yamlToObjectMapping,
            Map<Integer, MappingTarget> yamlOrderedToObjectMapping, ParsingContextExecution context) {
        if (fields instanceof MappingNode) {
            for (NodeTuple field : ((MappingNode) fields).getValue()) {
                if (POSITION_MAP_KEY.equals(keyStr)) {
                    processPositionFieldMapping(field, yamlOrderedToObjectMapping, context);
                } else if (NAMED_MAP_KEY.equals(keyStr)) {
                    processNamedFieldMapping(field, yamlToObjectMapping, context);
                }
            }
        } else if (!(fields instanceof ScalarNode && ((ScalarNode) fields).getValue().isEmpty())) {
            throw new MappingGenerationException("Fields mapping definitions must be a mapping node.");
        }
    }

    private void processPositionFieldMapping(NodeTuple field, Map<Integer, MappingTarget> yamlOrderedToObjectMapping, ParsingContextExecution context) {
        Integer position = Integer.valueOf(((ScalarNode) field.getKeyNode()).getValue());
        yamlOrderedToObjectMapping.put(position, processMapping(field.getValueNode(), context));
    }

    private void processNamedFieldMapping(NodeTuple field, Map<String, MappingTarget> yamlToObjectMapping, ParsingContextExecution context) {
        String fieldName = ((ScalarNode) field.getKeyNode()).getValue();
        yamlToObjectMapping.put(fieldName, processMapping(field.getValueNode(), context));
    }

    private MappingTarget processMapping(Node mapping, ParsingContextExecution context) {
        if (mapping instanceof ScalarNode) {
            // create a scalar mapping
            String value = ParserUtils.getScalar(mapping, context.getParsingErrors());
            return new MappingTarget(value, SCALAR_PARSER);
        } else if (mapping instanceof MappingNode) {
            // yaml type is either a complex type or a map.
            List<NodeTuple> mappings = ((MappingNode) mapping).getValue();
            if (mappings.size() > 1) {
                // TODO type reference mapping
            } else if (mappings.size() == 1) {
                // collection mapping
                String collectionKey = ParserUtils.getScalar(mappings.get(0).getKeyNode(), context.getParsingErrors());
                Map<String, Node> collectionNode = mappingNodeAsMap((MappingNode) mappings.get(0).getValueNode());
                return getCollectionMapping(collectionKey, collectionNode, context);
            }
        }
        throw new MappingGenerationException("Yaml mapping node must be an instance of ScalarNode, A type mapping or a collection mapping");
    }

    private MappingTarget getCollectionMapping(String collectionKey, Map<String, Node> collectionNode, ParsingContextExecution context) {
        String mapping = ParserUtils.getScalar(collectionNode.get("mapping"), context.getParsingErrors());
        String type = ParserUtils.getScalar(collectionNode.get("type"), context.getParsingErrors());
        String key = null;
        Node keyNode = collectionNode.get("key");
        if (keyNode != null) {
            key = ParserUtils.getScalar(collectionNode.get("key"), context.getParsingErrors());
        }

        ReferencedParser referencedParser = new ReferencedParser(type);

        switch (collectionKey) {
        case "map":
            MapParser mapParser = new MapParser(referencedParser, "mapping", key);
            return new MappingTarget(mapping, mapParser);
        case "set":
            SetParser setParser = new SetParser(referencedParser, "sequence", key);
            return new MappingTarget(mapping, setParser);
        case "list":
            ListParser listParser = new ListParser(referencedParser, "sequence", key);
            return new MappingTarget(mapping, listParser);
        }
        throw new MappingGenerationException("Yaml mapping node must be an instance of ScalarNode, A type mapping or a collection mapping");
    }

    private Map<String, Node> mappingNodeAsMap(MappingNode mappingNode) {
        Map<String, Node> nodeMap = Maps.newHashMap();
        for (NodeTuple mapping : mappingNode.getValue()) {
            String keyStr = ((ScalarNode) mapping.getKeyNode()).getValue();
            nodeMap.put(keyStr, mapping.getValueNode());
        }
        return nodeMap;
    }

    @Override
    public boolean isDeferred(ParsingContextExecution context) {
        return false;
    }
}