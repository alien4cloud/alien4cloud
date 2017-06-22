package alien4cloud.tosca.parser.postprocess;

import org.alien4cloud.tosca.model.templates.SubstitutionMapping;
import org.alien4cloud.tosca.model.types.NodeType;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.impl.ErrorCode;

/**
 * Perform post processing of a substitution mapping.
 */
@Component
public class SubstitutionMappingPostProcessor implements IPostProcessor<SubstitutionMapping> {

    private void addError(ErrorCode errorCode, String context, String problem, String note, Node node) {
        if (node != null) {
            ParsingContextExecution.getParsingErrors().add(new ParsingError(errorCode, context, node.getStartMark(), problem, node.getEndMark(), note));
        } else {
            ParsingContextExecution.getParsingErrors().add(new ParsingError(errorCode, context, null, problem, null, note));
        }
    }

    @Override
    public void process(SubstitutionMapping instance) {
        if (instance == null) {
            // no substitution mapping.
            return;
        }
        NodeType substitutionDerivedFrom = ToscaContext.get(NodeType.class, instance.getSubstitutionType());
        Node node = ParsingContextExecution.getObjectToNodeMap().get(instance.getSubstitutionType());
        if (substitutionDerivedFrom == null) {
            addError(ErrorCode.TYPE_NOT_FOUND, "Type not found",
                    "The type from the element is not found neither in the archive or it's dependencies or is not defined while required.",
                    instance.getSubstitutionType(), node);
        } else if (!substitutionDerivedFrom.isAbstract()) {
            addError(ErrorCode.DERIVED_FROM_CONCRETE_TYPE_SUBSTITUTION, "Substitution cannot derive from concrete type",
                    "The substitution derives from " + instance.getSubstitutionType() + " which is not abstract.", instance.getSubstitutionType(), node);
        }
    }
}