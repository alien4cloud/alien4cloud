package alien4cloud.tosca.parser;

import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.ComplexPropertyValue;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.model.definitions.ListPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import alien4cloud.tosca.ArchiveParserTest;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.impl.ErrorCode;

import com.google.common.collect.Lists;

/**
 * Test tosca parsing for Tosca Simple profile in YAML alien_dsl_1_2_0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:tosca/parser-application-context.xml")
public class ToscaParserSimpleProfileAlien120Test extends AbstractToscaParserSimpleProfileTest {

    @Override
    protected String getRootDirectory() {
        return "src/test/resources/tosca/SimpleProfil_alien120/parsing/";
    }

    @Override
    protected String getToscaVersion() {
        return "alien_dsl_1_2_0";
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBadOccurrence() throws FileNotFoundException, ParsingException {
        Mockito.reset(repositorySearchService);
        NodeType mockedResult = Mockito.mock(NodeType.class);
        Mockito.when(
                repositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.SoftwareComponent"),
                        Mockito.any(Set.class))).thenReturn(mockedResult);
        Mockito.when(mockedResult.getDerivedFrom()).thenReturn(Lists.newArrayList("tosca.nodes.Root"));
        Mockito.when(
                repositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(Set.class)))
                .thenReturn(mockedResult);

        Mockito.when(
                repositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Compute"), Mockito.any(Set.class)))
                .thenReturn(mockedResult);
        RelationshipType hostedOn = new RelationshipType();
        Mockito.when(
                repositorySearchService.getElementInDependencies(Mockito.eq(RelationshipType.class), Mockito.eq("tosca.relationships.HostedOn"),
                        Mockito.any(Set.class))).thenReturn(hostedOn);
        CapabilityType mockedCapabilityResult = Mockito.mock(CapabilityType.class);
        Mockito.when(
                repositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq("tosca.capabilities.Endpoint"),
                        Mockito.any(Set.class))).thenReturn(mockedCapabilityResult);
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-node-type-invalid-occurrence.yml"));

        Assert.assertEquals(2, countErrorByLevelAndCode(parsingResult, ParsingErrorLevel.ERROR, ErrorCode.SYNTAX_ERROR));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRelationshipType() throws FileNotFoundException, ParsingException {
        Mockito.reset(repositorySearchService);
        RelationshipType mockedResult = Mockito.mock(RelationshipType.class);
        Mockito.when(
                repositorySearchService.getElementInDependencies(Mockito.eq(RelationshipType.class), Mockito.eq("tosca.relationships.Relationship"),
                        Mockito.any(Set.class))).thenReturn(mockedResult);
        Mockito.when(mockedResult.getDerivedFrom()).thenReturn(Lists.newArrayList("tosca.capabilities.Root"));

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-relationship-type.yml"));
        assertNoBlocker(parsingResult);
        ArchiveRoot archiveRoot = parsingResult.getResult();
        Assert.assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        Assert.assertEquals(1, archiveRoot.getRelationshipTypes().size());
        Entry<String, RelationshipType> entry = archiveRoot.getRelationshipTypes().entrySet().iterator().next();
        Assert.assertEquals("mycompany.mytypes.myapplication.MyRelationship", entry.getKey());
        RelationshipType relationship = entry.getValue();
        Assert.assertEquals(Lists.newArrayList("tosca.relationships.Relationship", "tosca.capabilities.Root"), relationship.getDerivedFrom());
        Assert.assertEquals("a custom relationship", relationship.getDescription());

        // properties
        Assert.assertEquals(2, relationship.getProperties().size());
        Assert.assertTrue(relationship.getProperties().containsKey("my_feature_setting"));
        PropertyDefinition pd = relationship.getProperties().get("my_feature_setting");
        Assert.assertEquals("string", pd.getType());
        Assert.assertTrue(relationship.getProperties().containsKey("my_feature_value"));
        pd = relationship.getProperties().get("my_feature_value");
        Assert.assertEquals("integer", pd.getType());

        // valid targets
        Assert.assertEquals(2, relationship.getValidTargets().length);
        Assert.assertEquals("tosca.capabilities.Feature1", relationship.getValidTargets()[0]);
        Assert.assertEquals("tosca.capabilities.Feature2", relationship.getValidTargets()[1]);

    }

    @Test
    public void testDataTypesExtendsNative() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-data-types-extends-native.yml"));
        ArchiveParserTest.displayErrors(parsingResult);
        Assert.assertEquals(3, parsingResult.getResult().getDataTypes().size());
        Assert.assertEquals(1, parsingResult.getResult().getNodeTypes().size());
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
        Assert.assertEquals(1, parsingResult.getResult().getTopology().getNodeTemplates().size());
        NodeTemplate nodeTemplate = parsingResult.getResult().getTopology().getNodeTemplates().values().iterator().next();
        Assert.assertEquals(3, nodeTemplate.getProperties().size());
        // check url property
        Assert.assertTrue(nodeTemplate.getProperties().containsKey("url"));
        AbstractPropertyValue url = nodeTemplate.getProperties().get("url");
        Assert.assertTrue(url instanceof ScalarPropertyValue);
        Assert.assertEquals("https://kikoo.com", ((ScalarPropertyValue) url).getValue());
        // check ipv6_addresses property
        Assert.assertTrue(nodeTemplate.getProperties().containsKey("ipv6_addresses"));
        AbstractPropertyValue ipv6_addresses = nodeTemplate.getProperties().get("ipv6_addresses");
        Assert.assertTrue(ipv6_addresses instanceof ListPropertyValue);
        List<Object> ipv6_addresses_list = ((ListPropertyValue) ipv6_addresses).getValue();
        Assert.assertEquals(2, ipv6_addresses_list.size());
        Assert.assertEquals("192.168.0.10", ipv6_addresses_list.get(0));
        Assert.assertEquals("10.0.0.10", ipv6_addresses_list.get(1));
        // check passwords property
        Assert.assertTrue(nodeTemplate.getProperties().containsKey("passwords"));
        AbstractPropertyValue passwords = nodeTemplate.getProperties().get("passwords");
        Assert.assertTrue(passwords instanceof ComplexPropertyValue);
        Map<String, Object> passwords_map = ((ComplexPropertyValue) passwords).getValue();
        Assert.assertEquals(2, passwords_map.size());
        Assert.assertEquals("123456789", passwords_map.get("user1"));
        Assert.assertEquals("abcdefghij", passwords_map.get("user2"));
    }

    @Test
    public void testDataTypesExtendsNativeWithError1() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-data-types-extends-native-error1.yml"));
        ArchiveParserTest.displayErrors(parsingResult);
        Assert.assertEquals(3, parsingResult.getResult().getDataTypes().size());
        Assert.assertEquals(1, parsingResult.getResult().getNodeTypes().size());
        Assert.assertEquals(1, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testDataTypesExtendsNativeWithError2() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-data-types-extends-native-error2.yml"));
        ArchiveParserTest.displayErrors(parsingResult);
        Assert.assertEquals(3, parsingResult.getResult().getDataTypes().size());
        Assert.assertEquals(1, parsingResult.getResult().getNodeTypes().size());
        Assert.assertEquals(1, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testDataTypesExtendsNativeWithError3() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-data-types-extends-native-error3.yml"));
        ArchiveParserTest.displayErrors(parsingResult);
        Assert.assertEquals(3, parsingResult.getResult().getDataTypes().size());
        Assert.assertEquals(1, parsingResult.getResult().getNodeTypes().size());
        Assert.assertEquals(1, parsingResult.getContext().getParsingErrors().size());
    }

    @Test
    public void testDataTypesComplexWithDefault() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-data-types-complex-default.yml"));
        ArchiveParserTest.displayErrors(parsingResult);
        Assert.assertEquals(1, parsingResult.getResult().getDataTypes().size());
        Assert.assertEquals(1, parsingResult.getResult().getNodeTypes().size());
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
        NodeType commandType = parsingResult.getResult().getNodeTypes().get("alien.test.Command");
        Assert.assertNotNull(commandType);
        PropertyDefinition pd = commandType.getProperties().get("customer");
        Assert.assertNotNull(pd);
        // check the default value
        Object defaultValue = pd.getDefault();
        Assert.assertNotNull(defaultValue);
        Assert.assertTrue(defaultValue instanceof ComplexPropertyValue);
        ComplexPropertyValue cpv = (ComplexPropertyValue) defaultValue;
        Map<String, Object> valueAsMap = cpv.getValue();
        Assert.assertNotNull(valueAsMap);
        Assert.assertTrue(valueAsMap.containsKey("first_name"));
        Assert.assertEquals("Foo", valueAsMap.get("first_name"));
        Assert.assertTrue(valueAsMap.containsKey("last_name"));
        Assert.assertEquals("Bar", valueAsMap.get("last_name"));
        Assert.assertEquals(1, parsingResult.getResult().getTopology().getNodeTemplates().size());
        NodeTemplate nodeTemplate = parsingResult.getResult().getTopology().getNodeTemplates().values().iterator().next();
        // on the node, the default value should be set
        Assert.assertNotNull(nodeTemplate.getProperties());
        Assert.assertTrue(nodeTemplate.getProperties().containsKey("customer"));
        AbstractPropertyValue apv = nodeTemplate.getProperties().get("customer");
        Assert.assertNotNull(apv);
        Assert.assertTrue(apv instanceof ComplexPropertyValue);
        cpv = (ComplexPropertyValue) apv;
        valueAsMap = cpv.getValue();
        Assert.assertNotNull(valueAsMap);
        Assert.assertTrue(valueAsMap.containsKey("first_name"));
        Assert.assertEquals("Foo", valueAsMap.get("first_name"));
        Assert.assertTrue(valueAsMap.containsKey("last_name"));
        Assert.assertEquals("Bar", valueAsMap.get("last_name"));
    }

    @Test
    public void testDataTypesVeryComplexWithDefault() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-data-types-very-complex-default.yml"));
        ArchiveParserTest.displayErrors(parsingResult);
        Assert.assertEquals(2, parsingResult.getResult().getDataTypes().size());
        Assert.assertEquals(1, parsingResult.getResult().getNodeTypes().size());
        Assert.assertEquals(0, parsingResult.getContext().getParsingErrors().size());
        NodeType commandType = parsingResult.getResult().getNodeTypes().get("alien.test.Command");
        Assert.assertNotNull(commandType);
        PropertyDefinition pd = commandType.getProperties().get("customer");
        Assert.assertNotNull(pd);
        // check the default value
        Object defaultValue = pd.getDefault();
        Assert.assertNotNull(defaultValue);
        Assert.assertTrue(defaultValue instanceof ComplexPropertyValue);
        ComplexPropertyValue cpv = (ComplexPropertyValue) defaultValue;
        Map<String, Object> valueAsMap = cpv.getValue();
        Assert.assertNotNull(valueAsMap);
        Assert.assertTrue(valueAsMap.containsKey("first_name"));
        Assert.assertEquals("Foo", valueAsMap.get("first_name"));
        Assert.assertTrue(valueAsMap.containsKey("last_name"));
        Assert.assertEquals("Bar", valueAsMap.get("last_name"));
        Assert.assertTrue(valueAsMap.containsKey("address"));
        Object addressObj = valueAsMap.get("address");
        Assert.assertNotNull(addressObj);
        Assert.assertTrue(addressObj instanceof Map);
        Map<String, Object> addressMap = (Map<String, Object>) addressObj;
        Assert.assertTrue(addressMap.containsKey("street_name"));
        Assert.assertEquals("rue des peupliers", addressMap.get("street_name"));
        Assert.assertTrue(addressMap.containsKey("zipcode"));
        Assert.assertEquals("92130", addressMap.get("zipcode"));
        Assert.assertTrue(addressMap.containsKey("city_name"));
        Assert.assertEquals("ISSY LES MOULES", addressMap.get("city_name"));
        Assert.assertTrue(valueAsMap.containsKey("emails"));
        Object emailsObj = valueAsMap.get("emails");
        Assert.assertNotNull(emailsObj);
        Assert.assertTrue(emailsObj instanceof List);
        List<Object> emailsList = (List<Object>) emailsObj;
        Assert.assertEquals(2, emailsList.size());
        Assert.assertEquals("contact@fastconnect.fr", emailsList.get(0));
        Assert.assertEquals("info@fastconnect.fr", emailsList.get(1));
        Object accountsObj = valueAsMap.get("accounts");
        Assert.assertNotNull(accountsObj);
        Assert.assertTrue(accountsObj instanceof Map);
        Map<String, Object> accountsMap = (Map<String, Object>) accountsObj;
        Assert.assertEquals(2, accountsMap.size());
        Assert.assertTrue(accountsMap.containsKey("main"));
        Assert.assertEquals("root", accountsMap.get("main"));
        Assert.assertTrue(accountsMap.containsKey("secondary"));
        Assert.assertEquals("user", accountsMap.get("secondary"));
        Assert.assertEquals(1, parsingResult.getResult().getTopology().getNodeTemplates().size());
        NodeTemplate nodeTemplate = parsingResult.getResult().getTopology().getNodeTemplates().values().iterator().next();
        // on the node, the default value should be set
        Assert.assertNotNull(nodeTemplate.getProperties());
        Assert.assertTrue(nodeTemplate.getProperties().containsKey("customer"));
        AbstractPropertyValue apv = nodeTemplate.getProperties().get("customer");
        Assert.assertNotNull(apv);
        Assert.assertTrue(apv instanceof ComplexPropertyValue);
        cpv = (ComplexPropertyValue) apv;
        valueAsMap = cpv.getValue();
        Assert.assertNotNull(valueAsMap);
        Assert.assertTrue(valueAsMap.containsKey("first_name"));
        Assert.assertEquals("Foo", valueAsMap.get("first_name"));
        Assert.assertTrue(valueAsMap.containsKey("last_name"));
        Assert.assertEquals("Bar", valueAsMap.get("last_name"));
        Assert.assertTrue(valueAsMap.containsKey("address"));
        addressObj = valueAsMap.get("address");
        Assert.assertNotNull(addressObj);
        Assert.assertTrue(addressObj instanceof Map);
        addressMap = (Map<String, Object>) addressObj;
        Assert.assertTrue(addressMap.containsKey("street_name"));
        Assert.assertEquals("rue des peupliers", addressMap.get("street_name"));
        Assert.assertTrue(addressMap.containsKey("zipcode"));
        Assert.assertEquals("92130", addressMap.get("zipcode"));
        Assert.assertTrue(addressMap.containsKey("city_name"));
        Assert.assertEquals("ISSY LES MOULES", addressMap.get("city_name"));
        Assert.assertTrue(valueAsMap.containsKey("emails"));
        emailsObj = valueAsMap.get("emails");
        Assert.assertNotNull(emailsObj);
        Assert.assertTrue(emailsObj instanceof List);
        emailsList = (List<Object>) emailsObj;
        Assert.assertEquals(2, emailsList.size());
        Assert.assertEquals("contact@fastconnect.fr", emailsList.get(0));
        Assert.assertEquals("info@fastconnect.fr", emailsList.get(1));
        accountsObj = valueAsMap.get("accounts");
        Assert.assertNotNull(accountsObj);
        Assert.assertTrue(accountsObj instanceof Map);
        accountsMap = (Map<String, Object>) accountsObj;
        Assert.assertEquals(2, accountsMap.size());
        Assert.assertTrue(accountsMap.containsKey("main"));
        Assert.assertEquals("root", accountsMap.get("main"));
        Assert.assertTrue(accountsMap.containsKey("secondary"));
        Assert.assertEquals("user", accountsMap.get("secondary"));
    }

}
