package alien4cloud.it;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = { "classpath:alien/rest/tags" }, format = { "pretty", "html:target/cucumber/tags", "json:target/cucumber/cucumber-tags.json" })
public class RunTagConfigurationIT {
}
