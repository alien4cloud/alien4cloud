package alien4cloud.var;

public class TokenizerException extends Exception {

    private static final long serialVersionUID = 7687343741189460788L;

    public TokenizerException() {
    }

    public TokenizerException(String message) {
        super(message);
    }

    public TokenizerException(String message, Throwable cause) {
        super(message, cause);
    }

    public TokenizerException(Throwable cause) {
        super(cause);
    }
}
