package alien4cloud.rest.cloud;

import java.io.IOException;
import java.util.UUID;

import javax.annotation.Resource;
import javax.validation.Valid;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.cloud.CloudImageService;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.DeleteReferencedObjectException;
import alien4cloud.images.IImageDAO;
import alien4cloud.images.exception.ImageUploadException;
import alien4cloud.model.cloud.CloudImage;
import alien4cloud.model.cloud.CloudImageRequirement;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.utils.ReflectionUtil;

import com.google.common.collect.Sets;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.Authorization;

/**
 * @author Minh Khang VU
 */
@Slf4j
@RestController
@RequestMapping("/rest/cloud-images")
public class CloudImageController {

    @Resource
    private CloudImageService cloudImageService;

    @Resource
    private IImageDAO imageDAO;

    /**
     * Create a new cloud image.
     *
     * @param request The cloud image creation request.
     */
    @ApiOperation(value = "Create a new cloud image.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<String> create(
            @ApiParam(value = "The instance of cloud image to add.", required = true) @Valid @RequestBody CloudImageCreateRequest request) {
        CloudImage cloudImage = new CloudImage();
        cloudImage.setId(UUID.randomUUID().toString());
        cloudImage.setName(request.getName());
        cloudImage.setOsArch(request.getOsArch());
        cloudImage.setOsDistribution(request.getOsDistribution());
        cloudImage.setOsType(request.getOsType());
        cloudImage.setOsVersion(request.getOsVersion());
        if (request.getDiskSize() != null || request.getMemSize() != null || request.getNumCPUs() != null) {
            cloudImage.setRequirement(new CloudImageRequirement());
            cloudImage.getRequirement().setDiskSize(request.getDiskSize());
            cloudImage.getRequirement().setMemSize(request.getMemSize());
            cloudImage.getRequirement().setNumCPUs(request.getNumCPUs());
        }
        cloudImageService.ensureCloudImageUniqueness(cloudImage);
        cloudImageService.saveCloudImage(cloudImage);
        return RestResponseBuilder.<String> builder().data(cloudImage.getId()).build();
    }

    /**
     * Update an existing cloud image.
     *
     * @param id id of the cloud image
     * @param request the request which contains the cloud image details
     */
    @ApiOperation(value = "Update an existing cloud image.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> update(@ApiParam(value = "The cloud image id to update.", required = true) @PathVariable String id,
            @ApiParam(value = "The update request.", required = true) @Valid @RequestBody CloudImageUpdateRequest request) {
        CloudImage cloudImage = cloudImageService.getCloudImageFailIfNotExist(id);
        // Register the state before the merge
        String oldName = cloudImage.getName();
        ReflectionUtil.mergeObject(request, cloudImage);
        // This information is mandatory and so we do not need to check null value
        if (!oldName.equals(cloudImage.getName())) {
            // Check uniqueness before updating
            cloudImageService.ensureCloudImageUniqueness(cloudImage);
        }
        if (request.getDiskSize() != null || request.getMemSize() != null || request.getNumCPUs() != null) {
            CloudImageRequirement oldCir = cloudImage.getRequirement();
            if (oldCir == null) {
                oldCir = new CloudImageRequirement();
                cloudImage.setRequirement(oldCir);
            }
            ReflectionUtil.mergeObject(request, oldCir);
        }
        cloudImageService.saveCloudImage(cloudImage);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Delete an instance of a cloud image.
     *
     * @param id Id of the cloud image to delete.
     */
    @ApiOperation(value = "Delete an existing cloud image. The operation fails in case a cloud is still using the image.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> delete(@ApiParam(value = "Id of the cloud image to delete.", required = true) @PathVariable String id) {
        String[] cloudsUsingImage = cloudImageService.getCloudsUsingImage(id);
        if (cloudsUsingImage.length > 0) {
            throw new DeleteReferencedObjectException("Trying to delete a cloud image which is still used by following clouds "
                    + Sets.newHashSet(cloudsUsingImage));
        }
        cloudImageService.deleteCloudImage(id);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Get details for a cloud image.
     *
     * @param id Id of the cloud to delete.
     */
    @ApiOperation(value = "Get details of a cloud image.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<CloudImage> get(@ApiParam(value = "Id of the cloud image for which to get details.", required = true) @PathVariable String id) {
        return RestResponseBuilder.<CloudImage> builder().data(cloudImageService.getCloudImageFailIfNotExist(id)).build();
    }

    /**
     * Search for cloud images.
     *
     * @param searchRequest Query to find the cloud images.
     * @return A {@link RestResponse} that contains a {@link alien4cloud.dao.model.GetMultipleDataResult} that contains the clouds.
     */
    @ApiOperation(value = "Search for cloud images.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/search", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<GetMultipleDataResult> search(@RequestBody CloudImageSearchRequest searchRequest) {
        GetMultipleDataResult result = cloudImageService.get(searchRequest.getQuery(), searchRequest.getExclude(), searchRequest.getFrom(),
                searchRequest.getSize());
        return RestResponseBuilder.<GetMultipleDataResult> builder().data(result).build();
    }

    /**
     * Update cloud image's icon.
     *
     * @param id The cloud image id.
     * @param image new icon of the cloud image.
     * @return nothing if success, error will be handled in global exception strategy
     */
    @ApiOperation(value = "Updates the icon for the cloud image.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}/icon", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<String> updateImage(@PathVariable String id, @RequestParam("file") MultipartFile image) {
        CloudImage cloudImage = cloudImageService.getCloudImageFailIfNotExist(id);
        String iconId;
        try {
            iconId = imageDAO.writeImage(image.getBytes());
        } catch (IOException e) {
            throw new ImageUploadException("Unable to read image from file upload [" + image.getOriginalFilename() + "] to update cloud image [" + id + "]", e);
        }
        cloudImage.setIconId(iconId);
        cloudImageService.saveCloudImage(cloudImage);
        return RestResponseBuilder.<String> builder().data(iconId).build();
    }

    /**
     * Search for cloud names linked to this image.
     *
     * @param id The cloud image id.
     * @return A {@link RestResponse} that contains a <code>String</code> array (cloud names).
     */
    @ApiOperation(value = "Get the cloud names related to this image.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}/clouds", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<String[]> getCloudImageClouds(@PathVariable String id) {
        String[] cloudNames = cloudImageService.getCloudsNameUsingImage(id);
        return RestResponseBuilder.<String[]> builder().data(cloudNames).build();
    }
}
