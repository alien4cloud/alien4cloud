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
        PropertyValidationRequest propertyCheckRequest = new PropertyValidationRequest(configurationTagName, value, tag);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/properties/check", JsonUtil.toString(propertyCheckRequest)));
    }

    @When("^I set the value \"([^\"]*)\" for the cloud meta-property \"([^\"]*)\" of the cloud \"([^\"]*)\"$")
    public void I_set_the_value_for_the_cloud_meta_property_of_the_cloud(String value, String metaPropertyName, String cloudName) throws Throwable {
        MetaPropConfiguration propertyDefinition = Context.getInstance().getConfigurationTag(metaPropertyName);
        PropertyValidationRequest propertyCheckRequest = new PropertyValidationRequest(value, propertyDefinition.getId(), propertyDefinition);
        String cloudId = Context.getInstance().getCloudId(cloudName);
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postJSon("/rest/clouds/" + cloudId + "/properties", JsonUtil.toString(propertyCheckRequest)));
    }

}
