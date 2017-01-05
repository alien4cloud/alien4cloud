package org.alien4cloud.tosca.model;

import java.nio.file.Path;
import java.util.Set;

import com.google.common.collect.Sets;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(of = "self")
public class CsarDependenciesBean {
    private Path path;
    private CSARDependency self = new CSARDependency();
    private Set<CSARDependency> dependencies;
    private Set<CsarDependenciesBean> dependents = Sets.newHashSet();

}
