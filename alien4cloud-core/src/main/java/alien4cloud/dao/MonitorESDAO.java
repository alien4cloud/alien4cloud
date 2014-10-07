package alien4cloud.dao;

import java.beans.IntrospectionException;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import alien4cloud.dao.ESGenericSearchDAO;
import alien4cloud.exception.IndexingServiceException;
import alien4cloud.paas.model.AbstractMonitorEvent;
import alien4cloud.paas.model.PaaSDeploymentStatusMonitorEvent;
import alien4cloud.paas.model.PaaSInstanceStateMonitorEvent;
import alien4cloud.paas.model.PaaSMessageMonitorEvent;
import alien4cloud.tosca.container.model.topology.Topology;
import alien4cloud.tosca.container.serializer.BoundSerializer;
import alien4cloud.utils.JSonMapEntryArraySerializer;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Elastic Search DAO for Monitor events in Alien application.
 *
 * @author luc boutier
 */
@Component("alien-monitor-es-dao")
public class MonitorESDAO extends ESGenericSearchDAO {

    /**
     * Initialize the dao after being loaded by spring (Create the indexes).
     */
    @PostConstruct
    public void initEnvironment() {
        // init ES annotation scanning
        try {
            getMappingBuilder().initialize("alien4cloud");
        } catch (IntrospectionException | IOException e) {
            throw new IndexingServiceException("Could not initialize elastic search mapping builder", e);
        }
        // init indices and mapped classes
        setJsonMapper(new ElasticSearchMapper());
        Class<?>[] classes = new Class<?>[] { AbstractMonitorEvent.class, PaaSDeploymentStatusMonitorEvent.class, PaaSInstanceStateMonitorEvent.class,
                PaaSMessageMonitorEvent.class };
        initIndices("deployedtopologies", false, Topology.class);
        initIndices("deploymentmonitorevents", true, classes);
        initCompleted();
    }

    public static class ElasticSearchMapper extends ObjectMapper {
        private static final long serialVersionUID = 1L;

        public ElasticSearchMapper() {
            super();
            this._serializationConfig = this._serializationConfig.withAttribute(BoundSerializer.BOUND_SERIALIZER_AS_NUMBER, "true");
            this._serializationConfig = this._serializationConfig.withAttribute(JSonMapEntryArraySerializer.MAP_SERIALIZER_AS_ARRAY, "true");
            this._deserializationConfig = this._deserializationConfig.withAttribute(JSonMapEntryArraySerializer.MAP_SERIALIZER_AS_ARRAY, "true");
        }
    }
}
