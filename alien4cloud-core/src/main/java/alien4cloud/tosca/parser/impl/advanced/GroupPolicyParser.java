package alien4cloud.tosca.parser.impl.advanced;

import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;

import org.elasticsearch.common.collect.Maps;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;

import alien4cloud.model.topology.AbstractPolicy;
import alien4cloud.model.topology.GenericPolicy;
import alien4cloud.model.topology.HaPolicy;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.parser.impl.base.ScalarParser;
import alien4cloud.tosca.parser.mapping.DefaultParser;

@Component
public class GroupPolicyParser extends DefaultParser<AbstractPolicy> {

    @Resource
    private ScalarParser scalarParser;

    public static final String NAME = "name";
    public static final String TYPE = "type";
    public static final String VALUE = "value";
    
    private static final Map<String,Class<? extends AbstractPolicy>> POLICY_TYPES = Maps.newLinkedHashMap();
    
    static {
        POLICY_TYPES.put(HaPolicy.HA_POLICY, HaPolicy.class);
    }

    @Override
    public AbstractPolicy parse(Node node, ParsingContextExecution context) {
        if (node instanceof ScalarNode) {
            // Spec at A.8.1.5.1 says it is a "list of names of policies"
            // though the examples treat it as maps, and in some cases it seems the type might be specified;
            // accept all syntaxes for now
            Map<String, Object> nodeMap = Maps.newHashMap();
            String name = scalarParser.parse(node, context);
            if (POLICY_TYPES.containsKey(name)) {
                nodeMap.put(TYPE, name);
            } else {
                nodeMap.put(NAME, name);
            }
            return buildPolicy(nodeMap, node, context);
        }
        
        if (!(node instanceof MappingNode)) {
            // we expect a MappingNode
            context.getParsingErrors().add(new ParsingError(ErrorCode.YAML_MAPPING_NODE_EXPECTED, null, node.getStartMark(), null, node.getEndMark(), null));
            return null;
        }
        Map<String, Object> nodeMap = ParserUtils.parseMap((MappingNode) node);

        String name = (String) nodeMap.get(NAME);
        String type = (String) nodeMap.get(TYPE);
        if (nodeMap.size() == 1 && name==null && type==null) {
            // short notation '<key>: <value>' where (in priority order) 
            // - <value> is a map and <key> matches a known pre-defined type, then <value> is a map of data passed to the type
            // - <value> matches a known pre-defined type, then <key> is taken as a name
            // - else taken as a generic policy with name <key>, with <value> set as the map (if it's a map) or as a `value` in the map (if it's not a map)
            Entry<String, Object> e = nodeMap.entrySet().iterator().next();

            nodeMap.clear();
            if (e.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> v = (Map<String,Object>)e.getValue();
                nodeMap.putAll(v);
                if (POLICY_TYPES.containsKey(e.getKey())) {
                    nodeMap.put(TYPE, e.getKey());
                } else {
                    nodeMap.put(NAME, e.getKey());
                }
            } else if (e.getValue() instanceof CharSequence) {
                if (POLICY_TYPES.containsKey(e.getValue())) {
                    nodeMap.put(TYPE, e.getValue().toString());
                    nodeMap.put(NAME, e.getKey());
                } else {
                    nodeMap.put(NAME, e.getKey());
                    nodeMap.put(VALUE, e.getValue().toString());
                }
            }
        }

        return buildPolicy(nodeMap, node, context);
    }

    private AbstractPolicy buildPolicy(Map<String, Object> nodeMap, Node node, ParsingContextExecution context) {
        String type = (String) nodeMap.get(TYPE);
        AbstractPolicy result = null;
        if (type!=null) {
            switch (type) {
            case HaPolicy.HA_POLICY:
                result = new HaPolicy(nodeMap);
                break;
            }
        }
        if (result==null) {
            result = new GenericPolicy(nodeMap);
        }
        return result;
    }

}