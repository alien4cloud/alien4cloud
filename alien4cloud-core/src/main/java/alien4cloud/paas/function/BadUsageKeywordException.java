package alien4cloud.paas.function;

import org.alien4cloud.tosca.normative.constants.ToscaFunctionConstants;

/**
 * Exception thrown when using a {@link ToscaFunctionConstants} keyword in a bad way.
 *
 * @author 'Igor Ngouagna'
 *
 */
public class BadUsageKeywordException extends FunctionEvaluationException {

    private static final long serialVersionUID = 1L;

    public BadUsageKeywordException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadUsageKeywordException(String message) {
        super(message);
    }

}
