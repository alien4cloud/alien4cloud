package alien4cloud.paas.plan;

import java.util.Date;
import java.util.List;
import java.util.Map;

import lombok.Getter;

import org.springframework.stereotype.Component;

import alien4cloud.paas.AbstractPaaSProvider;
import alien4cloud.paas.IConfigurablePaaSProvider;
import alien4cloud.paas.exception.OperationExecutionException;
import alien4cloud.paas.exception.PluginConfigurationException;
import alien4cloud.paas.model.AbstractMonitorEvent;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.paas.model.InstanceInformation;
import alien4cloud.paas.model.NodeOperationExecRequest;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.tosca.container.model.topology.Topology;
import alien4cloud.tosca.model.PropertyDefinition;

@Getter
@Component
public class MockPaaSProvider extends AbstractPaaSProvider implements IConfigurablePaaSProvider<String> {
    private List<PaaSNodeTemplate> roots;

    @Override
    public void undeploy(String deploymentId) {
    }

    @Override
    public void scale(String applicationId, String nodeTemplateId, int instances) {
    }

    @Override
    public DeploymentStatus getStatus(String deploymentId) {
        return null;
    }

    @Override
    public DeploymentStatus[] getStatuses(String[] deploymentIds) {
        return null;
    }

    @Override
    public Map<String, Map<Integer, InstanceInformation>> getInstancesInformation(String deploymentId, Topology topology) {
        return null;
    }

    @Override
    public AbstractMonitorEvent[] getEventsSince(Date date, int maxEvents) {
        return null;
    }

    @Override
    protected void doDeploy(String deploymentName, String deploymentId, Topology topology, List<PaaSNodeTemplate> roots,
            Map<String, PaaSNodeTemplate> nodeTemplates) {
        this.roots = roots;
    }

    @Override
    public Map<String, PropertyDefinition> getDeploymentPropertyMap() {
        return null;
    }

    @Override
    public String getDefaultConfiguration() {
        return null;
    }

    @Override
    public void setConfiguration(String configuration) throws PluginConfigurationException {
    }

    @Override
    public Map<String, String> executeOperation(String deploymentId, NodeOperationExecRequest request) throws OperationExecutionException {
        return null;
    }

}