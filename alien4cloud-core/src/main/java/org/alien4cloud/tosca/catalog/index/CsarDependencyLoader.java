package org.alien4cloud.tosca.catalog.index;

import java.util.Set;

import javax.inject.Inject;

import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import alien4cloud.exception.NotFoundException;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.context.ToscaContextual;

@Component
public class CsarDependencyLoader implements ICsarDependencyLoader {

    @Inject
    private ICsarService csarService;

    @Override
    public Set<CSARDependency> getDependencies(String name, String version) {
        Csar csar = csarService.get(name, version);
        if (csar == null) {
            throw new NotFoundException("Csar with name [" + name + "] and version [" + version + "] cannot be found");
        }
        if (csar.getDependencies() == null || csar.getDependencies().isEmpty()) {
            return Sets.newHashSet();
        }
        return Sets.newHashSet(csar.getDependencies());
    }

    @Override
    @ToscaContextual
    public CSARDependency buildDependencyBean(String name, String version) {
        CSARDependency newDependency = new CSARDependency(name, version);
        Csar csar = ToscaContext.get().getArchive(name, version);
        if (csar != null) {
            newDependency.setHash(csar.getHash());
        }
        return newDependency;
    }
}
