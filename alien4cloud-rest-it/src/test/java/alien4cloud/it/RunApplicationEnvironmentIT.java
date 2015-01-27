package alien4cloud.it;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = {
//
"classpath:alien/rest/application-environment"
// "classpath:alien/rest/application/application_environment_update.feature"
// "classpath:alien/rest/application/application_environment_create.feature"
//
}, format = { "pretty", "html:target/cucumber/application-environment", "json:target/cucumber/cucumber-application-environment.json" })
public class RunApplicationEnvironmentIT {
}
