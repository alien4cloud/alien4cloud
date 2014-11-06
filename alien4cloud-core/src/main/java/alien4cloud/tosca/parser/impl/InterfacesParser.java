package alien4cloud.tosca.parser.impl;

import java.util.Map;

import org.elasticsearch.common.collect.Maps;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

import alien4cloud.tosca.container.model.type.Interface;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.MapParser;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;

public class InterfacesParser extends MapParser<Interface> {
    public InterfacesParser(INodeParser<Interface> valueParser, String toscaType) {
        super(valueParser, toscaType);
    }

    @Override
    public Map<String, Interface> parse(Node node, ParsingContextExecution context) {
        if (node instanceof MappingNode) {
            return super.parse(node, context);
        }
        // Specific for interfaces node can define or only reference interfaces
        Map<String, Interface> interfaces = Maps.newHashMap();
        if (node instanceof SequenceNode) {
            for (Node interfaceTypeNode : ((SequenceNode) node).getValue()) {
                if (interfaceTypeNode instanceof ScalarNode) {
                    addInterfaceFromType((ScalarNode) interfaceTypeNode, interfaces, context);
                } else {
                    ParserUtils.addTypeError(interfaceTypeNode, context.getParsingErrors(), "interface");
                }
            }
        } else if (node instanceof ScalarNode) {
            addInterfaceFromType((ScalarNode) node, interfaces, context);
        } else {
            // add an error
            ParserUtils.addTypeError(node, context.getParsingErrors(), "interface");
        }
        return interfaces;
    }

    private void addInterfaceFromType(ScalarNode node, Map<String, Interface> interfaces, ParsingContextExecution context) {
        String interfaceType = ((ScalarNode) node).getValue();
        // TODO type validation
        interfaces.put(interfaceType, new Interface());
    }
}