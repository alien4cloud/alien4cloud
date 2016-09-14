package alien4cloud.topology;

import java.util.UUID;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.springframework.stereotype.Service;

import alien4cloud.csar.services.CsarService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.DeleteReferencedObjectException;
import alien4cloud.exception.NotFoundException;
import org.alien4cloud.tosca.model.Csar;
import alien4cloud.model.templates.TopologyTemplate;
import alien4cloud.model.templates.TopologyTemplateVersion;
import org.alien4cloud.tosca.model.templates.Topology;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.Role;
import lombok.extern.slf4j.Slf4j;

import static alien4cloud.dao.FilterUtil.fromKeyValueCouples;

/**
 * Manage operations on a topology template.
 */
@Slf4j
@Service
public class TopologyTemplateService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Inject
    private TopologyTemplateVersionService versionService;
    @Inject
    private TopologyServiceCore topologyServiceCore;
    @Inject
    private CsarService csarService;

    /**
     * Retrieve the topology template from its id
     *
     * @param topologyTemplateId the topology template's id
     * @return the required topology template
     */
    public TopologyTemplate getOrFailTopologyTemplate(String topologyTemplateId) {
        TopologyTemplate topologyTemplate = alienDAO.findById(TopologyTemplate.class, topologyTemplateId);
        if (topologyTemplate == null) {
            log.debug("Failed to recover the topology template <{}>", topologyTemplateId);
            throw new NotFoundException("Topology template with id [" + topologyTemplateId + "] cannot be found");
        }
        return topologyTemplate;
    }

    /**
     * Retrieve a topology template from it's name.
     * 
     * @param name The name of the topology template.
     * @return The instance of the topology template that match the given name or null if none is found.
     */
    public TopologyTemplate getTopologyTemplateByName(String name) {
        return alienDAO.buildQuery(TopologyTemplate.class).setFilters(fromKeyValueCouples("name", name)).prepareSearch().find();
    }

    /**
     * Create a new topology template.
     *
     * @param topology The topology to use for the new template.
     * @param name The name of the topology template.
     * @param description The description of the topology template
     * @param version The initial version of the topology template.
     * @return An instance of topology template.
     */
    public TopologyTemplate createTopologyTemplate(Topology topology, String name, String description, String version) {
        String topologyId = UUID.randomUUID().toString();
        topology.setId(topologyId);

        String topologyTemplateId = UUID.randomUUID().toString();
        TopologyTemplate topologyTemplate = new TopologyTemplate();
        topologyTemplate.setId(topologyTemplateId);
        topologyTemplate.setName(name);
        topologyTemplate.setDescription(description);

        topology.setDelegateId(topologyTemplateId);
        topology.setDelegateType(TopologyTemplate.class.getSimpleName().toLowerCase());

        topologyServiceCore.save(topology);
        this.alienDAO.save(topologyTemplate);
        if (version == null) {
            versionService.createVersion(topologyTemplateId, null, topology);
        } else {
            versionService.createVersion(topologyTemplateId, null, version, null, topology);
        }

        return topologyTemplate;
    }

    /**
     * Delete a topology template from alien4cloud.
     *
     * @param topologyTemplateId The id of the topology template to delete.
     */
    public void delete(String topologyTemplateId) {
        AuthorizationUtil.checkHasOneRoleIn(Role.ARCHITECT);
        TopologyTemplate topologyTemplate = getOrFailTopologyTemplate(topologyTemplateId);
        // here we check that the template is not used in a topology or template composition
        for (TopologyTemplateVersion ttv : versionService.getByDelegateId(topologyTemplate.getId())) {
            Topology topology = topologyServiceCore.getTopology(ttv.getTopologyId());
            if (topology != null && topology.getSubstitutionMapping() != null && topology.getSubstitutionMapping().getSubstitutionType() != null) {
                // this topology template expose some substitution stuffs
                // we have to check that it is not used by another topology
                Csar csar = csarService.getTopologySubstitutionCsar(topology.getId());
                if (csar != null) {
                    Topology[] dependentTopologies = csarService.getDependantTopologies(csar.getName(), csar.getVersion());
                    if (dependentTopologies != null && dependentTopologies.length > 0) {
                        throw new DeleteReferencedObjectException("This csar can not be deleted since it's a dependencie for others");
                    }
                }
            }
        }
        // none of the version is used as embeded topology, we have to delete each version
        for (TopologyTemplateVersion ttv : versionService.getByDelegateId(topologyTemplate.getId())) {
            versionService.delete(ttv.getId());
        }
        alienDAO.delete(TopologyTemplate.class, topologyTemplate.getId());
    }
}