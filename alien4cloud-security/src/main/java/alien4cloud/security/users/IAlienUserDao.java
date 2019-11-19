package alien4cloud.security.users;

import java.util.List;
import java.util.Map;

import org.elasticsearch.index.query.QueryBuilder;

import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.security.model.User;

/**
 * DAO to manage users in a store.
 * 
 * @author luc boutier
 */
public interface IAlienUserDao {
    /**
     * Create a user in the store.
     * 
     * @param user The user to store.
     */
    void save(User user);

    /**
     * Read a user from the store.
     * 
     * @param username The user unique id.
     */
    User find(String username);

    /**
     * Delete a user from the store.
     * 
     * @param userName The id of the user to delete.
     */
    void delete(String userName);

    /**
     * Search for users.
     * 
     * @param searchQuery the search query text.
     * @param group The group to limit the search to a specific user group.
     * @param from offset from the first result you want to fetch.
     * @param size maximum amount of {@link User} to be returned.
     */
    FacetedSearchResult search(String searchQuery, String group, int from, int size);

    /**
     * Find user with filters
     * 
     * @param filters
     * @param maxElements
     * @return
     */
    GetMultipleDataResult find(Map<String, String[]> filters, int maxElements);

    /**
     * Read users from the store.
     * 
     * @param usernames an array of unique ids.
     */
    List<User> find(String... usernames);

    /**
     * Read users from the store, with pagination.
     * 
     * @param from offset from the first result you want to fetch.
     * @param size maximum amount of {@link User} to be returned.*
     * @param customFilter a customized filter.
     */
    GetMultipleDataResult<User> find(String searchQuery, int from, int size, QueryBuilder customFilter);
}