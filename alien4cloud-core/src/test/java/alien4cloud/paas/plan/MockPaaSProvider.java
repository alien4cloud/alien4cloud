package alien4cloud.paas.plan;

import java.util.Date;
import java.util.List;
import java.util.Map;

import lombok.Getter;

import org.springframework.stereotype.Component;

import alien4cloud.paas.IConfigurablePaaSProvider;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.paas.IPaaSProvider;
import alien4cloud.paas.exception.OperationExecutionException;
import alien4cloud.paas.exception.PluginConfigurationException;
import alien4cloud.paas.model.AbstractMonitorEvent;
import alien4cloud.paas.model.NodeOperationExecRequest;
import alien4cloud.paas.model.PaaSDeploymentContext;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSTopologyDeploymentContext;
import alien4cloud.tosca.model.PropertyDefinition;

@Getter
@Component
public class MockPaaSProvider implements IConfigurablePaaSProvider<String>, IPaaSProvider {

    private List<PaaSNodeTemplate> roots;

    @Override
    public String getDefaultConfiguration() {
        return null;
    }

    @Override
    public void setConfiguration(String configuration) throws PluginConfigurationException {

    }

    @Override
    public void deploy(PaaSTopologyDeploymentContext deploymentContext, IPaaSCallback<?> callback) {
        this.roots = deploymentContext.getPaaSTopology().getComputes();
    }

    @Override
    public void undeploy(PaaSDeploymentContext deploymentContext, IPaaSCallback<?> callback) {

    }

    @Override
    public void scale(PaaSDeploymentContext deploymentContext, String nodeTemplateId, int instances, IPaaSCallback<?> callback) {

    }

    @Override
    public void getEventsSince(Date date, int maxEvents, IPaaSCallback<AbstractMonitorEvent[]> eventCallback) {

    }

    @Override
    public void executeOperation(PaaSDeploymentContext deploymentContext, NodeOperationExecRequest request,
            IPaaSCallback<Map<String, String>> operationResultCallback) throws OperationExecutionException {

    }

    @Override
    public Map<String, PropertyDefinition> getDeploymentPropertyMap() {
        return null;
    }
}