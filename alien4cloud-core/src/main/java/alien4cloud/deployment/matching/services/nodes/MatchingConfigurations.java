package alien4cloud.deployment.matching.services.nodes;

import alien4cloud.model.deployment.matching.MatchingConfiguration;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;
import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class MatchingConfigurations {
    private Map<String, MatchingConfiguration> matchingConfigurations = Maps.newHashMap();
}