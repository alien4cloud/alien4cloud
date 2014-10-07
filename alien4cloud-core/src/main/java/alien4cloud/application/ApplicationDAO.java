package alien4cloud.application;

import javax.annotation.Resource;

import org.elasticsearch.mapping.QueryHelper;
import org.springframework.stereotype.Component;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.Application;

/**
 * Manages simple DAO features to work with applications.
 *
 * @author luc boutier
 */
@Component
public class ApplicationDAO {
    @Resource
    private QueryHelper queryHelper;

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    /**
     * Get an application from it's id and throw a {@link NotFoundException} in case the application doesn't exists.
     *
     * @param id The id of the application to get.
     * @return The application to get.
     */
    public Application getApplication(String id) {
        Application application = alienDAO.findById(Application.class, id);
        if (application == null) {
            throw new NotFoundException("Application [" + id + "] cannot be found");
        }
        return application;
    }
}
