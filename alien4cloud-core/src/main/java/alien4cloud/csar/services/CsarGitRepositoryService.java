package alien4cloud.csar.services;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.git.CsarGitCheckoutLocation;
import alien4cloud.model.git.CsarGitRepository;
import alien4cloud.utils.UrlUtil;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.UUID;

/**
 * Manages operations on a CsarGitRepository
 */
@Slf4j
@Service
public class CsarGitRepositoryService {
    private static final String URL_FIELD = "repositoryUrl";
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    private static final String forbiddenSubPathCharacter = "*";

    /**
     * Create a CsarGitRepository in the system to store its informations
     *
     * @param repositoryUrl The unique Git url of the CsarGitRepository
     * @param username The username of the user
     * @param password The password of the user
     * @param importLocations Locations where Csar's files are store
     * @param isStoredLocally The state of the the CsarGitRepository
     * @return The auto-generated id of the CsarGitRepository object
     */
    public String create(String repositoryUrl, String username, String password, List<CsarGitCheckoutLocation> importLocations, boolean isStoredLocally) {
        validatesRepositoryUrl(repositoryUrl);
        failIfExists(repositoryUrl);
        validatesImportLocations(importLocations);
        // create it
        CsarGitRepository csarGit = new CsarGitRepository();
        csarGit.setId(UUID.randomUUID().toString());
        csarGit.setRepositoryUrl(repositoryUrl);
        csarGit.setUsername(username);
        csarGit.setPassword(password);
        csarGit.setImportLocations(importLocations);
        csarGit.setStoredLocally(isStoredLocally);
        alienDAO.save(csarGit);
        return csarGit.getId();
    }

    private void validatesImportLocations(List<CsarGitCheckoutLocation> importLocations) {
        if (importLocations.isEmpty()) {
            throw new InvalidArgumentException("Import locations cannot be empty.");
        }
        importLocations.stream().forEachOrdered(location -> {
            if (forbiddenSubPathCharacter.equals(location.getSubPath())) {
                location.setSubPath(null);
                log.debug("The path file cannot be the special character : <" + forbiddenSubPathCharacter + ">. All files will be imported.");
            }
        });
    }

    private void validatesRepositoryUrl(String repositoryUrl) {
        // check if the repository url has a valid format
        if (!UrlUtil.isValid(repositoryUrl)) {
            throw new InvalidArgumentException("Repository url <" + repositoryUrl + "> is not a valid url.");
        }
    }

    private void failIfExists(String repositoryUrl) {
        CsarGitRepository existingCsarGitRepository = alienDAO.customFind(CsarGitRepository.class, QueryBuilders.termQuery(URL_FIELD, repositoryUrl));
        if (existingCsarGitRepository != null) {
            throw new AlreadyExistException("A repository with url <" + repositoryUrl + "> already exists in alien 4 cloud.");
        }
    }

    /**
     * Get the csar git repository matching the given id or throw a NotFoundException
     *
     * @param id Id of the csar git repository that we want to get.
     * @return An instance of the csar git repository.
     */
    public CsarGitRepository getOrFail(String id) {
        CsarGitRepository csarGitRepository = alienDAO.findById(CsarGitRepository.class, id);
        if (csarGitRepository == null) {
            throw new NotFoundException("CSAR Git repository [" + id + "] doesn't exists.");
        }
        return csarGitRepository;
    }

    /**
     * Get multiple csar git repositories.
     *
     * @param query The query to apply to filter csar git repositories.
     * @param from The start index of the query.
     * @param size The maximum number of elements to return.
     * @return A {@link GetMultipleDataResult} that contains CsarGitRepository objects.
     */
    public GetMultipleDataResult<CsarGitRepository> search(String query, int from, int size) {
        return alienDAO.search(CsarGitRepository.class, query, null, from, size);
    }

    /**
     * Update informations for a given CsarGitRepository.
     *
     * @param id The id of the CsarGitRepository to update.
     * @param repositoryUrl The new url of the CsarGitRepository
     * @param username The username associated to the CsarGitRepository
     * @param password The password associated to the CsarGitRepository
     * @param importLocations
     * @param isStoredLocally
     */
    public void update(String id, String repositoryUrl, String username, String password, List<CsarGitCheckoutLocation> importLocations,
            boolean isStoredLocally) {
        validatesRepositoryUrl(repositoryUrl);
        CsarGitRepository repositoryToUpdate = getOrFail(id);
        if (!repositoryToUpdate.getRepositoryUrl().equals(repositoryUrl)) {
            failIfExists(repositoryUrl);
            repositoryToUpdate.setRepositoryUrl(repositoryUrl);
        }
        if (username != null) {
            repositoryToUpdate.setUsername(username);
        }
        if (password != null) {
            repositoryToUpdate.setPassword(password);
        }
        repositoryToUpdate.setStoredLocally(isStoredLocally);

        // just merge the existing location hash into new list so that we don't loose this information
        for (CsarGitCheckoutLocation location : importLocations) {
            CsarGitCheckoutLocation existingLocation = findLocationIn(location, repositoryToUpdate.getImportLocations());
            if (existingLocation != null) {
                location.setLastImportedHash(existingLocation.getLastImportedHash());
            }
            if (forbiddenSubPathCharacter.equals(location.getSubPath())) {
                location.setSubPath(null);
                log.debug("The path file cannot be the special character : <" + forbiddenSubPathCharacter + ">. All files will be imported.");
            }
        }
        repositoryToUpdate.setImportLocations(importLocations);

        alienDAO.save(repositoryToUpdate);
    }

    private CsarGitCheckoutLocation findLocationIn(CsarGitCheckoutLocation location, List<CsarGitCheckoutLocation> importLocations) {
        for (CsarGitCheckoutLocation givenLocation : importLocations) {
            if (givenLocation.getBranchId().equals(location.getBranchId())
                    && ((Strings.isNullOrEmpty(givenLocation.getSubPath()) && Strings.isNullOrEmpty(location.getSubPath()))
                            || (!Strings.isNullOrEmpty(givenLocation.getSubPath()) && givenLocation.getSubPath().equals(location.getSubPath())))) {
                return givenLocation;
            }
        }
        return null;
    }
}
