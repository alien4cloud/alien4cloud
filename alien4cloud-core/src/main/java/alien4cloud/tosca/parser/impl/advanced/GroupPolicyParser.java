package alien4cloud.tosca.parser.impl.advanced;

import java.util.List;
import java.util.Map;
<<<<<<< HEAD
import java.util.Map.Entry;
=======
>>>>>>> e91a4aff682c100d67646de6b5211d100a9215c6

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.common.collect.Maps;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;

import alien4cloud.model.topology.AbstractPolicy;
import alien4cloud.model.topology.HaPolicy;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.parser.impl.base.ScalarParser;
import alien4cloud.tosca.parser.mapping.DefaultParser;

@Component
@Slf4j
public class GroupPolicyParser extends DefaultParser<AbstractPolicy> {

    @Resource
    private ScalarParser scalarParser;

    private static final String NAME = "name";

    private static final String TYPE = "type";

    @Override
    public AbstractPolicy parse(Node node, ParsingContextExecution context) {
        if (!(node instanceof MappingNode)) {
            // we expect a MappingNode
            context.getParsingErrors().add(new ParsingError(ErrorCode.YAML_MAPPING_NODE_EXPECTED, null, node.getStartMark(), null, node.getEndMark(), null));
            return null;
        }
        MappingNode mappingNode = ((MappingNode) node);
        List<NodeTuple> children = mappingNode.getValue();
        int tupleIdx = 0;
        Map<String, String> nodeMap = Maps.newHashMap();
        for (NodeTuple child : children) {
            String key = scalarParser.parse(child.getKeyNode(), context);
            String value = scalarParser.parse(child.getValueNode(), context);
            if (tupleIdx == 0 && StringUtils.isEmpty(value)) {
                // the first entry is in fact the policyName
                nodeMap.put(NAME, key);
            } else {
                nodeMap.put(key, value);
            }
        }
        if (nodeMap.size() == 1 && !nodeMap.containsKey(NAME) && !nodeMap.containsKey(TYPE)) {
            // short notation : 'name': 'type'
            Entry<String, String> e = nodeMap.entrySet().iterator().next();
            nodeMap.clear();
            nodeMap.put(NAME, e.getKey());
            nodeMap.put(TYPE, e.getValue());
        }
        return buildPolicy(nodeMap, node, context);
    }

    private AbstractPolicy buildPolicy(Map<String, String> nodeMap, Node node, ParsingContextExecution context) {
        String type = nodeMap.get(TYPE);
        AbstractPolicy result = null;
        switch (type) {
        case HaPolicy.HA_POLICY:
            result = new HaPolicy();
            break;
        }
        if (result == null) {
            context.getParsingErrors().add(
                    new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.UNKOWN_GROUP_POLICY, null, node.getStartMark(), null, node.getEndMark(), type));
            return null;
        }
        result.setName(nodeMap.get(NAME));
        return result;
    }

}