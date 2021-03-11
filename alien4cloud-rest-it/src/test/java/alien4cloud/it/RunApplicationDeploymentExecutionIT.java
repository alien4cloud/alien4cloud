package alien4cloud.it;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = {
        "classpath:alien/rest/application-deployment-execution/"
}, format = { "pretty", "html:target/cucumber/application_deployment_execution", "json:target/cucumber/cucumber-application-deployment-execution.json" })
// @Ignore
public class RunApplicationDeploymentExecutionIT {
}
