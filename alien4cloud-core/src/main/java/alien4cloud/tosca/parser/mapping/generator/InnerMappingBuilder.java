package alien4cloud.tosca.parser.mapping.generator;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.NodeTuple;

import alien4cloud.tosca.parser.MappingTarget;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.parser.impl.base.InnerParser;

/**
 * Build Mapping target for a inner object property.
 */
@Component
public class InnerMappingBuilder implements IMappingBuilder {
    private static final String INNER = "inner";
    private static final String TYPE = "type";
    private static final String DEFERRED = "deferred";
    private static final String DEFERRED_ORDER = "deferredOrder";

    @Override
    public String getKey() {
        return INNER;
    }

    @Override
    public MappingTarget buildMapping(MappingNode mappingNode, ParsingContextExecution context) {
        String inner = null;
        String type = null;
        boolean deferred = false;
        int deferredOrder = 0;
        for (NodeTuple tuple : mappingNode.getValue()) {
            String key = ParserUtils.getScalar(tuple.getKeyNode(), context);
            String value = ParserUtils.getScalar(tuple.getValueNode(), context);
            if (INNER.equals(key)) {
                inner = value;
            } else if (TYPE.equals(key)) {
                type = value;
            } else if (DEFERRED.equals(key)) {
                deferred = Boolean.parseBoolean(value);
            } else if (DEFERRED_ORDER.equals(key)) {
                deferredOrder = Integer.parseInt(value);
            }
        }
        if (type == null || inner == null) {
            context.getParsingErrors().add(
                    new ParsingError(ErrorCode.ALIEN_MAPPING_ERROR, "'type' is required for inner parser", mappingNode.getStartMark(), "", mappingNode
                            .getEndMark(), ""));
        }
        return new MappingTarget(inner, new InnerParser(type, deferred, deferredOrder));
    }
}