package alien4cloud.tosca.container.model;

import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * Defines a dependency on a CloudServiceArchive.
 */
@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
@NoArgsConstructor
@EqualsAndHashCode(of = { "name", "version" })
@ToString
public class CSARDependency {
    @NonNull
    private String name;
    @NonNull
    private String version;

    public CSARDependency(String name, String version) {
        this.name = name;
        this.version = version;
    }
}
