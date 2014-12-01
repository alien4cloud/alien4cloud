package alien4cloud.it.utils.websocket;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * A convenient simple callback to retrieve data from stomp connection without having to manage concurrency
 * 
 * @author Minh Khang VU
 */
@Slf4j
public class StompCallback<T> implements IStompCallback<T>, IStompDataFuture<T> {

    private ReentrantLock finishLock;

    private Condition finishCondition;

    private List<StompData> dataList;

    private AtomicReference<Throwable> exception;

    private AtomicInteger dataToBeRetrieved;

    private Class<T> dataType;

    public StompCallback(Class<T> dataType) {
        this.exception = new AtomicReference<>();
        this.finishLock = new ReentrantLock();
        this.finishCondition = this.finishLock.newCondition();
        this.dataList = new ArrayList<>();
        this.dataType = dataType;
    }

    @Override
    public Class<T> getExpectedDataType() {
        return this.dataType;
    }

    @Override
    public void onData(String topic, T data) {
        try {
            finishLock.lock();
            dataList.add(new StompData(topic, data));
            if (log.isDebugEnabled()) {
                log.debug("Received {}th data with content {}", dataList.size(), data);
            }
            if (dataToBeRetrieved != null && dataList.size() >= dataToBeRetrieved.get()) {
                finishCondition.signal();
            }
        } finally {
            finishLock.unlock();
        }
    }

    @Override
    public void onError(Throwable error) {
        try {
            finishLock.lock();
            exception.set(error);
            finishCondition.signal();
        } finally {
            finishLock.unlock();
        }
    }

    @SneakyThrows(Throwable.class)
    public StompData<T>[] getData(int minimumNumberOfData, long timeout, TimeUnit timeUnit) {
        try {
            this.finishLock.lock();
            if (this.dataToBeRetrieved != null) {
                throw new IllegalStateException("Cannot handle concurrent consumers");
            }
            if (this.exception.get() != null) {
                throw this.exception.get();
            }
            this.dataToBeRetrieved = new AtomicInteger(minimumNumberOfData);
            if (this.dataList.size() < this.dataToBeRetrieved.get()) {
                this.finishCondition.await(timeout, timeUnit);
            }
            if (this.exception.get() != null) {
                throw this.exception.get();
            }
            return this.dataList.toArray(new StompData[this.dataList.size()]);
        } finally {
            this.dataList.clear();
            this.dataToBeRetrieved = null;
            this.finishLock.unlock();
        }
    }
}
