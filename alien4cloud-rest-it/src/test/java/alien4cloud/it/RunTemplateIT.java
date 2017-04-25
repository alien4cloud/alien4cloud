package alien4cloud.it;

import org.junit.Ignore;
import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = {
        //
        "classpath:alien/rest/template"
        // "classpath:alien/rest/template/topology_template_versions.feature"
        //
}, format = { "pretty", "html:target/cucumber/template", "json:target/cucumber/cucumber-template.json" })
// @Ignore
public class RunTemplateIT {
}
