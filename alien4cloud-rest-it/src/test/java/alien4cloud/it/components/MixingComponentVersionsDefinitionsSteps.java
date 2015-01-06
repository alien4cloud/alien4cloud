package alien4cloud.it.components;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;

import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.it.Context;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;

import com.google.common.collect.Sets;

import cucumber.api.java.en.Then;

public class MixingComponentVersionsDefinitionsSteps {

    @Then("^The response should contains (\\d+) elements from various types of version \"([^\"]*)\" and older versions are$")
    public void The_response_should_contains_elements_from_various_types_of_version_and_older_versions_are(int expectedElementSize, String expectedVersion,
            List<String> expectedOlderVersions) throws Throwable {
        Set<String> expectedOlderVersionsSet = Sets.newHashSet(expectedOlderVersions);
        SearchDefinitionSteps searchDefinitionSteps = new SearchDefinitionSteps();
        searchDefinitionSteps.The_response_should_contains_elements_from_various_types(expectedElementSize);
        RestResponse<FacetedSearchResult> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), FacetedSearchResult.class);
        FacetedSearchResult searchResp = restResponse.getData();
        Object[] elements = searchResp.getData();
        for (int i = 0; i < expectedElementSize; i++) {
            Map<String, Object> element = (Map<String, Object>) elements[i];
            Assert.assertEquals(expectedVersion, element.get("archiveVersion"));
            List<Object> actualOlderVersions = (List<Object>) element.get("olderVersions");
            if (expectedOlderVersions == null || expectedOlderVersions.isEmpty()) {
                Assert.assertTrue(actualOlderVersions == null || actualOlderVersions.isEmpty());
            } else {
                Assert.assertEquals(expectedOlderVersionsSet, Sets.newHashSet(actualOlderVersions));
            }
        }
    }

}
