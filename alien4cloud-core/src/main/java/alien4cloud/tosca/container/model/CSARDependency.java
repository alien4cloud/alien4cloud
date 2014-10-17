package alien4cloud.tosca.container.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * Dependency on another CSAR.
 * 
 * @author luc boutier
 */
@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = { "name", "version" })
@ToString
public class CSARDependency {
    @NonNull
    private String name;
    @NonNull
    private String version;
}
