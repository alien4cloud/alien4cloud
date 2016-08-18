package alien4cloud.tosca.parser.mapping.generator;

import alien4cloud.tosca.parser.MappingTarget;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.impl.base.ReferencedParser;
import alien4cloud.tosca.parser.impl.base.ValidatedNodeParser;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.NodeTuple;

import javax.annotation.Resource;
import javax.validation.Validator;

/**
 * Build Mapping target for map.
 */
@Component
public class ValidatedMappingBuilder implements IMappingBuilder {
    @Resource
    private Validator validator;

    private static final String VALIDATED = "validated";
    private static final String TYPE = "type";

    @Override
    public String getKey() {
        return VALIDATED;
    }

    @Override
    public MappingTarget buildMapping(MappingNode mappingNode, ParsingContextExecution context) {
        String type = null;

        for (NodeTuple tuple : mappingNode.getValue()) {
            String tupleKey = ParserUtils.getScalar(tuple.getKeyNode(), context);
            if (TYPE.equals(tupleKey)) {
                type = ParserUtils.getScalar(tuple.getValueNode(), context);
            }
        }

        return new MappingTarget("", new ValidatedNodeParser(validator, new ReferencedParser(type)));
    }
}