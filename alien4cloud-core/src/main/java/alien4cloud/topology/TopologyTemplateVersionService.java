package alien4cloud.topology;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import alien4cloud.common.AbtractVersionService;
import alien4cloud.csar.services.CsarService;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.DeleteReferencedObjectException;
import alien4cloud.model.components.Csar;
import alien4cloud.model.templates.TopologyTemplate;
import alien4cloud.model.templates.TopologyTemplateVersion;
import alien4cloud.model.topology.Topology;

@Service
public class TopologyTemplateVersionService extends AbtractVersionService<TopologyTemplateVersion> {

    @Resource
    private TopologyServiceCore topologyServiceCore;
    @Resource
    private TopologyService topologyService;
    @Resource
    private CsarService csarService;

    @PostConstruct
    public void init() {
        // this code to ensure backward compatibility when introducing versions for topology templates (1.0.0-SM27)
        // search all topology templates
        GetMultipleDataResult<TopologyTemplate> result = alienDAO.find(TopologyTemplate.class, null, Integer.MAX_VALUE);
        for (TopologyTemplate tt : result.getData()) {
            String ttid = tt.getId();
            if (getByDelegateId(ttid).length == 0) {
                // no versions found for this topology template, we create one per default
                this.createVersion(ttid, tt.getTopologyId(), null);
            }
        }
    }

    @Override
    public void delete(String versionId) {
        TopologyTemplateVersion ttv = getOrFail(versionId);
        Topology topology = topologyServiceCore.getTopology(ttv.getTopologyId());
        Csar csar = null;
        if (topology.getSubstitutionMapping() != null && topology.getSubstitutionMapping().getSubstitutionType() != null) {
            // this topology expose substitution, we have to delete the related CSAR and type
            csar = csarService.getTopologySubstitutionCsar(topology.getId());
            // will fail if the stuff is used in a topology
            if (csar != null && csarService.isDependency(csar.getName(), csar.getVersion())) {
                throw new DeleteReferencedObjectException("This topology template version can not be deleted since it's already used.");
            }
        }
        super.delete(versionId);
        if (csar != null) {
            csarService.deleteCsar(csar.getId());
        }
    }

    @Override
    protected TopologyTemplateVersion buildVersionImplem() {
        return new TopologyTemplateVersion();
    }

    @Override
    protected TopologyTemplateVersion[] buildVersionImplemArray(int length) {
        return new TopologyTemplateVersion[length];
    }

    @Override
    protected Class<TopologyTemplateVersion> getVersionImplemClass() {
        return TopologyTemplateVersion.class;
    }

    @Override
    protected Class<?> getDelegateClass() {
        return TopologyTemplate.class;
    }

    @Override
    protected String getDelegatePropertyName() {
        return "topologyTemplateId";
    }

}
