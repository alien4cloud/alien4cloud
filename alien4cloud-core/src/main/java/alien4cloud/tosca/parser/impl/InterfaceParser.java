package alien4cloud.tosca.parser.impl;

import java.util.Map;

import org.springframework.data.annotation.Reference;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.tosca.container.model.type.Interface;
import alien4cloud.tosca.container.model.type.Operation;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.MapParser;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.mapping.Wd03OperationDefinition;

@Component
public class InterfaceParser implements INodeParser<Interface> {
    @Reference
    private Wd03OperationDefinition operationDefinition;

    @Override
    public Interface parse(Node node, ParsingContextExecution context) {
        MapParser<Operation> operationParser = new MapParser<Operation>(operationDefinition.getParser(), "operations");
        Map<String, Operation> operations = operationParser.parse(node, context);
        Interface interfaz = new Interface();
        interfaz.setOperations(operations);
        return interfaz;
    }

    @Override
    public boolean isDeffered() {
        return false;
    }
}