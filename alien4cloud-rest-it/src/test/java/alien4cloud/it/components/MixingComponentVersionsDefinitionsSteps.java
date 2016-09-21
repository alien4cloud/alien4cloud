package alien4cloud.it.components;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alien4cloud.tosca.catalog.CatalogVersionResult;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;

import com.google.common.collect.Sets;

import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.it.Context;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.java.en.Then;

public class MixingComponentVersionsDefinitionsSteps {

    @Then("^The response should contains (\\d+) elements from various types of version \"([^\"]*)\" and older versions are$")
    public void The_response_should_contains_elements_from_various_types_of_version_and_older_versions_are(int expectedElementSize, String expectedVersion,
            List<String> expectedOlderVersions) throws Throwable {
        Set<String> expectedOlderVersionsSet = (expectedOlderVersions == null) ? null : Sets.newHashSet(expectedOlderVersions);
        SearchDefinitionSteps searchDefinitionSteps = new SearchDefinitionSteps();
        searchDefinitionSteps.The_response_should_contains_elements_from_various_types(expectedElementSize);
        RestResponse<FacetedSearchResult> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), FacetedSearchResult.class);
        FacetedSearchResult searchResp = restResponse.getData();
        Object[] elements = searchResp.getData();
        for (int i = 0; i < expectedElementSize; i++) {
            Map<String, Object> element = (Map<String, Object>) elements[i];
            Assert.assertEquals(expectedVersion, element.get("archiveVersion"));
            Set<String> actualOlderVersions = getComponentVersions((String) element.get("elementId"));
            actualOlderVersions.remove(expectedVersion);
            if (CollectionUtils.isEmpty(expectedOlderVersions)) {
                Assert.assertTrue(actualOlderVersions.isEmpty());
            } else {
                Assert.assertEquals(expectedOlderVersionsSet, actualOlderVersions);
            }
        }
    }

    private Set<String> getComponentVersions(String elementId) throws Throwable {
        Set<String> versions = Sets.newHashSet();
        String responseString = Context.getRestClientInstance().get("/rest/v1/components/element/" + elementId + "/versions");
        RestResponse<?> response = JsonUtil.read(responseString);
        List<CatalogVersionResult> versionResults = JsonUtil.toList(JsonUtil.toString(response.getData()), CatalogVersionResult.class);
        for (CatalogVersionResult versionResult : versionResults) {
            versions.add(versionResult.getVersion());
        }
        return versions;
    }

    @Then("^The response should contains (\\d+) elements from various types of version \"([^\"]*)\"$")
    public void The_response_should_contains_elements_from_various_types_of_version(int expectedElementSize, String expectedVersion) throws Throwable {
        The_response_should_contains_elements_from_various_types_of_version_and_older_versions_are(expectedElementSize, expectedVersion, null);
    }

}
