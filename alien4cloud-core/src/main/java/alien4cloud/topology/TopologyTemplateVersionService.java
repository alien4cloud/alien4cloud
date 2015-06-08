package alien4cloud.topology;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import alien4cloud.common.AbtractVersionService;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.model.templates.TopologyTemplate;
import alien4cloud.model.templates.TopologyTemplateVersion;

@Service
public class TopologyTemplateVersionService extends AbtractVersionService<TopologyTemplateVersion> {

    @PostConstruct
    public void init() {
        // this code to ensure backward compatibility when introducing versions for topology templates (1.0.0-SM27)
        // search all topology templates
        GetMultipleDataResult<TopologyTemplate> result = alienDAO.find(TopologyTemplate.class, null, Integer.MAX_VALUE);
        for (TopologyTemplate tt : result.getData()) {
            String ttid = tt.getId();
            if (getByDelegateId(ttid).length == 0) {
                // no versions found for this topology template, we create one per default
                this.createVersion(ttid, tt.getTopologyId());
            }
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
    protected String getDelegatePropertyName() {
        return "topologyTemplateId";
    }

}
