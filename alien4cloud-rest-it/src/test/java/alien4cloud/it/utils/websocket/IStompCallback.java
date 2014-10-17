package alien4cloud.it.utils.websocket;

/**
 * @author Minh Khang VU
 */
public interface IStompCallback<T> {

    void onData(String topic, T data);

    void onError(Throwable error);
}
