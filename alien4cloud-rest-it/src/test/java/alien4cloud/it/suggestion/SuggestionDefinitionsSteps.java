package alien4cloud.it.suggestion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.alien4cloud.tosca.model.types.ArtifactType;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Assert;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.dao.ElasticSearchDAO;
import alien4cloud.it.Context;
import alien4cloud.model.common.SuggestionEntry;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.suggestion.CreateSuggestionEntryRequest;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class SuggestionDefinitionsSteps {

    private static final Map<String, String> toESType = Maps.newHashMap();

    static {
        toESType.put("node", NodeType.class.getSimpleName().toLowerCase());
        toESType.put("artifact", ArtifactType.class.getSimpleName().toLowerCase());
        toESType.put("capability", CapabilityType.class.getSimpleName().toLowerCase());
        toESType.put("relationship", RelationshipType.class.getSimpleName().toLowerCase());
    }

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
        String suggestionId = SuggestionEntry.generateId("toscaelement", toESType.get(type), elementId, property);
        String suggestionsText = Context.getRestClientInstance().get("/rest/v1/suggestions/" + suggestionId + "/values");
        Context.getInstance().registerRestResponse(suggestionsText);
    }

    @When("^I get suggestions for text \"([^\"]*)\" for property \"([^\"]*)\" of \"([^\"]*)\" \"([^\"]*)\"$")
    public void iGetSuggestionsForTextForPropertyOf(String input, String property, String type, String elementId) throws Throwable {
        String suggestionId = SuggestionEntry.generateId("toscaelement", toESType.get(type), elementId, property);
        String suggestionsText = Context.getRestClientInstance().getUrlEncoded("/rest/v1/suggestions/" + suggestionId + "/values",
                Arrays.<NameValuePair> asList(new BasicNameValuePair("input", input), new BasicNameValuePair("limit", "2")));
        Context.getInstance().registerRestResponse(suggestionsText);
    }

    @When("^I add suggestion \"([^\"]*)\" for property \"([^\"]*)\" of \"([^\"]*)\" \"([^\"]*)\"$")
    public void iAddSuggestionForPropertyOf(String value, String property, String type, String elementId) throws Throwable {
        String suggestionId = SuggestionEntry.generateId("toscaelement", toESType.get(type), elementId, property);
        Context.getRestClientInstance().put("/rest/v1/suggestions/" + suggestionId + "/values/" + value);
    }

    @And("^I initialize default suggestions entry$")
    public void iInitializeDefaultSuggestionsEntry() throws Throwable {
        String response = Context.getRestClientInstance().postUrlEncoded("/rest/v1/suggestions/init", new ArrayList<NameValuePair>());
        Assert.assertNull(JsonUtil.read(response).getError());
    }

    @And("^The RestResponse should contain (\\d+) element\\(s\\) in this order:$")
    public void theRestResponseShouldContainElementSInThisOrder(int numberOfElements, List<String> expectedSuggestions) throws Throwable {
        String[] suggestions = JsonUtil.read(Context.getInstance().getRestResponse(), String[].class).getData();
        Assert.assertEquals(numberOfElements, suggestions.length);
        String[] expectedSuggestionsArray = expectedSuggestions.toArray(new String[expectedSuggestions.size()]);
        Assert.assertArrayEquals(expectedSuggestionsArray, suggestions);
    }

    @Given("^I create suggestion for property \"([^\"]*)\" of \"([^\"]*)\" \"([^\"]*)\" with initial values \"([^\"]*)\"$")
    public void iCreateSuggestionForPropertyOfWithInitialValues(String property, String type, String elementId, String initialValues) throws Throwable {
        String response = Context.getRestClientInstance().postJSon(
                "/rest/v1/suggestions/",
                JsonUtil.toString(new CreateSuggestionEntryRequest(ElasticSearchDAO.TOSCA_ELEMENT_INDEX, toESType.get(type), elementId, property,
                        Sets
                        .newHashSet(initialValues.split(",")))));
        Assert.assertNull(JsonUtil.read(response).getError());
    }

    @When("^I ask suggestions for node type with \"([^\"]*)\"$")
    public void iAskSuggestionsForNodeTypeWith(String searchText) throws Throwable {
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().getUrlEncoded("/rest/v1/suggest/nodetypes", Lists.newArrayList(new BasicNameValuePair("text", searchText))));
    }
}
