package alien4cloud.utils;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Getter
@Setter
public class TreeNode {
    private String name;
    private String fullPath;
    private boolean isLeaf;
    private List<TreeNode> children;
    @JsonIgnore
    private TreeNode parent;
}
