package alien4cloud.model.topology;

import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;

/**
 * A node group is a group of nodes in a topology. All members share the same policies.
 */
@Getter
@Setter
@ESObject
@NoArgsConstructor
public class NodeGroup {

    private String name;

    private Set<String> members;

    private List<Policy> policies;

}
