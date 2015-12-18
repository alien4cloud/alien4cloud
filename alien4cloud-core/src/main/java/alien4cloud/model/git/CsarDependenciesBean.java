package alien4cloud.model.git;

import java.nio.file.Path;
import java.util.Set;

import alien4cloud.model.components.CSARDependency;
import com.google.common.collect.Sets;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(of = "self")
public class CsarDependenciesBean {
    private Path path;
    private CSARDependency self;
    private Set<CSARDependency> dependencies;
    private Set<CsarDependenciesBean> dependents = Sets.newHashSet();

}
