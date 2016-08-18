package alien4cloud.tosca.parser.impl.advanced;

import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.parser.impl.base.MapParser;
import alien4cloud.tosca.parser.mapping.DefaultDeferredParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import java.util.Map;
import java.util.Map.Entry;

@Component
@Slf4j
public class NodeTemplateCapabilitiesParser extends DefaultDeferredParser<Void> {

    @Override
    public Void parse(Node node, ParsingContextExecution context) {
        Object parent = context.getParent();
        if (!(parent instanceof NodeTemplate)) {
            // TODO: throw ex
            return null;
        }
        NodeTemplate nodeTemplate = (NodeTemplate) parent;
        Map<String, Capability> capabilities = nodeTemplate.getCapabilities();

        if (!(node instanceof MappingNode)) {
            // we expect a SequenceNode
            context.getParsingErrors().add(new ParsingError(ErrorCode.YAML_MAPPING_NODE_EXPECTED, null, node.getStartMark(), null, node.getEndMark(), null));
            return null;
        }
        MappingNode mappingNode = ((MappingNode) node);
        for (NodeTuple nodeTuple : mappingNode.getValue()) {
            // first of all, we need to get the key (the capability name)
            Node keyNode = nodeTuple.getKeyNode();
            if (!(keyNode instanceof ScalarNode)) {
                context.getParsingErrors().add(
                        new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.YAML_SCALAR_NODE_EXPECTED, null, keyNode.getStartMark(), null, keyNode
                                .getEndMark(), null));
                continue;
            }
            String key = ((ScalarNode) keyNode).getValue();
            // can we find the corresponding capability ?
            Capability capability;
            if (capabilities == null || (capability = capabilities.get(key)) == null) {
                // add a warning, we will ignore this property since it does not fit to an existing capa
                context.getParsingErrors()
                        .add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.UNKNOWN_CAPABILITY, null, keyNode.getStartMark(), null,
                                keyNode.getEndMark(), key));
                continue;
            }
            Map<String, AbstractPropertyValue> capabilitiesProperties = capability.getProperties();
            if (capabilitiesProperties == null || capabilitiesProperties.isEmpty()) {
                // the capability has now properties, this means the capability type doesn't define props.
                // not necessary to continue.
                continue;
            }
            // now look for a 'properties' node
            Node valueNode = nodeTuple.getValueNode();
            if (!(valueNode instanceof MappingNode)) {
                continue;
            }
            for (NodeTuple childNodeTuple : ((MappingNode) valueNode).getValue()) {
                Node childKeyNode = childNodeTuple.getKeyNode();
                if (childKeyNode instanceof ScalarNode && ((ScalarNode) childKeyNode).getValue().equals("properties")) {
                    Node propertiesValueNode = childNodeTuple.getValueNode();

                    // parse the 'properties'
                    INodeParser<AbstractPropertyValue> propertyValueParser = context.getRegistry().get("node_template_property");
                    MapParser<AbstractPropertyValue> mapParser = new MapParser<AbstractPropertyValue>(propertyValueParser, "node_template_property");
                    Map<String, AbstractPropertyValue> parsedCapabilitiesProperties = mapParser.parse(propertiesValueNode, context);

                    // now merge the capability properties
                    for (Entry<String, AbstractPropertyValue> legacyEntry : capabilitiesProperties.entrySet()) {
                        String legacyKey = legacyEntry.getKey();
                        AbstractPropertyValue v = parsedCapabilitiesProperties.remove(legacyKey);
                        if (v != null) {
                            legacyEntry.setValue(v);
                        }
                    }
                    // now just iterate over remaining parsed entry to raise warns
                    for (String notFoundKey : parsedCapabilitiesProperties.keySet()) {
                        // add warning
                        context.getParsingErrors().add(
                                new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.UNRECOGNIZED_PROPERTY, null, propertiesValueNode.getStartMark(), null,
                                        propertiesValueNode.getEndMark(), notFoundKey));
                    }
                }
            }

        }
        return null;
    }

}