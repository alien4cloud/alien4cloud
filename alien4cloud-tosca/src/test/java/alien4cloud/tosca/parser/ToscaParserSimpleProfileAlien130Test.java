package alien4cloud.tosca.parser;

import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.definitions.ImplementationArtifact;
import org.alien4cloud.tosca.model.types.AbstractInstantiableToscaType;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.elasticsearch.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.Lists;

import alien4cloud.tosca.model.ArchiveRoot;
import org.alien4cloud.tosca.normative.constants.NormativeCredentialConstant;
import org.alien4cloud.tosca.normative.constants.NormativeTypesConstant;
import alien4cloud.tosca.parser.impl.ErrorCode;

public class ToscaParserSimpleProfileAlien130Test extends AbstractToscaParserSimpleProfileTest {

    @Override
    protected String getRootDirectory() {
        return "src/test/resources/tosca/SimpleProfile_alien130/parsing/";
    }

    @Override
    protected String getToscaVersion() {
        return "alien_dsl_1_3_0";
    }

    @Test
    public void testDerivedFromNothing() throws ParsingException {
        ParserTestUtil.mockNormativeTypes(csarRepositorySearchService);
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "derived_from_nothing/template.yml"));
        List<ParsingError> errors = parsingResult.getContext().getParsingErrors();
        Assert.assertEquals(5, errors.size());
        Assert.assertTrue(errors.stream()
                .allMatch(error -> error.getErrorLevel() == ParsingErrorLevel.WARNING && error.getErrorCode() == ErrorCode.DERIVED_FROM_NOTHING));
        Assert.assertTrue(parsingResult.getResult().getNodeTypes().values().stream()
                .allMatch(nodeType -> nodeType.getDerivedFrom() != null && nodeType.getDerivedFrom().contains(NormativeTypesConstant.ROOT_NODE_TYPE)));
        Assert.assertTrue(parsingResult.getResult().getDataTypes().values().stream()
                .allMatch(dataType -> dataType.getDerivedFrom() != null && dataType.getDerivedFrom().contains(NormativeTypesConstant.ROOT_DATA_TYPE)));
        Assert.assertTrue(parsingResult.getResult().getCapabilityTypes().values().stream().allMatch(capabilityType -> capabilityType.getDerivedFrom() != null
                && capabilityType.getDerivedFrom().contains(NormativeTypesConstant.ROOT_CAPABILITY_TYPE)));
        Assert.assertTrue(
                parsingResult.getResult().getRelationshipTypes().values().stream().allMatch(relationshipType -> relationshipType.getDerivedFrom() != null
                        && relationshipType.getDerivedFrom().contains(NormativeTypesConstant.ROOT_RELATIONSHIP_TYPE)));
        Assert.assertTrue(parsingResult.getResult().getArtifactTypes().values().stream().allMatch(
                artifactType -> artifactType.getDerivedFrom() != null && artifactType.getDerivedFrom().contains(NormativeTypesConstant.ROOT_ARTIFACT_TYPE)));
    }

    @Test
    public void testCapabilities() throws ParsingException {
        NodeType mockedResult = Mockito.mock(NodeType.class);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.SoftwareComponent"),
                Mockito.any(Set.class))).thenReturn(mockedResult);
        Mockito.when(mockedResult.getDerivedFrom()).thenReturn(Lists.newArrayList("tosca.nodes.Root"));
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(Set.class)))
                .thenReturn(mockedResult);

        Mockito.when(
                csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Compute"), Mockito.any(Set.class)))
                .thenReturn(mockedResult);
        CapabilityType mockedCapabilityResult = Mockito.mock(CapabilityType.class);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq("tosca.capabilities.Endpoint"),
                Mockito.any(Set.class))).thenReturn(mockedCapabilityResult);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq("tosca.capabilities.Container"),
                Mockito.any(Set.class))).thenReturn(mockedCapabilityResult);

        RelationshipType connectsTo = new RelationshipType();
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(RelationshipType.class), Mockito.eq("tosca.relationships.ConnectsTo"),
                Mockito.any(Set.class))).thenReturn(connectsTo);

        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "requirement_capabilities.yaml"));
        ParserTestUtil.displayErrors(parsingResult);
        parsingResult.getResult().getNodeTypes().values().forEach(nodeType -> {
            nodeType.getRequirements().forEach(requirementDefinition -> {
                switch (requirementDefinition.getId()) {
                case "host":
                    Assert.assertEquals("tosca.capabilities.Container", requirementDefinition.getType());
                    break;
                case "endpoint":
                case "another_endpoint":
                    Assert.assertEquals("tosca.capabilities.Endpoint", requirementDefinition.getType());
                    Assert.assertEquals(0, requirementDefinition.getLowerBound());
                    Assert.assertEquals(Integer.MAX_VALUE, requirementDefinition.getUpperBound());
                    Assert.assertEquals("tosca.relationships.ConnectsTo", requirementDefinition.getRelationshipType());
                    break;
                }
            });
            nodeType.getCapabilities().forEach(capabilityDefinition -> {
                switch (capabilityDefinition.getId()) {
                case "host":
                    Assert.assertEquals("tosca.capabilities.Container", capabilityDefinition.getType());
                    break;
                case "endpoint":
                case "another_endpoint":
                    Assert.assertEquals("tosca.capabilities.Endpoint", capabilityDefinition.getType());
                    Assert.assertNotNull(capabilityDefinition.getDescription());
                }
            });
        });
    }

    @Test
    public void testNodeTypeMissingRequirementType() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-node-type-missing-requirement-type.yml"));
        Assert.assertEquals(2, parsingResult.getContext().getParsingErrors().size());
        Assert.assertEquals(ErrorCode.TYPE_NOT_FOUND, parsingResult.getContext().getParsingErrors().get(0).getErrorCode());
        Assert.assertEquals(ErrorCode.TYPE_NOT_FOUND, parsingResult.getContext().getParsingErrors().get(1).getErrorCode());
    }

    @Test
    public void testNodeTypeMissingCapabilityType() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "tosca-node-type-missing-capability-type.yml"));
        Assert.assertEquals(1, parsingResult.getContext().getParsingErrors().size());
        Assert.assertEquals(ErrorCode.TYPE_NOT_FOUND, parsingResult.getContext().getParsingErrors().get(0).getErrorCode());
    }

    private void validateHttpArtifact(NodeType httpComponent) {
        ImplementationArtifact httpComponentCreateArtifact = getImplementationArtifact(httpComponent, "create");
        Assert.assertEquals("https://otherCompany/script/short_notation.sh", httpComponentCreateArtifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Implementation.Bash", httpComponentCreateArtifact.getArtifactType());
        Assert.assertNull(httpComponentCreateArtifact.getRepositoryCredential());
        Assert.assertNull(httpComponentCreateArtifact.getRepositoryName());
        Assert.assertNull(httpComponentCreateArtifact.getArtifactRepository());
        Assert.assertNull(httpComponentCreateArtifact.getRepositoryURL());

        ImplementationArtifact httpComponentStartArtifact = getImplementationArtifact(httpComponent, "start");
        Assert.assertEquals("myScript.abc", httpComponentStartArtifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Implementation.Bash", httpComponentStartArtifact.getArtifactType());
        Assert.assertEquals(
                ImmutableMap.<String, Object> builder().put(NormativeCredentialConstant.USER_KEY, "good_user")
                        .put(NormativeCredentialConstant.TOKEN_KEY, "real_secured_password").put(NormativeCredentialConstant.TOKEN_TYPE, "password").build(),
                httpComponentStartArtifact.getRepositoryCredential());
        Assert.assertEquals("script_repo", httpComponentStartArtifact.getRepositoryName());
        Assert.assertNull(httpComponentStartArtifact.getArtifactRepository());
        Assert.assertEquals("https://myCompany/script", httpComponentStartArtifact.getRepositoryURL());
    }

    @Test
    public void testParseImplementationArtifactWithRepository() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "implementation_artifact.yml"));
        ParserTestUtil.displayErrors(parsingResult);
        Assert.assertTrue(parsingResult.getContext().getParsingErrors().isEmpty());
        ArchiveRoot archiveRoot = parsingResult.getResult();
        Assert.assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        Assert.assertEquals(2, archiveRoot.getArtifactTypes().size());
        Assert.assertEquals(4, archiveRoot.getNodeTypes().size());
        Assert.assertEquals(3, archiveRoot.getRepositories().size());
        Assert.assertEquals(3, archiveRoot.getRelationshipTypes().size());

        NodeType httpComponent = archiveRoot.getNodeTypes().get("my.http.component");
        validateHttpArtifact(httpComponent);

        NodeType httpComponentExtended = archiveRoot.getNodeTypes().get("my.http.component.extended");
        validateHttpArtifact(httpComponentExtended);

        NodeType gitComponent = archiveRoot.getNodeTypes().get("my.git.component");
        ImplementationArtifact gitComponentCreateArtifact = getImplementationArtifact(gitComponent, "create");
        Assert.assertEquals("master:myGitScript.xyz", gitComponentCreateArtifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Implementation.Bash", gitComponentCreateArtifact.getArtifactType());
        Assert.assertNull(gitComponentCreateArtifact.getRepositoryCredential());
        Assert.assertEquals("git_repo", gitComponentCreateArtifact.getRepositoryName());
        Assert.assertEquals("git", gitComponentCreateArtifact.getArtifactRepository());
        Assert.assertEquals("https://github.com/myId/myRepo.git", gitComponentCreateArtifact.getRepositoryURL());

        RelationshipType httpRelationship = archiveRoot.getRelationshipTypes().get("my.http.relationship");
        ImplementationArtifact httpRelationshipCreateArtifact = getImplementationArtifact(httpRelationship, "create");
        Assert.assertEquals("https://otherCompany/script/short_notation.sh", httpRelationshipCreateArtifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Implementation.Bash", httpRelationshipCreateArtifact.getArtifactType());
        Assert.assertNull(httpRelationshipCreateArtifact.getRepositoryCredential());
        Assert.assertNull(httpRelationshipCreateArtifact.getRepositoryName());
        Assert.assertNull(httpRelationshipCreateArtifact.getArtifactRepository());
        Assert.assertNull(httpRelationshipCreateArtifact.getRepositoryURL());

        ImplementationArtifact httpRelationshipStartArtifact = getImplementationArtifact(httpRelationship, "start");
        Assert.assertEquals("myScript.abc", httpRelationshipStartArtifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Implementation.Bash", httpRelationshipStartArtifact.getArtifactType());
        Assert.assertEquals(
                ImmutableMap.<String, Object> builder().put(NormativeCredentialConstant.USER_KEY, "good_user")
                        .put(NormativeCredentialConstant.TOKEN_KEY, "real_secured_password").put(NormativeCredentialConstant.TOKEN_TYPE, "password").build(),
                httpRelationshipStartArtifact.getRepositoryCredential());
        Assert.assertEquals("script_repo", httpRelationshipStartArtifact.getRepositoryName());
        Assert.assertNull(httpRelationshipStartArtifact.getArtifactRepository());
        Assert.assertEquals("https://myCompany/script", httpRelationshipStartArtifact.getRepositoryURL());
    }

    private void validateSimpleWar(DeploymentArtifact artifact) {
        Assert.assertEquals("binary/myWar.war", artifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Deployment.War", artifact.getArtifactType());
        Assert.assertNull(artifact.getRepositoryCredential());
        Assert.assertNull(artifact.getRepositoryName());
        Assert.assertNull(artifact.getArtifactRepository());
        Assert.assertNull(artifact.getRepositoryURL());
    }

    private void validateRemoteWar(DeploymentArtifact repositoryArtifact) {
        Assert.assertEquals("alien4cloud:alien4cloud-ui:1.3.0-SM3", repositoryArtifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Deployment.War", repositoryArtifact.getArtifactType());
        Assert.assertEquals(
                ImmutableMap.<String, Object> builder().put(NormativeCredentialConstant.USER_KEY, "good_user")
                        .put(NormativeCredentialConstant.TOKEN_KEY, "real_secured_password").put(NormativeCredentialConstant.TOKEN_TYPE, "password").build(),
                repositoryArtifact.getRepositoryCredential());
        Assert.assertEquals("maven_repo", repositoryArtifact.getRepositoryName());
        Assert.assertEquals("maven", repositoryArtifact.getArtifactRepository());
        Assert.assertEquals("https://fastconnect.org/maven/content/repositories/fastconnect", repositoryArtifact.getRepositoryURL());
    }

    private void validateMavenDeploymentArtifact(NodeType mavenComponent) {
        DeploymentArtifact artifact = getDeploymentArtifact(mavenComponent, "simple_war");
        validateSimpleWar(artifact);
        DeploymentArtifact repositoryArtifact = getDeploymentArtifact(mavenComponent, "remote_war");
        validateRemoteWar(repositoryArtifact);
    }

    @Test
    public void testParseDeploymentArtifactWithRepository() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "deployment_artifact.yml"));
        ParserTestUtil.displayErrors(parsingResult);
        Assert.assertTrue(parsingResult.getContext().getParsingErrors().isEmpty());
        ArchiveRoot archiveRoot = parsingResult.getResult();
        Assert.assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        Assert.assertEquals(1, archiveRoot.getRepositories().size());
        Assert.assertEquals(2, archiveRoot.getArtifactTypes().size());
        Assert.assertEquals(3, archiveRoot.getNodeTypes().size());
        Assert.assertEquals(3, archiveRoot.getNodeTypes().size());
        Assert.assertEquals(3, archiveRoot.getRelationshipTypes().size());

        NodeType mavenComponent = archiveRoot.getNodeTypes().get("my.maven.component");
        validateMavenDeploymentArtifact(mavenComponent);

        NodeType mavenExtendedComponent = archiveRoot.getNodeTypes().get("my.maven.component.extended");
        validateMavenDeploymentArtifact(mavenExtendedComponent);

        DeploymentArtifact artifact = archiveRoot.getTopology().getInputArtifacts().get("simple_war");
        validateSimpleWar(artifact);

        DeploymentArtifact repositoryArtifact = archiveRoot.getTopology().getInputArtifacts().get("remote_war");
        validateRemoteWar(repositoryArtifact);

        artifact = archiveRoot.getTopology().getNodeTemplates().get("my_node").getArtifacts().get("simple_war");
        Assert.assertEquals("binary/myWar.war", artifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Deployment.War", artifact.getArtifactType());
        Assert.assertNull(artifact.getRepositoryCredential());
        Assert.assertNull(artifact.getRepositoryName());
        Assert.assertNull(artifact.getArtifactRepository());
        Assert.assertNull(artifact.getRepositoryURL());

        repositoryArtifact = archiveRoot.getTopology().getNodeTemplates().get("my_node").getArtifacts().get("remote_war");
        Assert.assertEquals("alien4cloud:alien4cloud-ui:1.3.0-SM3", repositoryArtifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Deployment.War", repositoryArtifact.getArtifactType());
        Assert.assertEquals(
                ImmutableMap.<String, Object> builder().put(NormativeCredentialConstant.USER_KEY, "good_user")
                        .put(NormativeCredentialConstant.TOKEN_KEY, "real_secured_password").put(NormativeCredentialConstant.TOKEN_TYPE, "password").build(),
                repositoryArtifact.getRepositoryCredential());
        Assert.assertEquals("maven_repo", repositoryArtifact.getRepositoryName());
        Assert.assertEquals("maven", repositoryArtifact.getArtifactRepository());
        Assert.assertEquals("https://fastconnect.org/maven/content/repositories/fastconnect", repositoryArtifact.getRepositoryURL());
    }

    private ImplementationArtifact getImplementationArtifact(AbstractInstantiableToscaType component, String operation) {
        return component.getInterfaces().values().iterator().next().getOperations().get(operation).getImplementationArtifact();
    }

    private DeploymentArtifact getDeploymentArtifact(AbstractInstantiableToscaType component, String artifactName) {
        return component.getArtifacts().get(artifactName);
    }

    @Test
    public void testRangeType() throws ParsingException {
        ParserTestUtil.mockNormativeTypes(csarRepositorySearchService);
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "range_type.yml"));
        List<ParsingError> errors = parsingResult.getContext().getParsingErrors();
        Assert.assertEquals(0, errors.size());
    }

    @Test
    public void testRangeTypeConstraint() throws ParsingException {
        ParserTestUtil.mockNormativeTypes(csarRepositorySearchService);
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "range_type_constraint.yml"));
        List<ParsingError> errors = parsingResult.getContext().getParsingErrors();
        Assert.assertEquals(0, errors.size());
    }

    @Test
    public void testRangeTypeConstraintFailMin() throws ParsingException {
        ParserTestUtil.mockNormativeTypes(csarRepositorySearchService);
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "range_type_constraint_fail_min.yml"));
        List<ParsingError> errors = parsingResult.getContext().getParsingErrors();
        Assert.assertEquals(0, errors.size());
    }

    @Test
    public void testRangeTypeConstraintFailMax() throws ParsingException {
        ParserTestUtil.mockNormativeTypes(csarRepositorySearchService);
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "range_type_constraint_fail_max.yml"));
        List<ParsingError> errors = parsingResult.getContext().getParsingErrors();
        Assert.assertEquals(0, errors.size());
    }
}