package alien4cloud.utils;

import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "name")
public class TreeNode implements Comparable<TreeNode> {
    private String name;
    /** The full path of the node in the tree. */
    private String fullPath;
    /** Optional temporary artifact id that replace the actual file in an edition context. */
    private String artifactId;
    private boolean isLeaf;
    private TreeSet<TreeNode> children;
    @JsonIgnore
    private TreeNode parent;

    public TreeNode(String name) {
        this.name = name;
    }

    /**
     * Get a children based on it's name.
     * 
     * @param childName The name of the children to get.
     * @return A tree node or null.
     */
    public TreeNode getChild(String childName) {
        TreeNode template = new TreeNode(childName);
        TreeNode match = children == null ? null : children.ceiling(template);
        if (template.equals(match)) {
            return match;
        }
        return null;
    }

    @Override
    public int compareTo(TreeNode o) {
        return this.name.compareTo(o.getName());
    }
}