package alien4cloud.tosca.parser;

import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import alien4cloud.model.components.DeploymentArtifact;
import alien4cloud.model.components.ImplementationArtifact;
import alien4cloud.model.components.IndexedArtifactToscaElement;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.tosca.ArchiveParserTest;
import alien4cloud.tosca.model.ArchiveRoot;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:tosca/parser-application-context.xml")
public class ToscaParserSimpleProfileAlien130Test extends AbstractToscaParserSimpleProfileTest {

    @Test
    public void testParseImplementationArtifactWithRepository() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "implementation_artifact.yml"));
        ArchiveParserTest.displayErrors(parsingResult);
        ArchiveRoot archiveRoot = parsingResult.getResult();
        Assert.assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        Assert.assertEquals(1, archiveRoot.getArtifactTypes().size());
        Assert.assertEquals(2, archiveRoot.getNodeTypes().size());
        Assert.assertEquals(2, archiveRoot.getRepositories().size());
        Assert.assertEquals(1, archiveRoot.getRelationshipTypes().size());

        IndexedNodeType httpComponent = archiveRoot.getNodeTypes().get("my.http.component");

        ImplementationArtifact httpComponentCreateArtifact = getImplementationArtifact(httpComponent, "create");
        Assert.assertEquals("https://otherCompany/script/short_notation.sh", httpComponentCreateArtifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Implementation.Bash", httpComponentCreateArtifact.getArtifactType());
        Assert.assertNull(httpComponentCreateArtifact.getRepositoryCredentials());
        Assert.assertNull(httpComponentCreateArtifact.getRepositoryName());
        Assert.assertNull(httpComponentCreateArtifact.getArtifactRepository());
        Assert.assertNull(httpComponentCreateArtifact.getRepositoryURL());

        ImplementationArtifact httpComponentStartArtifact = getImplementationArtifact(httpComponent, "start");
        Assert.assertEquals("myScript.abc", httpComponentStartArtifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Implementation.Bash", httpComponentStartArtifact.getArtifactType());
        Assert.assertEquals("good_user:real_secured_password", httpComponentStartArtifact.getRepositoryCredentials());
        Assert.assertEquals("script_repo", httpComponentStartArtifact.getRepositoryName());
        Assert.assertNull(httpComponentStartArtifact.getArtifactRepository());
        Assert.assertEquals("https://myCompany/script", httpComponentStartArtifact.getRepositoryURL());

        IndexedNodeType gitComponent = archiveRoot.getNodeTypes().get("my.git.component");
        ImplementationArtifact gitComponentCreateArtifact = getImplementationArtifact(gitComponent, "create");
        Assert.assertEquals("master:myGitScript.xyz", gitComponentCreateArtifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Implementation.Bash", gitComponentCreateArtifact.getArtifactType());
        Assert.assertNull(gitComponentCreateArtifact.getRepositoryCredentials());
        Assert.assertEquals("git_repo", gitComponentCreateArtifact.getRepositoryName());
        Assert.assertEquals("git", gitComponentCreateArtifact.getArtifactRepository());
        Assert.assertEquals("https://github.com/myId/myRepo.git", gitComponentCreateArtifact.getRepositoryURL());

        IndexedRelationshipType httpRelationship = archiveRoot.getRelationshipTypes().get("my.http.relationship");
        ImplementationArtifact httpRelationshipCreateArtifact = getImplementationArtifact(httpRelationship, "create");
        Assert.assertEquals("https://otherCompany/script/short_notation.sh", httpRelationshipCreateArtifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Implementation.Bash", httpRelationshipCreateArtifact.getArtifactType());
        Assert.assertNull(httpRelationshipCreateArtifact.getRepositoryCredentials());
        Assert.assertNull(httpRelationshipCreateArtifact.getRepositoryName());
        Assert.assertNull(httpRelationshipCreateArtifact.getArtifactRepository());
        Assert.assertNull(httpRelationshipCreateArtifact.getRepositoryURL());

        ImplementationArtifact httpRelationshipStartArtifact = getImplementationArtifact(httpRelationship, "start");
        Assert.assertEquals("myScript.abc", httpRelationshipStartArtifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Implementation.Bash", httpRelationshipStartArtifact.getArtifactType());
        Assert.assertEquals("good_user:real_secured_password", httpRelationshipStartArtifact.getRepositoryCredentials());
        Assert.assertEquals("script_repo", httpRelationshipStartArtifact.getRepositoryName());
        Assert.assertNull(httpRelationshipStartArtifact.getArtifactRepository());
        Assert.assertEquals("https://myCompany/script", httpRelationshipStartArtifact.getRepositoryURL());
    }

    @Test
    public void testParseDeploymentArtifactWithRepository() throws ParsingException {
        ParsingResult<ArchiveRoot> parsingResult = parser.parseFile(Paths.get(getRootDirectory(), "deployment_artifact.yml"));
        ArchiveParserTest.displayErrors(parsingResult);
        ArchiveRoot archiveRoot = parsingResult.getResult();
        Assert.assertNotNull(archiveRoot.getArchive());
        Assert.assertEquals(getToscaVersion(), archiveRoot.getArchive().getToscaDefinitionsVersion());
        Assert.assertEquals(1, archiveRoot.getArtifactTypes().size());
        Assert.assertEquals(1, archiveRoot.getNodeTypes().size());
        Assert.assertEquals(1, archiveRoot.getNodeTypes().size());
        Assert.assertEquals(1, archiveRoot.getRelationshipTypes().size());

        IndexedNodeType mavenComponent = archiveRoot.getNodeTypes().get("my.maven.component");
        DeploymentArtifact artifact = getDeploymentArtifact(mavenComponent, "simple_war");
        Assert.assertEquals("binary/myWar.war", artifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Deployment.War", artifact.getArtifactType());
        Assert.assertNull(artifact.getRepositoryCredentials());
        Assert.assertNull(artifact.getRepositoryName());
        Assert.assertNull(artifact.getArtifactRepository());
        Assert.assertNull(artifact.getRepositoryURL());

        DeploymentArtifact repositoryArtifact = getDeploymentArtifact(mavenComponent, "remote_war");
        Assert.assertEquals("alien4cloud:alien4cloud-ui:1.3.0-SM3", repositoryArtifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Deployment.War", repositoryArtifact.getArtifactType());
        Assert.assertEquals("good_user:real_secured_password", repositoryArtifact.getRepositoryCredentials());
        Assert.assertEquals("maven_repo", repositoryArtifact.getRepositoryName());
        Assert.assertEquals("maven", repositoryArtifact.getArtifactRepository());
        Assert.assertEquals("https://fastconnect.org/maven/content/repositories/fastconnect", repositoryArtifact.getRepositoryURL());

        artifact = archiveRoot.getTopology().getInputArtifacts().get("simple_war");
        Assert.assertEquals("binary/myWar.war", artifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Deployment.War", artifact.getArtifactType());
        Assert.assertNull(artifact.getRepositoryCredentials());
        Assert.assertNull(artifact.getRepositoryName());
        Assert.assertNull(artifact.getArtifactRepository());
        Assert.assertNull(artifact.getRepositoryURL());

        repositoryArtifact = archiveRoot.getTopology().getInputArtifacts().get("remote_war");
        Assert.assertEquals("alien4cloud:alien4cloud-ui:1.3.0-SM3", repositoryArtifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Deployment.War", repositoryArtifact.getArtifactType());
        Assert.assertEquals("good_user:real_secured_password", repositoryArtifact.getRepositoryCredentials());
        Assert.assertEquals("maven_repo", repositoryArtifact.getRepositoryName());
        Assert.assertEquals("maven", repositoryArtifact.getArtifactRepository());
        Assert.assertEquals("https://fastconnect.org/maven/content/repositories/fastconnect", repositoryArtifact.getRepositoryURL());

        artifact = archiveRoot.getTopology().getNodeTemplates().get("my_node").getArtifacts().get("simple_war");
        Assert.assertEquals("binary/myWar.war", artifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Deployment.War", artifact.getArtifactType());
        Assert.assertNull(artifact.getRepositoryCredentials());
        Assert.assertNull(artifact.getRepositoryName());
        Assert.assertNull(artifact.getArtifactRepository());
        Assert.assertNull(artifact.getRepositoryURL());

        repositoryArtifact = archiveRoot.getTopology().getNodeTemplates().get("my_node").getArtifacts().get("remote_war");
        Assert.assertEquals("alien4cloud:alien4cloud-ui:1.3.0-SM3", repositoryArtifact.getArtifactRef());
        Assert.assertEquals("tosca.artifacts.Deployment.War", repositoryArtifact.getArtifactType());
        Assert.assertEquals("good_user:real_secured_password", repositoryArtifact.getRepositoryCredentials());
        Assert.assertEquals("maven_repo", repositoryArtifact.getRepositoryName());
        Assert.assertEquals("maven", repositoryArtifact.getArtifactRepository());
        Assert.assertEquals("https://fastconnect.org/maven/content/repositories/fastconnect", repositoryArtifact.getRepositoryURL());
    }

    private ImplementationArtifact getImplementationArtifact(IndexedArtifactToscaElement component, String operation) {
        return component.getInterfaces().values().iterator().next().getOperations().get(operation).getImplementationArtifact();
    }

    private DeploymentArtifact getDeploymentArtifact(IndexedArtifactToscaElement component, String artifactName) {
        return component.getArtifacts().get(artifactName);
    }

    @Override
    protected String getRootDirectory() {
        return "src/test/resources/tosca/SimpleProfil_alien130/parsing/";
    }

    @Override
    protected String getToscaVersion() {
        return "alien_dsl_1_3_0";
    }
}
