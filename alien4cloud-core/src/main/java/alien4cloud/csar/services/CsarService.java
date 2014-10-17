package alien4cloud.csar.services;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.csar.model.Csar;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.NotFoundException;
import alien4cloud.tosca.container.model.CSARDependency;
import alien4cloud.tosca.container.model.CloudServiceArchive;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@Component
public class CsarService implements ICsarDependencyLoader {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO csarDAO;

    /**
     * Retrieve direct dependencies for csar with given name and version
     *
     * @param csarName name of the csar
     * @param csarVersion version the csar
     * @return the list of dependencies
     */
    @Override
    public Set<CSARDependency> getDependencies(String csarName, String csarVersion) {
        Csar csar = new Csar();
        csar.setName(csarName);
        csar.setVersion(csarVersion);
        csar = csarDAO.findById(Csar.class, csar.getId());
        if (csar == null) {
            throw new NotFoundException("Csar with name [" + csarName + "] and version [" + csarVersion + "] cannot be found");
        }
        if (csar.getDependencies() == null || csar.getDependencies().isEmpty()) {
            return Sets.newHashSet();
        }
        return Sets.newHashSet(csar.getDependencies());
    }

    public Csar saveUploadedCsar(CloudServiceArchive archive) {
        Csar csar = new Csar();
        csar.setName(archive.getMeta().getName());
        csar.setVersion(archive.getMeta().getVersion());
        Set<CSARDependency> dependencies = archive.getMeta().getDependencies();
        if (dependencies != null && !dependencies.isEmpty()) {
            for (CSARDependency dependency : dependencies) {
                dependencies.addAll(getDependencies(dependency.getName(), dependency.getVersion()));
            }
        }
        csar.setDependencies(dependencies);
        csarDAO.save(csar);
        return csar;
    }

    public Map<String, Csar> findByIds(String fetchContext, String... ids) {
        Map<String, Csar> csarMap = Maps.newHashMap();
        List<Csar> csars = csarDAO.findByIdsWithContext(Csar.class, fetchContext, ids);
        for (Csar csar : csars) {
            csarMap.put(csar.getId(), csar);
        }
        return csarMap;
    }

    public Csar getMandatoryCsar(String csarId) {
        Csar csar = csarDAO.findById(Csar.class, csarId);
        if (csar == null) {
            throw new NotFoundException("Csar with id [" + csarId + "] do not exist");
        } else {
            return csar;
        }
    }
}
