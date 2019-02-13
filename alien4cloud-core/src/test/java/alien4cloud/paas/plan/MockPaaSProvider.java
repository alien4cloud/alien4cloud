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
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.paas.model.InstanceInformation;
import alien4cloud.paas.model.NodeOperationExecRequest;
import alien4cloud.paas.model.PaaSDeploymentContext;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSTopologyDeploymentContext;

@Getter
@Component
public class MockPaaSProvider implements IPaaSProvider, IConfigurablePaaSProvider<String> {

    private List<PaaSNodeTemplate> roots;

    @Override
    public void setConfiguration(String orchestratorId, String configuration) throws PluginConfigurationException {
    }

    @Override
    public void init(Map<String, PaaSTopologyDeploymentContext> activeDeployments) {

    }

    @Override
    public void deploy(PaaSTopologyDeploymentContext deploymentContext, IPaaSCallback<?> callback) {
        this.roots = deploymentContext.getPaaSTopology().getComputes();
    }

    @Override
    public void update(PaaSTopologyDeploymentContext deploymentContext, IPaaSCallback<?> callback) {

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
    public void executeOperation(PaaSTopologyDeploymentContext deploymentContext, NodeOperationExecRequest request,
            IPaaSCallback<Map<String, String>> operationResultCallback) throws OperationExecutionException {

    }

    @Override
    public void getStatus(PaaSDeploymentContext deploymentContext, IPaaSCallback<DeploymentStatus> callback) {

    }

    @Override
    public void getInstancesInformation(PaaSTopologyDeploymentContext deploymentContext,
            IPaaSCallback<Map<String, Map<String, InstanceInformation>>> callback) {

    }

    @Override
    public void switchMaintenanceMode(PaaSDeploymentContext deploymentContext, boolean maintenanceModeOn) {

    }

    @Override
    public void switchInstanceMaintenanceMode(PaaSDeploymentContext deploymentContext, String nodeId, String instanceId, boolean maintenanceModeOn) {

    }

    @Override
    public void launchWorkflow(PaaSDeploymentContext deploymentContext, String workflowName, Map<String, Object> inputs, IPaaSCallback<String> callback) {
    }

}