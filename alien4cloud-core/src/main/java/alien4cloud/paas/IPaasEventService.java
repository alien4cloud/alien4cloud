package alien4cloud.paas;


/**
 * The service to provide event based delivery of Paas event
 */
public interface IPaasEventService {

    void addListener(IPaasEventListener<?> listener);

    void removeListener(IPaasEventListener<?> listener);
}
