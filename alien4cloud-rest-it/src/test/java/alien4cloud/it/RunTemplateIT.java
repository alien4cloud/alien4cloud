package alien4cloud.it;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = { "classpath:alien/rest/template" }, format = { "pretty", "html:target/cucumber/template", "json:target/cucumber/cucumber-template.json" })
public class RunTemplateIT {
}
