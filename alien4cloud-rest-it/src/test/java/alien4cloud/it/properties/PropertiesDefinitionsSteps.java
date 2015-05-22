package alien4cloud.it.properties;

import alien4cloud.it.Context;
import alien4cloud.model.common.MetaPropConfiguration;
import alien4cloud.rest.internal.PropertyRequest;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.java.en.When;

public class PropertiesDefinitionsSteps {

    @When("^I fill the value \"([^\"]*)\" for \"([^\"]*)\" tag to check$")
    public void I_fill_the_value_for_tag(String value, String configurationTagName) throws Throwable {
        MetaPropConfiguration tag = Context.getInstance().getConfigurationTag(configurationTagName);
        PropertyRequest propertyCheckRequest = new PropertyRequest(configurationTagName, value, tag);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/properties/check", JsonUtil.toString(propertyCheckRequest)));
    }

}
