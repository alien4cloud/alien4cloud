package alien4cloud.tosca.parser.mapping.generator;

import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.NodeTuple;

import com.google.common.collect.Maps;

import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.MappingTarget;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.impl.base.BaseParserFactory;

/**
 * Build Mapping target for map.
 */
@Component
public class DiscriminatorMappingBuilder implements IMappingBuilder {
    private static final String DISCRIMINATOR = "discriminator";
    private static final String KEYS = "keys";
    private static final String DEFAULT = "default";

    @Resource
    private BaseParserFactory baseParserFactory;

    @Override
    public String getKey() {
        return DISCRIMINATOR;
    }

    @Override
    public MappingTarget buildMapping(MappingNode mappingNode, ParsingContextExecution context) {
        String mappingTarget = "";
        Map<String, INodeParser<?>> parsersByKey = Maps.newHashMap();
        INodeParser<?> fallbackParser = null;

        for (NodeTuple tuple : mappingNode.getValue()) {
            String tupleKey = ParserUtils.getScalar(tuple.getKeyNode(), context);
            if (DISCRIMINATOR.equals(tupleKey)) {
                mappingTarget = ParserUtils.getScalar(tuple.getValueNode(), context);
            } else if (KEYS.equals(tupleKey)) {
                MappingNode keys = (MappingNode) tuple.getValueNode();
                for (NodeTuple keyMappingTuple : keys.getValue()) {
                    parsersByKey.put(ParserUtils.getScalar(keyMappingTuple.getKeyNode(), context),
                            baseParserFactory.getReferencedParser(ParserUtils.getScalar(keyMappingTuple.getValueNode(), context)));
                }
            } else if (DEFAULT.equals(tupleKey)) {
                fallbackParser = baseParserFactory.getReferencedParser(ParserUtils.getScalar(tuple.getValueNode(), context));
            }
        }

        return new MappingTarget(mappingTarget, baseParserFactory.getKeyDiscriminatorParser(parsersByKey, fallbackParser));
    }
}