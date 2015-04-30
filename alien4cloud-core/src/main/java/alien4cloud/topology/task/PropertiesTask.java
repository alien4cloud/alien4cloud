package alien4cloud.topology.task;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class PropertiesTask extends TopologyTask {
    // list of required properties not set
    private List<String> properties;
}
