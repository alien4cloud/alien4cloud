package alien4cloud.it;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = {   "classpath:alien/rest/application-version/application_version_crud.feature"
}, format = { "pretty", "html:target/cucumber/application-version", "json:target/cucumber/cucumber-application-version.json" })
public class RunApplicationVersionIT {
}
