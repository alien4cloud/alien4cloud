package alien4cloud.it;

import org.junit.Ignore;
import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = {
        //
        "classpath:alien/rest/application"
        // "classpath:alien/rest/application/create_application.feature"
        // "classpath:alien/rest/application/create_application_with_template.feature"
        // "classpath:alien/rest/application/delete_application.feature"
        //
}, format = { "pretty", "html:target/cucumber/application", "json:target/cucumber/cucumber-application.json" })
// @Ignore
public class RunApplicationIT {
}