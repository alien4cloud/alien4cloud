package alien4cloud.tosca.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Set;

import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.types.PolicyType;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.alien4cloud.tosca.model.workflow.activities.InlineWorkflowActivity;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import alien4cloud.tosca.model.ArchiveRoot;

/**
 * Created by lucboutier on 12/04/2017.
 */

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
    public void testPolicyParsing() throws FileNotFoundException, ParsingException {
        Mockito.reset(csarRepositorySearchService);
        Mockito.when(csarRepositorySearchService.getArchive("tosca-normative-types", "1.0.0-ALIEN14")).thenReturn(Mockito.mock(Csar.class));
        PolicyType mockRoot = Mockito.mock(PolicyType.class);
        Mockito.when(mockRoot.isAbstract()).thenReturn(true);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(PolicyType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(Set.class)))
                .thenReturn(mockRoot);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-policy-type.yml"));
        assertEquals(0, parsingResult.getContext().getParsingErrors().size());

        ArchiveRoot archiveRoot = parsingResult.getResult();
        assertNotNull(archiveRoot.getArchive());
        assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        assertEquals(3, archiveRoot.getPolicyTypes().size());

        PolicyType minPolicyType = archiveRoot.getPolicyTypes().get("org.alien4cloud.sample.MinimalPolicyType");
        assertNotNull(minPolicyType);
        assertEquals("org.alien4cloud.sample.MinimalPolicyType", minPolicyType.getElementId());
        assertEquals("This is a sample policy type with minimal definition", minPolicyType.getDescription());
        assertEquals(1, minPolicyType.getDerivedFrom().size());
        assertEquals("tosca.policies.Root", minPolicyType.getDerivedFrom().get(0));

        PolicyType simplePolicyType = archiveRoot.getPolicyTypes().get("org.alien4cloud.sample.SimpleConditionPolicyType");
        assertNotNull(simplePolicyType);
        assertEquals("org.alien4cloud.sample.SimpleConditionPolicyType", simplePolicyType.getElementId());
        assertEquals("This is a sample policy type with simple definition", simplePolicyType.getDescription());
        assertEquals(1, simplePolicyType.getDerivedFrom().size());
        assertEquals("tosca.policies.Root", simplePolicyType.getDerivedFrom().get(0));
        assertEquals(2, simplePolicyType.getTags().size());
        assertEquals("sample_meta", simplePolicyType.getTags().get(0).getName());
        assertEquals("a meta data", simplePolicyType.getTags().get(0).getValue());
        assertEquals("anoter_meta", simplePolicyType.getTags().get(1).getName());
        assertEquals("another meta data", simplePolicyType.getTags().get(1).getValue());
        assertEquals(1, simplePolicyType.getProperties().size());
        assertNotNull(simplePolicyType.getProperties().get("sample_property"));
        assertEquals("string", simplePolicyType.getProperties().get("sample_property").getType());
        assertEquals(2, simplePolicyType.getTargets().size());
        assertEquals("tosca.nodes.Compute", simplePolicyType.getTargets().get(0));
        assertEquals("org.alien4cloud.Group", simplePolicyType.getTargets().get(1));

        PolicyType policyType = archiveRoot.getPolicyTypes().get("org.alien4cloud.sample.PolicyType");
        assertNotNull(policyType);
        assertEquals("org.alien4cloud.sample.PolicyType", policyType.getElementId());
        assertEquals("This is a sample policy type", policyType.getDescription());
        assertEquals(1, policyType.getDerivedFrom().size());
        assertEquals("tosca.policies.Root", policyType.getDerivedFrom().get(0));
        assertEquals(2, policyType.getTags().size());
        assertEquals("sample_meta", policyType.getTags().get(0).getName());
        assertEquals("a meta data", policyType.getTags().get(0).getValue());
        assertEquals("anoter_meta", policyType.getTags().get(1).getName());
        assertEquals("another meta data", policyType.getTags().get(1).getValue());
        assertEquals(1, simplePolicyType.getProperties().size());
        assertNotNull(simplePolicyType.getProperties().get("sample_property"));
        assertEquals("string", simplePolicyType.getProperties().get("sample_property").getType());
        assertEquals(2, simplePolicyType.getTargets().size());
        assertEquals("tosca.nodes.Compute", simplePolicyType.getTargets().get(0));
        assertEquals("org.alien4cloud.Group", simplePolicyType.getTargets().get(1));
    }

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

    @Test
    public void parseTopologyTemplateWithInlineWorkflow() throws ParsingException, IOException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-topology-template-inline-workflow.yml"));

        Assert.assertFalse(parsingResult.getResult().getTopology().getWorkflows().isEmpty());
        Assert.assertTrue(parsingResult.getResult().getTopology().getWorkflows().get("install") != null);

        Workflow wf = parsingResult.getResult().getTopology().getWorkflows().get("install");
        Assert.assertTrue(wf.getSteps().get("Compute_install_0").getActivities().size() == 1);
        Assert.assertTrue(wf.getSteps().get("Compute_install_0").getActivities().get(0) instanceof InlineWorkflowActivity);
        InlineWorkflowActivity activity = (InlineWorkflowActivity) wf.getSteps().get("Compute_install_0").getActivities().get(0);
        Assert.assertTrue(activity.getInline().equals("my_custom_wf"));
    }
}