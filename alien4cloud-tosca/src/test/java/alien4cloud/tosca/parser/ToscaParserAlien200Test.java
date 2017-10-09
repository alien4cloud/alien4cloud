package alien4cloud.tosca.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Set;

import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.PolicyType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.model.workflow.NodeWorkflowStep;
import org.alien4cloud.tosca.model.workflow.RelationshipWorkflowStep;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.alien4cloud.tosca.model.workflow.WorkflowStep;
import org.alien4cloud.tosca.model.workflow.activities.InlineWorkflowActivity;
import org.alien4cloud.tosca.normative.constants.NormativeComputeConstants;
import org.alien4cloud.tosca.normative.constants.NormativeRelationshipConstants;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.Lists;

import alien4cloud.paas.wf.util.WorkflowUtils;
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
    public void testPolicyTypeParsing() throws FileNotFoundException, ParsingException {
        Mockito.reset(csarRepositorySearchService);
        Mockito.when(csarRepositorySearchService.getArchive("tosca-normative-types", "1.0.0-ALIEN14")).thenReturn(Mockito.mock(Csar.class));
        PolicyType mockRoot = Mockito.mock(PolicyType.class);
        Mockito.when(mockRoot.isAbstract()).thenReturn(true);
        Mockito.when(
                csarRepositorySearchService.getElementInDependencies(Mockito.eq(PolicyType.class), Mockito.eq("tosca.policies.Root"), Mockito.any(Set.class)))
                .thenReturn(mockRoot);

        NodeType mockType = Mockito.mock(NodeType.class);
        Mockito.when(mockType.isAbstract()).thenReturn(true);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(Set.class)))
                .thenReturn(mockType);
        Mockito.when(
                csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Compute"), Mockito.any(Set.class)))
                .thenReturn(mockType);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-policy-type.yml"));
        assertEquals(0, parsingResult.getContext().getParsingErrors().size());

        ArchiveRoot archiveRoot = parsingResult.getResult();
        assertNotNull(archiveRoot.getArchive());
        assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        assertEquals(3, archiveRoot.getPolicyTypes().size());

        PolicyType minPolicyType = archiveRoot.getPolicyTypes().get("org.alien4cloud.sample.MinimalPolicyType");
        assertNotNull(minPolicyType);
        assertEquals("org.alien4cloud.test.policies.PolicyTypes", minPolicyType.getArchiveName());
        assertEquals("2.0.0-SNAPSHOT", minPolicyType.getArchiveVersion());
        assertEquals("org.alien4cloud.sample.MinimalPolicyType", minPolicyType.getElementId());
        assertEquals("This is a sample policy type with minimal definition", minPolicyType.getDescription());
        assertEquals(1, minPolicyType.getDerivedFrom().size());
        assertEquals("tosca.policies.Root", minPolicyType.getDerivedFrom().get(0));

        PolicyType simplePolicyType = archiveRoot.getPolicyTypes().get("org.alien4cloud.sample.SimpleConditionPolicyType");
        assertNotNull(simplePolicyType);
        assertEquals("org.alien4cloud.test.policies.PolicyTypes", simplePolicyType.getArchiveName());
        assertEquals("2.0.0-SNAPSHOT", simplePolicyType.getArchiveVersion());
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
        assertTrue(simplePolicyType.getTargets().contains("tosca.nodes.Compute"));
        assertTrue(simplePolicyType.getTargets().contains("tosca.nodes.Root"));

        PolicyType policyType = archiveRoot.getPolicyTypes().get("org.alien4cloud.sample.PolicyType");
        assertNotNull(policyType);
        assertEquals("org.alien4cloud.test.policies.PolicyTypes", policyType.getArchiveName());
        assertEquals("2.0.0-SNAPSHOT", policyType.getArchiveVersion());
        assertEquals("org.alien4cloud.sample.PolicyType", policyType.getElementId());
        assertEquals("This is a sample policy type", policyType.getDescription());
        assertEquals(1, policyType.getDerivedFrom().size());
        assertEquals("tosca.policies.Root", policyType.getDerivedFrom().get(0));
        assertEquals(2, policyType.getTags().size());
        assertEquals("sample_meta", policyType.getTags().get(0).getName());
        assertEquals("a meta data", policyType.getTags().get(0).getValue());
        assertEquals("anoter_meta", policyType.getTags().get(1).getName());
        assertEquals("another meta data", policyType.getTags().get(1).getValue());
        assertEquals(1, policyType.getProperties().size());
        assertNotNull(policyType.getProperties().get("sample_property"));
        assertEquals("string", policyType.getProperties().get("sample_property").getType());
        assertEquals(2, policyType.getTargets().size());
        assertTrue(simplePolicyType.getTargets().contains("tosca.nodes.Compute"));
        assertTrue(simplePolicyType.getTargets().contains("tosca.nodes.Root"));
    }

    @Test
    public void policyParsingWithUnknownTypeShouldFail() throws FileNotFoundException, ParsingException {
        Mockito.reset(csarRepositorySearchService);
        Mockito.when(csarRepositorySearchService.getArchive("tosca-normative-types", "1.0.0-ALIEN14")).thenReturn(Mockito.mock(Csar.class));
        PolicyType mockRoot = Mockito.mock(PolicyType.class);
        Mockito.when(mockRoot.isAbstract()).thenReturn(true);
        Mockito.when(
                csarRepositorySearchService.getElementInDependencies(Mockito.eq(PolicyType.class), Mockito.eq("tosca.policies.Root"), Mockito.any(Set.class)))
                .thenReturn(mockRoot);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-policy-type.yml"));
        assertEquals(4, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testPolicyTemplateParsing() throws FileNotFoundException, ParsingException {
        Mockito.reset(csarRepositorySearchService);
        Mockito.when(csarRepositorySearchService.getArchive("tosca-normative-types", "1.0.0-ALIEN14")).thenReturn(Mockito.mock(Csar.class));

        NodeType nodeType = new NodeType();
        nodeType.setElementId("tosca.nodes.Root");
        nodeType.setArchiveName("tosca-normative-types");
        nodeType.setArchiveVersion("1.0.0-ALIEN14");
        nodeType.setDerivedFrom(Lists.newArrayList("tosca.nodes.Root"));
        Mockito.when(
                csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Compute"), Mockito.any(Set.class)))
                .thenReturn(nodeType);

        nodeType = new NodeType();
        nodeType.setElementId("tosca.nodes.Root");
        nodeType.setArchiveName("tosca-normative-types");
        nodeType.setArchiveVersion("1.0.0-ALIEN14");
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(Set.class)))
                .thenReturn(nodeType);

        PolicyType policyType = Mockito.mock(PolicyType.class);
        policyType.setElementId("tosca.nodes.Root");
        policyType.setArchiveName("tosca-normative-types");
        policyType.setArchiveVersion("1.0.0-ALIEN14");
        Mockito.when(policyType.isAbstract()).thenReturn(true);
        Mockito.when(
                csarRepositorySearchService.getElementInDependencies(Mockito.eq(PolicyType.class), Mockito.eq("tosca.policies.Root"), Mockito.any(Set.class)))
                .thenReturn(policyType);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-policy-template.yml"));
        assertEquals(0, parsingResult.getContext().getParsingErrors().size());

        ArchiveRoot archiveRoot = parsingResult.getResult();
        assertNotNull(archiveRoot.getArchive());
        assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        assertEquals(1, archiveRoot.getPolicyTypes().size());

        PolicyType simplePolicyType = archiveRoot.getPolicyTypes().get("org.alien4cloud.sample.SamplePolicy");
        assertNotNull(simplePolicyType);
        assertEquals("org.alien4cloud.sample.SamplePolicy", simplePolicyType.getElementId());
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
        assertTrue(simplePolicyType.getTargets().contains("tosca.nodes.Compute"));
        assertTrue(simplePolicyType.getTargets().contains("tosca.nodes.Root"));

        // Test that the template is correctly parsed
        assertNotNull(archiveRoot.getTopology());
        assertEquals(1, archiveRoot.getTopology().getPolicies().size());
        assertNotNull(archiveRoot.getTopology().getPolicies().get("anti_affinity_policy"));
        assertEquals("org.alien4cloud.sample.SamplePolicy", archiveRoot.getTopology().getPolicies().get("anti_affinity_policy").getType());
        assertEquals(1, archiveRoot.getTopology().getPolicies().get("anti_affinity_policy").getProperties().size());
        assertNotNull(archiveRoot.getTopology().getPolicies().get("anti_affinity_policy").getProperties().get("sample_property"));
        assertEquals(2, archiveRoot.getTopology().getPolicies().get("anti_affinity_policy").getTags().size());
    }

    @Test
    public void policyTemplateParsingWithUnknownTargetShouldFail() throws FileNotFoundException, ParsingException {
        Mockito.reset(csarRepositorySearchService);
        Mockito.when(csarRepositorySearchService.getArchive("tosca-normative-types", "1.0.0-ALIEN14")).thenReturn(Mockito.mock(Csar.class));

        NodeType nodeType = new NodeType();
        nodeType.setElementId("tosca.nodes.Compute");
        nodeType.setArchiveName("tosca-normative-types");
        nodeType.setArchiveVersion("1.0.0-ALIEN14");
        nodeType.setDerivedFrom(Lists.newArrayList("tosca.nodes.Root"));
        Mockito.when(
                csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Compute"), Mockito.any(Set.class)))
                .thenReturn(nodeType);

        nodeType = new NodeType();
        nodeType.setElementId("tosca.nodes.Root");
        nodeType.setArchiveName("tosca-normative-types");
        nodeType.setArchiveVersion("1.0.0-ALIEN14");
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(Set.class)))
                .thenReturn(nodeType);

        PolicyType policyType = new PolicyType();
        policyType.setAbstract(true);
        policyType.setElementId("tosca.policies.Root");
        policyType.setArchiveName("tosca-normative-types");
        policyType.setArchiveVersion("1.0.0-ALIEN14");
        Mockito.when(
                csarRepositorySearchService.getElementInDependencies(Mockito.eq(PolicyType.class), Mockito.eq("tosca.policies.Root"), Mockito.any(Set.class)))
                .thenReturn(policyType);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-policy-template-fail.yml"));
        assertEquals(1, parsingResult.getContext().getParsingErrors().size());
    }

    public void parseTopologyTemplateWithActivities() throws ParsingException, IOException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-topology-template-activities.yml"));

        assertFalse(parsingResult.getResult().getTopology().getWorkflows().isEmpty());
        assertNotNull(parsingResult.getResult().getTopology().getWorkflows().get("install"));
        Workflow wf = parsingResult.getResult().getTopology().getWorkflows().get("install");

        // check activities
        assertTrue(wf.getSteps().get("Compute_install").getActivities().size() == 1);
        assertTrue(wf.getSteps().get("Compute_install_0").getActivities().size() == 1);
        assertTrue(wf.getSteps().get("Compute_install_1").getActivities().size() == 1);
        assertTrue(wf.getSteps().get("Compute_install").getOnSuccess().size() == 2);

        // check onSuccess
        assertTrue(wf.getSteps().get("Compute_install").getOnSuccess().contains("Compute_uninstall"));
        assertTrue(wf.getSteps().get("Compute_install").getOnSuccess().contains("Compute_uninstall_0"));
        assertTrue(wf.getSteps().get("Compute_install_1").getOnSuccess().contains("Compute_uninstall"));
        assertTrue(wf.getSteps().get("Compute_install_1").getOnSuccess().contains("Compute_uninstall_0"));

        // check activities of an other step
        assertTrue(wf.getSteps().get("Compute_uninstall").getActivities().size() == 1);
        assertTrue(wf.getSteps().get("Compute_uninstall_0").getActivities().size() == 1);
    }

    @Test
    public void parseTopologyTemplateWithInlineWorkflow() throws ParsingException, IOException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-topology-template-inline-workflow.yml"));

        assertFalse(parsingResult.getResult().getTopology().getWorkflows().isEmpty());
        assertNotNull(parsingResult.getResult().getTopology().getWorkflows().get("install"));
        Workflow wf = parsingResult.getResult().getTopology().getWorkflows().get("install");

        assertEquals(1, wf.getSteps().get("Compute_install_0").getActivities().size());
        assertTrue(wf.getSteps().get("Compute_install_0").getActivities().get(0) instanceof InlineWorkflowActivity);
        InlineWorkflowActivity activity = (InlineWorkflowActivity) wf.getSteps().get("Compute_install_0").getActivities().get(0);
        assertTrue(activity.getInline().equals("my_custom_wf"));
        WorkflowUtils.processInlineWorkflows(parsingResult.getResult().getTopology().getWorkflows());
        assertFalse(wf.getSteps().containsKey("Compute_install_0"));
        assertTrue(wf.getSteps().containsKey("Compute_stop"));
        assertTrue(wf.getSteps().containsKey("Compute_uninstall"));

        assertTrue(wf.getSteps().get("Compute_install").getOnSuccess().contains("Compute_stop"));
        assertTrue(wf.getSteps().get("Compute_stop").getPrecedingSteps().contains("Compute_install"));
        assertEquals(1, wf.getSteps().get("Compute_install").getOnSuccess().size());
        assertEquals(0, wf.getSteps().get("Compute_install").getPrecedingSteps().size());
        assertEquals(1, wf.getSteps().get("Compute_stop").getPrecedingSteps().size());

        assertFalse(wf.getSteps().get("Compute_install").getOnSuccess().contains("Compute_uninstall"));
        assertFalse(wf.getSteps().get("Compute_uninstall").getPrecedingSteps().contains("Compute_install"));

        assertTrue(wf.getSteps().get("Compute_stop").getOnSuccess().contains("Compute_uninstall"));
        assertTrue(wf.getSteps().get("Compute_uninstall").getPrecedingSteps().contains("Compute_stop"));
        assertEquals(1, wf.getSteps().get("Compute_stop").getOnSuccess().size());
        assertEquals(1, wf.getSteps().get("Compute_uninstall").getPrecedingSteps().size());
        assertEquals(0, wf.getSteps().get("Compute_uninstall").getOnSuccess().size());

    }

    @Test
    public void parseTopologyTemplateWithRelationshipWorkflow() throws ParsingException, IOException {
        Mockito.reset(csarRepositorySearchService);
        Mockito.when(csarRepositorySearchService.getArchive("tosca-normative-types", "1.0.0-ALIEN14")).thenReturn(Mockito.mock(Csar.class));

        NodeType mockCompute = new NodeType();
        mockCompute.setElementId(NormativeComputeConstants.COMPUTE_TYPE);
        mockCompute.setArchiveName("tosca-normative-types");
        mockCompute.setArchiveVersion("1.0.0-ALIEN14");
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq(NormativeComputeConstants.COMPUTE_TYPE),
                Mockito.any(Set.class))).thenReturn(mockCompute);

        RelationshipType mockHostedOn = Mockito.mock(RelationshipType.class);
        Mockito.when(mockHostedOn.getElementId()).thenReturn(NormativeRelationshipConstants.HOSTED_ON);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(RelationshipType.class),
                Mockito.eq(NormativeRelationshipConstants.HOSTED_ON), Mockito.any(Set.class))).thenReturn(mockHostedOn);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-topology-template-relationship-workflow.yml"));

        Assert.assertFalse(parsingResult.getResult().getTopology().getWorkflows().isEmpty());
        Assert.assertTrue(parsingResult.getResult().getTopology().getWorkflows().get("install") != null);

        Workflow wf = parsingResult.getResult().getTopology().getWorkflows().get("install");
        WorkflowStep relStep = wf.getSteps().get("OracleJDK_hostedOnComputeHost_pre_configure_source");
        Assert.assertNotNull(relStep);
        Assert.assertTrue(relStep instanceof RelationshipWorkflowStep);
        RelationshipWorkflowStep relationshipWorkflowStep = (RelationshipWorkflowStep) relStep;
        Assert.assertNotNull(relationshipWorkflowStep.getTargetRelationship());
        Assert.assertNotNull(relationshipWorkflowStep.getSourceHostId());
        Assert.assertNotNull(relationshipWorkflowStep.getTargetHostId());
        WorkflowStep nStep = wf.getSteps().get("OracleJDK_start");
        Assert.assertNotNull(nStep);
        Assert.assertTrue(nStep instanceof NodeWorkflowStep);
        NodeWorkflowStep nodeWorkflowStep = (NodeWorkflowStep) nStep;
        Assert.assertNotNull(nodeWorkflowStep.getHostId());
    }

    @Test
    public void parseTopologyTemplateWithRelationshipWorkflowMultipleActivities() throws ParsingException, IOException {
        ParsingResult<ArchiveRoot> parsingResult = parser
                .parseFile(Paths.get(getRootDirectory(), "tosca-topology-template-workflow-relationship-operation.yml"));

        assertFalse(parsingResult.getResult().getTopology().getWorkflows().isEmpty());
        assertNotNull(parsingResult.getResult().getTopology().getWorkflows().get("install"));

        Workflow wf = parsingResult.getResult().getTopology().getWorkflows().get("install");
        assertNotNull(wf.getSteps().get("SoftwareComponent_hostedOnComputeHost_pre_configure_source"));

        WorkflowStep step = wf.getSteps().get("SoftwareComponent_hostedOnComputeHost_pre_configure_source");
        assertTrue(step instanceof RelationshipWorkflowStep);
        RelationshipWorkflowStep relStep = (RelationshipWorkflowStep) step;
        assertTrue(relStep.getTarget().equals("SoftwareComponent"));
        assertTrue(relStep.getTargetRelationship().equals("hostedOnComputeHost"));
        assertTrue(relStep.getOperationHost().equals("SOURCE"));
        assertEquals(1, relStep.getActivities().size());
        assertEquals(2, relStep.getOnSuccess().size());

        // test the second step, create to split activities into steps
        WorkflowStep step_0 = wf.getSteps().get("SoftwareComponent_hostedOnComputeHost_pre_configure_source_0");
        assertTrue(step_0 instanceof RelationshipWorkflowStep);
        RelationshipWorkflowStep relStep_0 = (RelationshipWorkflowStep) step_0;
        assertTrue(relStep_0.getTarget().equals("SoftwareComponent"));
        assertTrue(relStep_0.getTargetRelationship().equals("hostedOnComputeHost"));
        assertTrue(relStep_0.getOperationHost().equals("SOURCE"));
        assertEquals(1, relStep_0.getActivities().size());
        assertEquals(1, relStep_0.getOnSuccess().size());
    }
}