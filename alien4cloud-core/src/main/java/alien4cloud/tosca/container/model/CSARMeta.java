package alien4cloud.tosca.container.model;

import java.util.List;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Contains meta-data on the archive.
 * 
 * @author luc boutier
 */
@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
public class CSARMeta {
    @NotNull
    private String name;
    @NotNull
    private String version;
    private String license;
    private String createdBy;
    @Valid
    private Set<CSARDependency> dependencies = Sets.newHashSet();
    private String entryDefinitions;
    @NotNull
    @Size(min = 1)
    private List<String> definitions = Lists.newArrayList();
}
