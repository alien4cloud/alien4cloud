package alien4cloud.service;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.service.ServiceResource;
import org.alien4cloud.tosca.model.instances.NodeInstance;
import alien4cloud.tosca.context.ToscaContextual;
import alien4cloud.tosca.topology.NodeTemplateBuilder;
import alien4cloud.utils.VersionUtil;
import alien4cloud.utils.version.Version;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.types.NodeType;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * Manages services.
 *
 * TODO: should be notifed when a location is deleted in order to clean locationIds
 */
@Service
public class ServiceResourceService {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    /**
     * Creates a service.
     *
     * @param serviceName The unique name that defines the service from user point of view.
     * @param serviceVersion The id of the plugin used to communicate with the orchestrator.
     * @return The generated identifier for the service.
     */
    @ToscaContextual
    public synchronized String create(String serviceName, String serviceVersion, NodeType nodeType, String deploymentId) {

        ServiceResource serviceResource = new ServiceResource();
        // generate an unique id
        serviceResource.setId(UUID.randomUUID().toString());
        serviceResource.setName(serviceName);
        serviceResource.setVersion(serviceVersion);
        serviceResource.setDeploymentId(deploymentId);
        serviceResource.setCreationDate(new Date());

        // build a nodeTemplate
        NodeTemplate nodeTemplate = NodeTemplateBuilder.buildNodeTemplate(nodeType, null);
        NodeInstance instance = new NodeInstance();
        instance.setNodeTemplate(nodeTemplate);
        serviceResource.setNodeInstance(instance);

        ensureUnicityAndSave(serviceResource, null, null);

        return serviceResource.getId();
    }

    /**
     * Get the service matching the given id or throw a NotFoundException.
     *
     * @param id If of the service that we want to get.
     * @return An instance of the service.
     */
    public ServiceResource getOrFail(String id) {
        ServiceResource serviceResource = alienDAO.findById(ServiceResource.class, id);
        if (serviceResource == null) {
            throw new NotFoundException("Service [" + id + "] doesn't exists.");
        }
        return serviceResource;
    }

    /**
     * Save the service but ensure that the name is unique for the given version before saving it.
     *
     * @param serviceResource The service to save.
     */
    public void save(ServiceResource serviceResource) {
        ensureUnicityAndSave(serviceResource, serviceResource.getName(), serviceResource.getVersion());
    }

    /**
     * Save the service but ensure that the name is unique for the given version before saving it.
     *
     * @param serviceResource The service to save.
     */
    public synchronized void ensureUnicityAndSave(ServiceResource serviceResource, String oldName, String oldVersion) {
        if (StringUtils.isBlank(oldName) || !Objects.equals(serviceResource.getName(), oldName) || StringUtils.isBlank(oldVersion) || !Objects.equals(serviceResource.getVersion(), oldVersion)) {
            // check that the service doesn't already exists
            TermQueryBuilder nameQuery = QueryBuilders.termQuery("name", serviceResource.getName());
            TermQueryBuilder versionQuery = QueryBuilders.termQuery("version", serviceResource.getVersion());
            if (alienDAO.count(ServiceResource.class, QueryBuilders.boolQuery().must(nameQuery).must(versionQuery)) > 0) {
                throw new AlreadyExistException("a service with the given name and version already exists.");
            }
        }
        Version version = VersionUtil.parseVersion(serviceResource.getVersion());
        serviceResource.setNestedVersion(version);
        alienDAO.save(serviceResource);
    }
}
