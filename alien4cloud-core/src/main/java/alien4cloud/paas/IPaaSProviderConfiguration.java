package alien4cloud.paas;

/**
 * When a provider configuration implements this interface, the orchestrator name can be used by the plugin.
 */
public interface IPaaSProviderConfiguration {

    void setOrchestratorName(String name);
    String getOrchestratorName();
    void setOrchestratorId(String id);
    String getOrchestratorId();

}
