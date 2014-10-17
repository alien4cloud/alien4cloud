package alien4cloud.csar.services;

import java.util.Set;

import alien4cloud.tosca.container.model.CSARDependency;

public interface ICsarDependencyLoader {

    Set<CSARDependency> getDependencies(String csarName, String csarVersion);
}
