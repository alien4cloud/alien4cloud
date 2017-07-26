package alien4cloud.deployment.matching.services.nodes;

import alien4cloud.model.deployment.matching.MatchingConfiguration;
import alien4cloud.tosca.parser.ParsingException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.io.FileNotFoundException;
import java.nio.file.Paths;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:matching/parser-application-context.xml")
@DirtiesContext
public class MatchingConfigurationsParserTest {
    @Inject
    private MatchingConfigurationsParser parser;

    @Test()
    public void testParsing() throws FileNotFoundException, ParsingException {
        MatchingConfigurations configurations = parser.parseFile(Paths.get("src/test/resources/matching/mock-resources-matching-config.yml")).getResult();
        Assert.assertEquals(1, configurations.getMatchingConfigurations().size());
        MatchingConfiguration computeConf = configurations.getMatchingConfigurations().get("org.alien4cloud.nodes.mock.Compute");
        Assert.assertNotNull(computeConf);
        Assert.assertEquals(0, computeConf.getProperties().size());
        Assert.assertEquals(2, computeConf.getCapabilities().size());
        Assert.assertEquals(4, computeConf.getCapabilities().get("host").getProperties().size());
    }
}