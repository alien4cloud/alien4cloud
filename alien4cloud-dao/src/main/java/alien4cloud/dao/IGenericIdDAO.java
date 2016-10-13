package alien4cloud.dao;

import java.util.List;

/**
 * A DAO that allows accessing data by Id or / and multiple Ids.
 * 
 * @author Igor Ngouagna
 */
public interface IGenericIdDAO {
    /**
     * Saves an entity into the repository
     * 
     * @param entity the entity to save
     */
    <T> void save(T entity);

    /**
     * Bulk save multiple entities into the repository
     * 
     * @param entities The entities to save.
     */
    <T> void save(T[] entities);

    /**
     * Find an instance from the given class.
     * 
     * @param clazz The class of the object to find.
     * @param id The id of the object.
     * @return The object that has the given id or null if no object matching the request is found.
     */
    <T> T findById(Class<T> clazz, String id);

    /**
     * Check whether an object with the given id exists
     * 
     * @param clazz The class of the object to find.
     * @param id The id of the object.
     * @param <T> type of the object
     * @return true if object exists, false otherwise
     */
    <T> boolean exist(Class<T> clazz, String id);

    /**
     * Find instances by id
     * 
     * @param clazz The class for which to find an instance.
     * @param ids array of id of the data to find.
     * @return List of Objects that has the given ids or empty list if no object matching the request is found.
     */
    <T> List<T> findByIds(Class<T> clazz, String... ids);

    /**
     * Delete an instance from the given class.
     * 
     * @param clazz The class of the object to delete.
     * @param id The id of the object to delete.
     */
    void delete(Class<?> clazz, String id);
}