package alien4cloud.tosca.parser;

import alien4cloud.tosca.model.ArchiveRoot;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Paths;

public class ToscaParserAlien200Test extends AbstractToscaParserSimpleProfileTest {
    @Override
    protected String getRootDirectory() {
        return "src/test/resources/tosca/alien_dsl_2_0_0/";
    }

    @Override
    protected String getToscaVersion() {
        return "alien_dsl_2_0_0";
    }

    @Test
    public void parseTopologyTemplateWithActivities() throws ParsingException, IOException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-topology-template-activities.yml"));

        Assert.assertFalse(parsingResult.getResult().getTopology().getWorkflows().isEmpty());
        Assert.assertTrue(parsingResult.getResult().getTopology().getWorkflows().get("install") != null);

        Workflow wf = parsingResult.getResult().getTopology().getWorkflows().get("install");

        // check activities
        Assert.assertTrue(wf.getSteps().get("Compute_install").getActivities().size() == 1);
        Assert.assertTrue(wf.getSteps().get("Compute_install_0").getActivities().size() == 1);
        Assert.assertTrue(wf.getSteps().get("Compute_install_1").getActivities().size() == 1);
        Assert.assertTrue(wf.getSteps().get("Compute_install").getOnSuccess().size() == 2);

        // check onSuccess
        Assert.assertTrue(wf.getSteps().get("Compute_install").getOnSuccess().contains("Compute_uninstall"));
        Assert.assertTrue(wf.getSteps().get("Compute_install").getOnSuccess().contains("Compute_uninstall_0"));
        Assert.assertTrue(wf.getSteps().get("Compute_install_1").getOnSuccess().contains("Compute_uninstall"));
        Assert.assertTrue(wf.getSteps().get("Compute_install_1").getOnSuccess().contains("Compute_uninstall_0"));

        // check activities of an other step
        Assert.assertTrue(wf.getSteps().get("Compute_uninstall").getActivities().size() == 1);
        Assert.assertTrue(wf.getSteps().get("Compute_uninstall_0").getActivities().size() == 1);
    }

}