package alien4cloud.it.utils.websocket;

import java.util.concurrent.TimeUnit;

/**
 * @author Minh Khang VU
 */
public interface IStompDataFuture<T> {

    StompData<T>[] getData(int minimumNumberOfData, long timeout, TimeUnit timeUnit);

}
