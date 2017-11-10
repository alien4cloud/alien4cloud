package alien4cloud.tosca.parser.impl.advanced;

import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParsingContextExecution;

import static org.alien4cloud.tosca.normative.constants.ToscaFunctionConstants.R_TARGET;
import static org.alien4cloud.tosca.normative.constants.ToscaFunctionConstants.TARGET;

/**
 * Specific get_attribute parser for 1.4 spec support to allow a transition from 1.3 support of { get_attribute: [TARGET, protocol] } which fetch from the node
 * to 1.4 { get_attribute: [TARGET, protocol] } which fetch from the capability.
 */
@Component
public class GetAttributeParser implements INodeParser<FunctionPropertyValue> {

    @Override
    public FunctionPropertyValue parse(Node node, ParsingContextExecution context) {
        FunctionPropertyValue functionPropertyValue = (FunctionPropertyValue) ParsingContextExecution.get().getRegistry().get("tosca_function").parse(node,
                context);
        if (functionPropertyValue.getParameters().size() > 0 && TARGET.equals(functionPropertyValue.getParameters().get(0))) {
            functionPropertyValue.getParameters().set(0, R_TARGET);
        }
        return functionPropertyValue;
    }
}