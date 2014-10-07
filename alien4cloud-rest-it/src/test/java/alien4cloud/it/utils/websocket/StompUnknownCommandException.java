package alien4cloud.it.utils.websocket;

/**
 * @author Minh Khang VU
 */
public class StompUnknownCommandException extends RuntimeException {

    public StompUnknownCommandException(String message) {
        super(message);
    }
}
