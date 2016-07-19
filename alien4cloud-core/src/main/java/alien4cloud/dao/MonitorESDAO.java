package alien4cloud.dao;

import java.beans.IntrospectionException;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import alien4cloud.exception.IndexingServiceException;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.paas.model.AbstractMonitorEvent;
import alien4cloud.paas.model.PaaSDeploymentLog;
import alien4cloud.paas.model.PaaSDeploymentStatusMonitorEvent;
import alien4cloud.paas.model.PaaSInstancePersistentResourceMonitorEvent;
import alien4cloud.paas.model.PaaSInstanceStateMonitorEvent;
import alien4cloud.paas.model.PaaSMessageMonitorEvent;
import alien4cloud.paas.model.PaaSWorkflowMonitorEvent;
import alien4cloud.paas.model.PaaSWorkflowStepMonitorEvent;

/**
 * Elastic Search DAO for Monitor events in Alien application.
 *
 * @author luc boutier
 */
@Component("alien-monitor-es-dao")
public class MonitorESDAO extends ESGenericSearchDAO {

    @Value("${paas_monitor.events_lifetime}")
    private String eventMonitoringTtl;

    /**
     * Initialize the dao after being loaded by spring (Create the indexes).
     */
    @PostConstruct
    public void initEnvironment() {
        // init ES annotation scanning
        try {
            getMappingBuilder().initialize("alien4cloud.paas.model");
        } catch (IntrospectionException | IOException e) {
            throw new IndexingServiceException("Could not initialize elastic search mapping builder", e);
        }
        // init indices and mapped classes
        setJsonMapper(ElasticSearchMapper.getInstance());

        Class<?>[] classes = new Class<?>[] { AbstractMonitorEvent.class, PaaSDeploymentStatusMonitorEvent.class, PaaSInstanceStateMonitorEvent.class,
                PaaSMessageMonitorEvent.class, PaaSInstancePersistentResourceMonitorEvent.class, PaaSWorkflowStepMonitorEvent.class,
                PaaSWorkflowMonitorEvent.class };
        initIndices("deployedtopologies", null, DeploymentTopology.class);
        initIndices("deploymentmonitorevents", eventMonitoringTtl, classes);
        initIndices(PaaSDeploymentLog.class.getSimpleName().toLowerCase(), eventMonitoringTtl, PaaSDeploymentLog.class);
        initCompleted();
    }

}