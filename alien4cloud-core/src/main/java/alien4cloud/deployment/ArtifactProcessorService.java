package alien4cloud.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import alien4cloud.component.repository.ArtifactRepositoryConstants;
import alien4cloud.component.repository.CsarFileRepository;
import alien4cloud.component.repository.exception.CSARVersionNotFoundException;
import alien4cloud.deployment.exceptions.UnresolvableArtifactException;
import alien4cloud.model.components.AbstractArtifact;
import alien4cloud.model.components.DeploymentArtifact;
import alien4cloud.model.components.Interface;
import alien4cloud.model.components.Operation;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSRelationshipTemplate;
import alien4cloud.paas.model.PaaSTopologyDeploymentContext;
import alien4cloud.repository.services.RepositoryService;
import alien4cloud.utils.FileUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Download all artifacts before deployment
 */
@Slf4j
@Component
public class ArtifactProcessorService {

    @Resource
    private RepositoryService repositoryService;

    @Resource
    private CsarFileRepository repository;

    private Path tempDir;

    private InputStream resolveArtifact(AbstractArtifact artifact) {
        return repositoryService.resolveArtifact(artifact.getArtifactRef(), artifact.getRepositoryURL(), artifact.getArtifactRepository(),
                artifact.getRepositoryCredentials());
    }

    private void processLocalArtifact(AbstractArtifact artifact) {
        try {
            Path csarPath = repository.getExpandedCSAR(artifact.getArchiveName(), artifact.getArchiveVersion());
            artifact.setArtifactPath(csarPath.resolve(artifact.getArtifactRef()));
            if (!Files.exists(artifact.getArtifactPath())) {
                throw new UnresolvableArtifactException("Artifact could not be accessed " + artifact);
            }
        } catch (CSARVersionNotFoundException e) {
            throw new UnresolvableArtifactException("Artifact could not be found " + artifact, e);
        }
    }

    private void processArtifact(AbstractArtifact artifact) {
        if (ArtifactRepositoryConstants.ALIEN_ARTIFACT_REPOSITORY.equals(artifact.getArtifactRepository())) {
            // Overridden / Uploaded from Alien UI do not process
            // TODO this should be done on Alien's side, the orchestrator plugin does not have responsibility to retrieve artifact it-self
            return;
        }
        if (artifact.getRepositoryName() == null) {
            // Short notation
            try {
                // Test if it's an URL
                new URL(artifact.getArtifactRef());
            } catch (MalformedURLException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Processing local artifact {}", artifact);
                }
                // Not a URL then must be a relative path to a file inside the csar
                processLocalArtifact(artifact);
                return;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Processing remote artifact {}", artifact);
        }
        InputStream artifactStream = resolveArtifact(artifact);
        if (artifactStream == null) {
            throw new UnresolvableArtifactException("Artifact could not be found " + artifact);
        }
        try {
            Path tempArtifactPath = Files.createTempFile(tempDir, "artifact", "");
            Files.copy(artifactStream, tempArtifactPath, StandardCopyOption.REPLACE_EXISTING);
            if (log.isDebugEnabled()) {
                log.debug("Remote artifact from {} resolved to {}", artifact.getArtifactRef(), tempArtifactPath);
            }
            artifact.setArtifactPath(tempArtifactPath);
            artifact.setArtifactRef(tempArtifactPath.getFileName().toString());
        } catch (IOException e) {
            throw new UnresolvableArtifactException("Artifact could not be resolved " + artifact, e);
        } finally {
            try {
                artifactStream.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    private void processInterfaces(Map<String, Interface> interfaceMap) {
        if (interfaceMap != null) {
            for (Interface interfazz : interfaceMap.values()) {
                if (interfazz.getOperations() != null) {
                    for (Operation operation : interfazz.getOperations().values()) {
                        if (operation.getImplementationArtifact() != null) {
                            processArtifact(operation.getImplementationArtifact());
                        }
                    }
                }
            }
        }
    }

    private void processImplementationArtifacts(PaaSTopologyDeploymentContext deploymentContext) {
        if (deploymentContext.getPaaSTopology().getAllNodes() != null) {
            for (PaaSNodeTemplate paaSNodeTemplate : deploymentContext.getPaaSTopology().getAllNodes().values()) {
                processInterfaces(paaSNodeTemplate.getInterfaces());
                if (paaSNodeTemplate.getRelationshipTemplates() != null) {
                    for (PaaSRelationshipTemplate relationshipTemplate : paaSNodeTemplate.getRelationshipTemplates()) {
                        processInterfaces(relationshipTemplate.getInterfaces());
                    }
                }
            }
        }
    }

    private void processDeploymentArtifacts(PaaSTopologyDeploymentContext deploymentContext) {
        if (deploymentContext.getDeploymentTopology().getNodeTemplates() != null) {
            for (NodeTemplate nodeTemplate : deploymentContext.getDeploymentTopology().getNodeTemplates().values()) {
                if (nodeTemplate.getArtifacts() != null) {
                    for (DeploymentArtifact artifact : nodeTemplate.getArtifacts().values()) {
                        processArtifact(artifact);
                    }
                }
            }
        }
    }

    public void processArtifacts(PaaSTopologyDeploymentContext deploymentContext) {
        processImplementationArtifacts(deploymentContext);
        processDeploymentArtifacts(deploymentContext);
    }

    @Value("${directories.alien}/${directories.upload_temp}")
    public void setTempDir(String tempDir) throws IOException {
        this.tempDir = FileUtil.createDirectoryIfNotExists(tempDir);
    }
}
