package alien4cloud.it;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = {
        //
        "classpath:alien/rest/suggestion"
        // "classpath:alien/rest/suggestion/nodetype_suggestion.feature"
        // "classpath:alien/rest/suggestion/property_suggestion.feature"
        // "classpath:alien/rest/suggestion/tag_suggestion.feature"
        //
}, format = { "pretty", "html:target/cucumber/suggestion", "json:target/cucumber/cucumber-suggestion.json" })
// @Ignore
public class RunSuggestionIT {
}
