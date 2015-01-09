package alien4cloud.model.common;

import java.util.List;

/**
 * Interface to be implemented by resources that supports tags management.
 */
public interface ITaggableResource {
    /**
     * Set the list of tags on the taggable resource.
     * 
     * @param tags The list of tags to set.
     */
    void setTags(List<Tag> tags);

    /**
     * Get the list of tags on the taggagle resource.
     * 
     * @return The list of tags to set.
     */
    List<Tag> getTags();
}