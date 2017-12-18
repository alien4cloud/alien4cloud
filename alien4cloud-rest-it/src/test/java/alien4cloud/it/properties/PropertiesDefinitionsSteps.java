package alien4cloud.it.properties;

import alien4cloud.it.Context;
import alien4cloud.model.common.MetaPropConfiguration;
import alien4cloud.rest.internal.model.PropertyValidationRequest;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.java.en.When;

public class PropertiesDefinitionsSteps {

    @When("^I fill the value \"([^\"]*)\" for \"([^\"]*)\" tag to check$")
    public void I_fill_the_value_for_tag(String value, String configurationTagName) throws Throwable {
        MetaPropConfiguration tag = Context.getInstance().getConfigurationTag(configurationTagName);
        PropertyValidationRequest propertyCheckRequest = new PropertyValidationRequest(value, configurationTagName, tag, null);
        Context.getInstance()
                .registerRestResponse(Context.getRestClientInstance().postJSon("/rest/v1/properties/check", JsonUtil.toString(propertyCheckRequest)));
    }
}
