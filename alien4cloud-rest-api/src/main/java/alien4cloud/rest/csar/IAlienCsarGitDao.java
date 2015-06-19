package alien4cloud.rest.csar;

import java.util.List;
import java.util.Map;

import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.security.model.CsarGitRepository;

public interface IAlienCsarGitDao {

    /**
     * * Create a CsarGit in the store.
     * 
     * @param csargit The CsarGit to store.
     */
    void save(CsarGitRepository csargit);

    /**
     * Read a CsarGit from the store.
     * 
     * @param id The CsarGit unique id.
     */
    CsarGitRepository find(String id);


    /**
     * Delete a CsarGit from the store.
     * 
     * @param id The id of the CsarGit to delete.
     */
    void delete(String id);

    /**
     * Search for CsarGits.
     * 
     * @param searchQuery the search query text.
     * @param group The group to limit the search to a specific user group.
     * @param from offset from the first result you want to fetch.
     * @param size maximum amount of {@link User} to be returned.
     */
    FacetedSearchResult search(String searchQuery, String group, int from, int size);

    /**
     * Find CsarGit with filters
     * 
     * @param filters
     * @param maxElements
     * @return
     */
    GetMultipleDataResult find(Map<String, String[]> filters, int maxElements);

    /**
     * Read CsarGits from the store.
     * 
     * @param ids an array of unique ids.
     */
    List<CsarGitRepository> find(String... ids);
}
