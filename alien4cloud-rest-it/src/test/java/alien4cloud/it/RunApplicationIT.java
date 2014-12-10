package alien4cloud.it;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = { "classpath:alien/rest/application"
// "classpath:alien/rest/application/create_application.feature"
// "classpath:alien/rest/application/application_environment_crud.feature"
}, format = { "pretty", "html:target/cucumber/application", "json:target/cucumber/cucumber-application.json" })
public class RunApplicationIT {
}
