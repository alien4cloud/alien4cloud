package alien4cloud.tool.compilation.exception;

import alien4cloud.exception.TechnicalException;

public class CompilationToolRuntimeException extends TechnicalException {

    private static final long serialVersionUID = -5519512669160805334L;

    public CompilationToolRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public CompilationToolRuntimeException(String message) {
        super(message);
    }

}
