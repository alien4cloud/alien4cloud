package alien4cloud.it;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = {
        "classpath:alien/rest/application-deployment-logs/"
}, format = { "pretty", "html:target/cucumber/application_deployment_logs", "json:target/cucumber/cucumber-application-deployment-logs.json" })
// @Ignore
public class RunApplicationDeploymentLogsIT {
}
