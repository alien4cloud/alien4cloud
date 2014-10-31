package alien4cloud.tosca.model;

import java.util.List;

import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

import com.google.common.collect.Lists;

/**
 * Contains meta-data on the archive.
 * 
 * @author luc boutier
 */
@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
public class ToscaMeta {
    @NotNull
    private String name;
    @NotNull
    private String version;
    private String license;
    private String createdBy;
    private String entryDefinitions;
    private List<String> definitions = Lists.newArrayList();
}
