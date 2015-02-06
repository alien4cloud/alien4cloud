package alien4cloud.dao;

import java.beans.IntrospectionException;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import alien4cloud.exception.IndexingServiceException;
import alien4cloud.json.serializer.BoundSerializer;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.model.*;
import alien4cloud.utils.jackson.ConditionalAttributes;

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
                PaaSMessageMonitorEvent.class, PaaSInstanceStorageMonitorEvent.class };
        initIndices("deployedtopologies", false, Topology.class);
        initIndices("deploymentmonitorevents", true, classes);
        initCompleted();
    }

    public static class ElasticSearchMapper extends ObjectMapper {
        private static final long serialVersionUID = 1L;

        public ElasticSearchMapper() {
            super();
            this._serializationConfig = this._serializationConfig.withAttribute(BoundSerializer.BOUND_SERIALIZER_AS_NUMBER, "true");
            this._serializationConfig = this._serializationConfig.withAttribute(ConditionalAttributes.ES, "true");
            this._deserializationConfig = this._deserializationConfig.withAttribute(ConditionalAttributes.ES, "true");
        }
    }
}