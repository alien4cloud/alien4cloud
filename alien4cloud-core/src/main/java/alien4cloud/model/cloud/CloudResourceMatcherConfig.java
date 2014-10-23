package alien4cloud.model.cloud;

import java.util.List;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Lists;

import lombok.Getter;
import lombok.Setter;

@ESObject
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloudResourceMatcherConfig {

    @Id
    private String id;

    private List<MatchedComputeTemplate> matchedComputeTemplates = Lists.newArrayList();
}