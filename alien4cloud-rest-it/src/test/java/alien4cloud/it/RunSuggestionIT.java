package alien4cloud.it;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = { "classpath:alien/rest/suggestion" }, format = { "pretty", "html:target/cucumber/suggestion", "json:target/cucumber/cucumber-suggestion.json" })
public class RunSuggestionIT {
}
