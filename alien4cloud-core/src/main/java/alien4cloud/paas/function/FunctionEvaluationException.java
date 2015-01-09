package alien4cloud.paas.function;

import alien4cloud.paas.exception.PaaSTechnicalException;

public class FunctionEvaluationException extends PaaSTechnicalException {

    private static final long serialVersionUID = 1L;

    public FunctionEvaluationException(String message, Throwable cause) {
        super(message, cause);
    }

    public FunctionEvaluationException(String message) {
        super(message);
    }

}
