package alien4cloud.it;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = {
//
"classpath:alien/rest/cloud"
//"classpath:alien/rest/cloud/get_cloud_configuration.feature",
//"classpath:alien/rest/cloud/get_cloud_deployment_properties.feature"
// "classpath:alien/rest/cloud/update_cloud.feature"
//
}, format = { "pretty", "html:target/cucumber/cloud", "json:target/cucumber/cucumber-cloud.json" })
public class RunCloudIT {
}
