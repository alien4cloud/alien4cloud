package alien4cloud.tosca.parser;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.yaml.snakeyaml.nodes.Node;

public class ValidatedTypeNodeParser<T> extends TypeNodeParser<T> {
    private final Validator validator;

    public ValidatedTypeNodeParser(Validator validator, Class<T> type, String toscaType) {
        super(type, toscaType);
        this.validator = validator;
    }

    @Override
    public T parse(Node node, ParsingContext context) {
        T parsed = super.parse(node, context);
        // perform validation of the parsed node
        Set<ConstraintViolation<T>> violations = validator.validate(parsed);
        for (ConstraintViolation<T> violation : violations) {
            StringBuilder buffer = new StringBuilder("Error while validating type [");
            buffer.append(violation.getLeafBean().getClass().getName()).append("], Path [");
            buffer.append(violation.getPropertyPath().toString()).append("] : ");
            buffer.append(violation.getMessage());

            String errorCode = violation.getConstraintDescriptor() != null ? violation.getConstraintDescriptor().getAnnotation().annotationType()
                    .getSimpleName() : "";

            context.getParsingErrors().add(new ToscaParsingError(null, errorCode, node.getStartMark(), buffer.toString(), node.getEndMark(), null));
        }

        return parsed;
    }
}