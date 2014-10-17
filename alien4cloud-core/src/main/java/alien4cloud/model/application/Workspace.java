package alien4cloud.model.application;

import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@ESObject
@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
@JsonInclude(Include.NON_NULL)
public class Workspace {
    private String id;
    private String name;
    private String description;
}