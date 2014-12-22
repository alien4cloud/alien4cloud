package alien4cloud.tosca.parser.impl.base;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.impl.ErrorCode;

/**
 * Wraps an {@link INodeParser} and performs validation (using javax validation) on the given node. All validation errors are added to the parsing result.
 * 
 * @param <T> The node returned by the parsing.
 */
public class ValidatedNodeParser<T> implements INodeParser<T> {
    private final INodeParser<T> delegate;
    private final Validator validator;

    public ValidatedNodeParser(Validator validator, INodeParser<T> delegate) {
        this.validator = validator;
        this.delegate = delegate;
    }

    @Override
    public T parse(Node node, ParsingContextExecution context) {
        T parsed = delegate.parse(node, context);
        // perform validation of the parsed node
        Set<ConstraintViolation<T>> violations = validator.validate(parsed);
        for (ConstraintViolation<T> violation : violations) {
            StringBuilder buffer = new StringBuilder("Error while validating type [");
            buffer.append(violation.getLeafBean().getClass().getName()).append("], Path [");
            buffer.append(violation.getPropertyPath().toString()).append("] : ");
            buffer.append(violation.getMessage());

            String errorCode = violation.getConstraintDescriptor() != null ? violation.getConstraintDescriptor().getAnnotation().annotationType()
                    .getSimpleName() : "";

            context.getParsingErrors().add(
                    new ParsingError(ErrorCode.VALIDATION_ERROR, errorCode, node.getStartMark(), buffer.toString(), node.getEndMark(), violation
                            .getPropertyPath().toString()));
        }

        return parsed;
    }

    @Override
    public boolean isDeferred(ParsingContextExecution context) {
        return delegate.isDeferred(context);
    }
}