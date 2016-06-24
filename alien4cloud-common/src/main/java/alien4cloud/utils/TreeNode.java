package alien4cloud.utils;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TreeNode {
    private String name;
    /** The full path of the node in the tree. */
    private String fullPath;
    /** Optional temporary artifact id that replace the actual file in an edition context. */
    private String artifactId;
    private boolean isLeaf;
    private List<TreeNode> children;
    @JsonIgnore
    private TreeNode parent;
}
