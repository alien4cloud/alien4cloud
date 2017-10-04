package alien4cloud.it.orchestrators;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;

import com.google.common.collect.Lists;

import alien4cloud.it.Context;
import alien4cloud.model.orchestrators.locations.AbstractLocationResourceTemplate;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplateWithDependencies;
import alien4cloud.model.orchestrators.locations.LocationResources;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.orchestrator.model.CreateLocationResourceTemplateRequest;
import alien4cloud.rest.orchestrator.model.LocationDTO;
import alien4cloud.rest.orchestrator.model.UpdateLocationResourceTemplatePropertyRequest;
import alien4cloud.rest.orchestrator.model.UpdateLocationResourceTemplateRequest;
import alien4cloud.rest.utils.JsonUtil;

public abstract class AbstractLocationResourceSteps {

    public void createResourceTemplate(String resourceType, String resourceName, String archiveName, String archiveVersion, String orchestratorName,
            String locationName) throws IOException {
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
        String locationId = Context.getInstance().getLocationId(orchestratorId, locationName);
        String restUrl = String.format(getBaseUrlFormat(), orchestratorId, locationId);
        CreateLocationResourceTemplateRequest request = new CreateLocationResourceTemplateRequest();
        request.setResourceName(resourceName);
        request.setResourceType(resourceType);
        request.setArchiveName(archiveName);
        request.setArchiveVersion(archiveVersion);
        String resp = Context.getRestClientInstance().postJSon(restUrl, JsonUtil.toString(request));

        RestResponse<LocationResourceTemplateWithDependencies> response = JsonUtil.read(resp, LocationResourceTemplateWithDependencies.class,
                Context.getJsonMapper());
        if (response.getError() == null && response.getData() != null) {
            Context.getInstance().registerOrchestratorLocationResource(orchestratorId, locationId, response.getData().getResourceTemplate().getId(),
                    resourceName);
        }
        Context.getInstance().registerRestResponse(resp);
    }

    protected void locationShouldContainResource(String resourceName, String resourceType, IResourceAccessor resourceAccessor) throws Throwable {
        boolean found = resourceFoundInLocation(resourceName, resourceType, resourceAccessor);
        Assert.assertTrue(found);
    }

    protected void locationShouldNotContainResource(String resourceName, String resourceType, IResourceAccessor resourceAccessor) throws Throwable {
        boolean found = resourceFoundInLocation(resourceName, resourceType, resourceAccessor);
        Assert.assertFalse(found);
    }

    private boolean resourceFoundInLocation(String resourceName, String resourceType, IResourceAccessor resourceAccessor) throws IOException {
        String restResponse = Context.getInstance().getRestResponse();
        RestResponse<LocationDTO> response = JsonUtil.read(restResponse, LocationDTO.class, Context.getJsonMapper());
        LocationDTO locationDTO = response.getData();
        boolean found = false;
        final List<? extends AbstractLocationResourceTemplate> templates = resourceAccessor.getResources(locationDTO.getResources());
        for (AbstractLocationResourceTemplate lrt : templates) {
            if (lrt.getName().equals(resourceName) && lrt.getTypes().contains(resourceType)) {
                found = true;
                break;
            }
        }
        return found;
    }

    protected void deleteResourceTemplate(String resourceName, String orchestratorName, String locationName) throws IOException {
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
        String locationId = Context.getInstance().getLocationId(orchestratorId, locationName);
        String resourceId = Context.getInstance().getLocationResourceId(orchestratorId, locationId, resourceName);
        String restUrl = String.format(getBaseUrlFormat() + "/%s", orchestratorId, locationId, resourceId);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete(restUrl));
    }

    public void updatePropertyValue(String orchestratorName, String locationName, String resourceName, String propertyName, Object propertyValue,
            String endpoint, String... extraArgs) throws IOException {
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
        String locationId = Context.getInstance().getLocationId(orchestratorId, locationName);
        String resourceId = Context.getInstance().getLocationResourceId(orchestratorId, locationId, resourceName);
        String restUrl;
        if (extraArgs.length > 0) {
            List<String> args = Lists.newArrayList(orchestratorId, locationId, resourceId);
            args.addAll(Arrays.asList(extraArgs));
            restUrl = String.format(endpoint, args.toArray());
        } else {
            restUrl = String.format(endpoint, orchestratorId, locationId, resourceId);
        }
        UpdateLocationResourceTemplatePropertyRequest request = new UpdateLocationResourceTemplatePropertyRequest();
        request.setPropertyName(propertyName);
        request.setPropertyValue(propertyValue);
        String resp = Context.getRestClientInstance().postJSon(restUrl, JsonUtil.toString(request));
        Context.getInstance().registerRestResponse(resp);
    }

    protected void updateLocationResource(String orchestratorName, String locationName, String oldName, String newName) throws IOException {
        String orchestratorId = Context.getInstance().getOrchestratorId(orchestratorName);
        String locationId = Context.getInstance().getLocationId(orchestratorId, locationName);
        String resourceId = Context.getInstance().getLocationResourceId(orchestratorId, locationId, oldName);

        UpdateLocationResourceTemplateRequest request = new UpdateLocationResourceTemplateRequest();
        request.setName(newName);

        String restUrl = String.format(getBaseUrlFormat() + "/%s", orchestratorId, locationId, resourceId);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().putJSon(restUrl, JsonUtil.toString(request)));
    }

    protected abstract String getBaseUrlFormat();

    protected String getUpdatePropertyUrlFormat() {
        return getBaseUrlFormat() + "/%s/template/properties";
    }

    protected interface IResourceAccessor {
        List<? extends AbstractLocationResourceTemplate> getResources(LocationResources locationResources);
    }
}
