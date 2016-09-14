package alien4cloud.topology;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import alien4cloud.common.AbtractVersionService;
import alien4cloud.csar.services.CsarService;
import alien4cloud.exception.DeleteReferencedObjectException;
import org.alien4cloud.tosca.model.Csar;
import alien4cloud.model.templates.TopologyTemplate;
import alien4cloud.model.templates.TopologyTemplateVersion;
import org.alien4cloud.tosca.model.templates.Topology;

@Service
public class TopologyTemplateVersionService extends AbtractVersionService<TopologyTemplateVersion> {

    @Resource
    private TopologyServiceCore topologyServiceCore;
    @Resource
    private CsarService csarService;

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
