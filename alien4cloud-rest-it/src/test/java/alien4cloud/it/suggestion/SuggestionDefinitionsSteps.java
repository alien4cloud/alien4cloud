package alien4cloud.it.suggestion;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Assert;

import alien4cloud.it.Context;
import alien4cloud.it.components.AddCommponentDefinitionSteps;
import alien4cloud.model.common.SuggestionEntry;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class SuggestionDefinitionsSteps {

    @When("^I ask suggestions for tag \"([^\"]*)\" with \"([^\"]*)\"$")
    public void I_ask_suggestions_for_tag_with(String path, String searchText) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/suggest/tag/" + path + "/" + searchText));
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

    @Given("^I already had a component \"([^\"]*)\" uploaded$")
    public void I_already_had_a_component_uploaded(String componentName) throws Throwable {
        new AddCommponentDefinitionSteps().uploadComponent(componentName);
    }

    @When("^I search for suggestion on index \"([^\"]*)\", type \"([^\"]*)\", path \"([^\"]*)\" with text \"([^\"]*)\"$")
    public void I_search_for_suggestion_on_index_type_path_with_text(String index, String type, String path, String text) throws Throwable {
        String suggestionsText = Context.getRestClientInstance().get("/rest/v1/suggestions/" + index + "/" + type + "/" + path + "?text=" + text);
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

    @When("^I get all suggestions for property \"([^\"]*)\" of \"([^\"]*)\" \"([^\"]*)\"$")
    public void iGetAllSuggestionsForPropertyOf(String property, String type, String elementId) throws Throwable {
        String suggestionId = SuggestionEntry.generateId("toscaelement", "indexed" + type + "type", elementId, property);
        String suggestionsText = Context.getRestClientInstance().get("/rest/v1/suggestions/" + suggestionId);
        Context.getInstance().registerRestResponse(suggestionsText);
    }

    @When("^I get suggestions for text \"([^\"]*)\" for property \"([^\"]*)\" of \"([^\"]*)\" \"([^\"]*)\"$")
    public void iGetSuggestionsForTextForPropertyOf(String input, String property, String type, String elementId) throws Throwable {
        String suggestionId = SuggestionEntry.generateId("toscaelement", "indexed" + type + "type", elementId, property);
        String suggestionsText = Context.getRestClientInstance().getUrlEncoded("/rest/v1/suggestions/" + suggestionId,
                Arrays.<NameValuePair>asList(new BasicNameValuePair("input", input), new BasicNameValuePair("limit", "2")));
        Context.getInstance().registerRestResponse(suggestionsText);
    }

    @When("^I add suggestion \"([^\"]*)\" for property \"([^\"]*)\" of \"([^\"]*)\" \"([^\"]*)\"$")
    public void iAddSuggestionForPropertyOf(String value, String property, String type, String elementId) throws Throwable {
        String suggestionId = SuggestionEntry.generateId("toscaelement", "indexed" + type + "type", elementId, property);
        Context.getRestClientInstance().put("/rest/v1/suggestions/" + suggestionId + "/" + value);
    }

    @And("^I initialize default suggestions entry$")
    public void iInitializeDefaultSuggestionsEntry() throws Throwable {
        Context.getRestClientInstance().postUrlEncoded("/rest/v1/suggestions/init", new ArrayList<NameValuePair>());
    }

    @And("^The RestResponse should contain (\\d+) element\\(s\\) in this order:$")
    public void theRestResponseShouldContainElementSInThisOrder(int numberOfElements, List<String> expectedSuggestions) throws Throwable {
        String[] suggestions = JsonUtil.read(Context.getInstance().getRestResponse(), String[].class).getData();
        Assert.assertEquals(numberOfElements, suggestions.length);
        String[] expectedSuggestionsArray = expectedSuggestions.toArray(new String[expectedSuggestions.size()]);
        Assert.assertArrayEquals(expectedSuggestionsArray, suggestions);
    }
}
