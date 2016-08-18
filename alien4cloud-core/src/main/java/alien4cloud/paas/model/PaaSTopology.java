package alien4cloud.paas.model;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The result of parsing of an Alien topology
 * 
 * @author Minh Khang VU
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
public class PaaSTopology {
    private List<PaaSNodeTemplate> computes;
    private List<PaaSNodeTemplate> networks;
    private List<PaaSNodeTemplate> volumes;
    private List<PaaSNodeTemplate> nonNatives;
    private Map<String, PaaSNodeTemplate> allNodes;
    private Map<String, List<PaaSNodeTemplate>> groups;
}
