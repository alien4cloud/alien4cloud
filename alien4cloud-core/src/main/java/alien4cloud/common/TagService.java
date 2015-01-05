package alien4cloud.common;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import alien4cloud.model.common.Tag;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.common.ITaggableResource;

import com.google.common.collect.Lists;

/**
 * Service that manages tags for taggable resources.
 */
@Service
public class TagService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    /**
     * Add or update a tag to a taggable resource.
     *
     * @param resource The resource for which to add or update the given tag.
     * @param key The key/name of the tag.
     * @param value The value of the tag.
     */
    public void upsertTag(ITaggableResource resource, String key, String value) {
        if (resource.getTags() == null) {
            resource.setTags(Lists.<Tag> newArrayList());
        }
        Tag newTag = new Tag(key, value);
        if (resource.getTags().contains(newTag)) {
            resource.getTags().remove(newTag);
        }
        resource.getTags().add(newTag);
        alienDAO.save(resource);
    }

    /**
     * Remove an existing tag.
     *
     * @param resource The resource from which to remove the tag.
     * @param key The key of the tag to remove.
     */
    public void removeTag(ITaggableResource resource, String key) {
        if (resource.getTags() != null) {
            resource.getTags().remove(new Tag(key, null));
            alienDAO.save(resource);
        }
    }
}
