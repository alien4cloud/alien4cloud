package alien4cloud.it;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = { "classpath:alien/rest/topology" }, format = { "pretty", "html:target/cucumber/topology", "json:target/cucumber/cucumber-topology.json" })
public class RunTopologyIT {

}
