package alien4cloud.it;

import org.junit.Ignore;
import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = {
        //
        "classpath:alien/rest/application-version"
        // "classpath:alien/rest/application-version/create_application_version.feature"
        // "classpath:alien/rest/application-version/delete_application_version.feature"
        // "classpath:alien/rest/application-version/search_application_version.feature"
        // "classpath:alien/rest/application-version/security_application_version.feature"
        // "classpath:alien/rest/application-version/update_application_version.feature"
        //
}, format = { "pretty", "html:target/cucumber/application-version", "json:target/cucumber/cucumber-application-version.json" })
public class RunApplicationVersionIT {
}
