package alien4cloud.it;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = { "classpath:alien/rest/application/crud_application_environment.feature" }, format = { "pretty",
        "html:target/cucumber/application", "json:target/cucumber/cucumber-application.json" })
public class RunApplicationIT {
}
