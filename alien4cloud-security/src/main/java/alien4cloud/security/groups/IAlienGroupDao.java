package alien4cloud.security.groups;

import java.util.List;
import java.util.Map;

import alien4cloud.dao.model.GetMultipleDataResult;

/**
 * DAO to manage groups in a store.
 * 
 * @author igor ngouagna
 */
public interface IAlienGroupDao {

    /**
     * Search group by id
     * 
     * @param id id of the group
     * @return the group if found, null otherwise
     */
    Group find(String id);

    /**
     * Create/update a group in the store.
     * 
     * @param group The group to store.
     */
    void save(Group group);

    /**
     * Delete a group from the store, given its name.
     * 
     * @param groupId The id of the group to delete.
     * 
     */
    void delete(String groupId);

    /**
     * Read a group from the store.
     * 
     * @param groupName The group's name.
     */
    boolean isGroupWithNameExist(String groupName);

    /**
     * Search for groups.
     * 
     * @param searchQuery the search query text.
     * @param filters filters to apply
     * @param from offset from the first result you want to fetch.
     * @param size maximum amount of {@link Group} to be returned.
     */
    GetMultipleDataResult search(String searchQuery, Map<String, String[]> filters, int from, int size);

    /**
     * Find group with filters
     * 
     * @param filters
     * @param maxElements
     * @return
     */
    GetMultipleDataResult find(Map<String, String[]> filters, int maxElements);

    /**
     * Read groups from the store.
     * 
     * @param ids an array of unique ids.
     */
    List<Group> find(String... ids);

}
