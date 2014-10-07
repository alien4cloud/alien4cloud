package alien4cloud.it.suggestion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;

import alien4cloud.it.Context;
import alien4cloud.it.components.AddCommponentDefinitionSteps;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class SuggestionDefinitionsSteps {

    @When("^I ask suggestions for tag \"([^\"]*)\" with \"([^\"]*)\"$")
    public void I_ask_suggestions_for_tag_with(String path, String searchText) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/suggest/tag/" + path + "/" + searchText));
    }

    @Then("^The suggestion response should contains (\\d+) elements$")
    public void The_suggestion_response_should_contains_elements(int expectedSize) throws Throwable {
        RestResponse<String[]> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), String[].class);
        String[] suggestionResp = restResponse.getData();

        assertNotNull(suggestionResp);
        assertEquals(expectedSize, suggestionResp.length);
    }

    @Then("^The suggestion response should contains \"([^\"]*)\"$")
    public void The_suggestion_response_should_contains(String expectedValue) throws Throwable {
        RestResponse<String[]> restResponse = JsonUtil.read(Context.getInstance().takeRestResponse(), String[].class);
        String[] suggestionResp = restResponse.getData();

        assertNotNull(suggestionResp);
        assertTrue(Arrays.asList(suggestionResp).contains(expectedValue));
    }

    private AddCommponentDefinitionSteps addCommponentDefinitionSteps = new AddCommponentDefinitionSteps();

    @Given("^I already had a component \"([^\"]*)\" uploaded$")
    public void I_already_had_a_component_uploaded(String componentName) throws Throwable {
        addCommponentDefinitionSteps.uploadComponent(componentName);
    }

    @When("^I search for suggestion on index \"([^\"]*)\", type \"([^\"]*)\", path \"([^\"]*)\" with text \"([^\"]*)\"$")
    public void I_search_for_suggestion_on_index_type_path_with_text(String index, String type, String path, String text) throws Throwable {
        String suggestionsText = Context.getRestClientInstance().get("/rest/suggestions/" + index + "/" + type + "/" + path + "?text=" + text);
        Context.getInstance().registerRestResponse(suggestionsText);
    }

    @Then("^The RestResponse should contain (\\d+) element\\(s\\):$")
    public void The_RestResponse_should_contain_element_s_(int numberOfElements, List<String> expectedSuggestions) throws Throwable {
        String[] suggestions = JsonUtil.read(Context.getInstance().getRestResponse(), String[].class).getData();
        Assert.assertEquals(numberOfElements, suggestions.length);
        Arrays.sort(suggestions, 0, suggestions.length);
        String[] expectedSuggestionsArray = expectedSuggestions.toArray(new String[expectedSuggestions.size()]);
        Arrays.sort(expectedSuggestionsArray, 0, expectedSuggestionsArray.length);
        Assert.assertArrayEquals(expectedSuggestionsArray, suggestions);
    }
}
