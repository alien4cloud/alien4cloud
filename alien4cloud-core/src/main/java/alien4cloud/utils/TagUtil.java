package alien4cloud.utils;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import alien4cloud.model.common.Tag;

import static alien4cloud.utils.AlienUtils.safe;

/**
 * Utility to work with tags.
 */
public final class TagUtil {
    private TagUtil() {
    }

    /**
     * Convert a tag list to a map
     *
     * @param tags
     *            The list of tags to convert in a map.
     * @return The value of the tag or null if no value exists.
     */
    public static Map<String, String> tagListToMap(List<Tag> tags) {
        Map<String, String> tagMap = Maps.newHashMap();
        for (Tag tag : safe(tags)) {
            tagMap.put(tag.getName(), tag.getValue());
        }
        return tagMap;
    }

    /**
     * Utility method to extract a single tag value from a tag list. If you want to extract more that a single tag value you should convert the tag list to map
     * rather than calling this method multiple times.
     *
     * @param tags
     *            The list of tags from which to extract a tag value.
     * @param key
     *            The key of the tag in the list.
     * @return The value of the tag or null if no value exists.
     */
    public static String getTagValue(List<Tag> tags, String key) {
        for (Tag tag : safe(tags)) {
            if (tag.getName().equals(key)) {
                return tag.getValue();
            }
        }
        return null;
    }

    /**
     * Utility method to get a {@link Tag} by its name from a tag list.
     * 
     * @param tags
     *            The list of tags from which to get a tag.
     * @param key
     *            The key of the tag in the list.
     * @return
     */
    public static Tag getTagByName(List<Tag> tags, String key) {
        for (Tag tag : safe(tags)) {
            if (tag.getName().equals(key)) {
                return tag;
            }
        }
        return null;
    }
}
