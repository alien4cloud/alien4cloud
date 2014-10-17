package alien4cloud.it;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = { "classpath:alien/rest/csars" }, format = { "pretty", "html:target/cucumber/csars", "json:target/cucumber/cucumber-csars.json" })
public class RunCsarIT {
}
