package alien4cloud.tosca.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.ComplexPropertyValue;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.definitions.IValue;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.DataType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.PolicyType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.Lists;

import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
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
}