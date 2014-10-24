package alien4cloud.cloud;

import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.cloud.Cloud;
import alien4cloud.model.cloud.CloudImage;

@Service
public class CloudImageService {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    public CloudImage getCloudImage(String id) {
        return alienDAO.findById(CloudImage.class, id);
    }

    public CloudImage getCloudImageFailIfNotExist(String id) {
        CloudImage image = getCloudImage(id);
        if (image == null) {
            throw new NotFoundException("Cloud image [" + id + "] cannot be found");
        }
        return image;
    }

    public void saveCloudImage(CloudImage cloudImage) {
        alienDAO.save(cloudImage);
    }

    public void deleteCloudImage(String id) {
        alienDAO.delete(CloudImage.class, id);
    }

    public boolean isCloudImageExist(CloudImage cloudImage) {
        Map<String, String[]> filters = Maps.newHashMap();
        filters.put("name", new String[]{cloudImage.getName()});
        return alienDAO.count(CloudImage.class, null, filters) > 0;
    }

    public void ensureCloudImageUniqueness(CloudImage cloudImage) {
        if (isCloudImageExist(cloudImage)) {
            throw new AlreadyExistException("Cloud image [" + cloudImage + "] already existed");
        }
    }

    /**
     * Get multiple cloud images.
     *
     * @param query The query to apply to filter cloud images.
     * @param ids   to be excluded from the result
     * @param from  The start index of the query.
     * @param size  The maximum number of elements to return.
     * @return A {@link alien4cloud.dao.model.GetMultipleDataResult} that contains cloud image objects.
     */
    public GetMultipleDataResult<CloudImage> get(String query, Set<String> ids, int from, int size) {
        FilterBuilder excludeFilter = null;
        // The filter check that id is not in 'ids' set
        if (ids != null && !ids.isEmpty()) {
            excludeFilter = FilterBuilders.notFilter(FilterBuilders.inFilter("id", ids.toArray(new String[ids.size()])));
        }
        return alienDAO.search(CloudImage.class, query, null, excludeFilter, null, from, size);
    }

    public Map<String, CloudImage> getMultiple(Set<String> imageIds) {
        Map<String, CloudImage> images = Maps.newHashMap();
        for (String imageId : imageIds) {
            images.put(imageId, getCloudImageFailIfNotExist(imageId));
        }
        return images;
    }

    /**
     * Get all cloud ids which use the image
     *
     * @param id id of the cloud image
     * @return the list of cloud ids using this image
     */
    public String[] getCloudsUsingImage(String id) {
        return alienDAO.selectPath(Cloud.class.getSimpleName().toLowerCase(), new Class<?>[]{Cloud.class}, QueryBuilders.termQuery("images", id), null, "id", 0, Integer.MAX_VALUE);
    }
}
