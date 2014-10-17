package alien4cloud.it.utils.websocket;

/**
 * @author Minh Khang VU
 */
public class StompErrorException extends RuntimeException {

    public StompErrorException(String message) {
        super(message);
    }
}
