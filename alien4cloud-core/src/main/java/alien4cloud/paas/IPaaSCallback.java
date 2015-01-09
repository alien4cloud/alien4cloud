package alien4cloud.paas;

/**
 * This interface defines callback for PaaS asynchronous API
 */
public interface IPaaSCallback<T> {

    void onSuccess(T data);

    void onFailure(Throwable throwable);
}
