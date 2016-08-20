package alien4cloud.model.topology;

import alien4cloud.json.deserializer.PolicyDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Set;

/**
 * A node group is a group of nodes in a topology. All members share the same policies.
 */
@Getter
@Setter
@NoArgsConstructor
public class NodeGroup {

    private String name;

    private Set<String> members;

    /**
     * The group index for a given topology.
     */
    private int index;

    @JsonDeserialize(contentUsing = PolicyDeserializer.class)
    private List<AbstractPolicy> policies;

}
