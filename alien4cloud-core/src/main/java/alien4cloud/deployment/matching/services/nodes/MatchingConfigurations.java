package alien4cloud.deployment.matching.services.nodes;

import alien4cloud.model.deployment.matching.MatchingConfiguration;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MatchingConfigurations {
    private List<MatchingConfiguration> matchingConfigurations;
}
