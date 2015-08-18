package alien4cloud.security.model;

import java.nio.file.Path;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CsarDependenciesBean {

    private Path path;
    private String name;
    private String version;
    private Set<?> dependencies;
}
