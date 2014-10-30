package alien4cloud.model.cloud;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@ESObject
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloudResourceMatcherConfig {

    @Id
    private String id;

    private List<MatchedComputeTemplate> matchedComputeTemplates = Lists.newArrayList();

    @JsonIgnore
    public Map<ComputeTemplate, String> getComputeTemplateMapping() {
        Map<ComputeTemplate, String> config = Maps.newHashMap();
        if (matchedComputeTemplates != null && !matchedComputeTemplates.isEmpty()) {
            for (MatchedComputeTemplate template : matchedComputeTemplates) {
                config.put(template.getComputeTemplate(), template.getPaaSResourceId());
            }
        }
        return config;
    }
}