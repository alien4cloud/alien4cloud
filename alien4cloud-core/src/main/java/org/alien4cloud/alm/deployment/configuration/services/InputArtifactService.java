package org.alien4cloud.alm.deployment.configuration.services;

import alien4cloud.component.repository.ArtifactRepositoryConstants;
import alien4cloud.component.repository.IFileRepository;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.topology.TopologyServiceCore;
import org.alien4cloud.alm.deployment.configuration.events.OnDeploymentConfigCopyEvent;
import org.alien4cloud.alm.deployment.configuration.model.AbstractDeploymentConfig;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentInputs;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.templates.Topology;
import org.apache.commons.collections4.MapUtils;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service responsible for management of input artifacts.
 */
@Service
public class InputArtifactService {
    @Resource
    private DeploymentConfigurationDao deploymentConfigurationDao;
    @Resource
    private IFileRepository artifactRepository;
    @Inject
    private TopologyServiceCore topologyServiceCore;

    /**
     * Update an input artifact value in the deployment topology
     *
     * @param environment The environment for which to update the input artifact.
     * @param topology The topology for which to update the artifact.
     * @param inputArtifactId The id of the input artifact to update.
     * @param artifactFile The file
     * @throws IOException
     */
    public void updateInputArtifact(ApplicationEnvironment environment, Topology topology, String inputArtifactId, MultipartFile artifactFile)
            throws IOException {
        checkInputArtifactExist(inputArtifactId, topology);
        DeploymentInputs deploymentInputs = getDeploymentInputs(environment.getTopologyVersion(), environment.getId());
        // FIXME ensure that deployment inputs are up-to date
        DeploymentArtifact artifact = getDeploymentArtifact(inputArtifactId, deploymentInputs);
        try (InputStream artifactStream = artifactFile.getInputStream()) {
            String artifactFileId = artifactRepository.storeFile(artifactStream);
            artifact.setArtifactName(artifactFile.getOriginalFilename());
            artifact.setArtifactRef(artifactFileId);
            artifact.setArtifactRepository(ArtifactRepositoryConstants.ALIEN_ARTIFACT_REPOSITORY);
            artifact.setRepositoryName(null);
            artifact.setRepositoryURL(null);
            deploymentConfigurationDao.save(deploymentInputs);
        }
    }

    public void updateInputArtifact(ApplicationEnvironment environment, Topology topology, String inputArtifactId, InputStream artifactStream, String filename)
            throws IOException {
        checkInputArtifactExist(inputArtifactId, topology);
        DeploymentInputs deploymentInputs = getDeploymentInputs(environment.getTopologyVersion(), environment.getId());
        // FIXME ensure that deployment inputs are up-to date
        DeploymentArtifact artifact = getDeploymentArtifact(inputArtifactId, deploymentInputs);
        String artifactFileId = artifactRepository.storeFile(artifactStream);
        artifact.setArtifactName(filename);
        artifact.setArtifactRef(artifactFileId);
        artifact.setArtifactRepository(ArtifactRepositoryConstants.ALIEN_ARTIFACT_REPOSITORY);
        artifact.setRepositoryName(null);
        artifact.setRepositoryURL(null);
        deploymentConfigurationDao.save(deploymentInputs);
    }

    public void updateInputArtifact(ApplicationEnvironment environment, Topology topology, String inputArtifactId, DeploymentArtifact updatedArtifact) {
        checkInputArtifactExist(inputArtifactId, topology);
        DeploymentInputs deploymentInputs = getDeploymentInputs(environment.getTopologyVersion(), environment.getId());

        DeploymentArtifact artifact = getDeploymentArtifact(inputArtifactId, deploymentInputs);

        artifact.setArtifactName(updatedArtifact.getArtifactName());
        artifact.setArtifactRef(updatedArtifact.getArtifactRef());
        artifact.setArtifactRepository(updatedArtifact.getArtifactRepository());
        artifact.setArtifactType(updatedArtifact.getArtifactType());
        artifact.setRepositoryName(updatedArtifact.getRepositoryName());
        artifact.setRepositoryURL(updatedArtifact.getRepositoryURL());
        artifact.setRepositoryCredential(updatedArtifact.getRepositoryCredential());
        artifact.setArchiveName(updatedArtifact.getArchiveName());
        artifact.setArchiveVersion(updatedArtifact.getArchiveVersion());

        deploymentConfigurationDao.save(deploymentInputs);
    }

    private DeploymentInputs getDeploymentInputs(String versionId, String environmentId) {
        DeploymentInputs deploymentInputs = deploymentConfigurationDao.findById(DeploymentInputs.class, AbstractDeploymentConfig.generateId(versionId, environmentId));
        if (deploymentInputs == null) {
            deploymentInputs = new DeploymentInputs(versionId, environmentId);
        }

        return deploymentInputs;
    }

    private void checkInputArtifactExist(String inputArtifactId, Topology topology) {
        if (topology.getInputArtifacts() == null || !topology.getInputArtifacts().containsKey(inputArtifactId)) {
            throw new NotFoundException("Input Artifact with key [" + inputArtifactId + "] doesn't exist within the topology.");
        }
    }

    private DeploymentArtifact getDeploymentArtifact(String inputArtifactId, DeploymentInputs inputs) {
        Map<String, DeploymentArtifact> artifacts = inputs.getInputArtifacts();
        if (artifacts == null) {
            artifacts = new HashMap<>();
            inputs.setInputArtifacts(artifacts);
        }
        DeploymentArtifact artifact = artifacts.get(inputArtifactId);
        if (artifact == null) {
            artifact = new DeploymentArtifact();
            artifacts.put(inputArtifactId, artifact);
        } else if (ArtifactRepositoryConstants.ALIEN_ARTIFACT_REPOSITORY.equals(artifact.getArtifactRepository())) {
            artifactRepository.deleteFile(artifact.getArtifactRef());
        }
        return artifact;
    }

    @EventListener
    @Order(11)
    public void onCopyConfiguration(OnDeploymentConfigCopyEvent onDeploymentConfigCopyEvent) {
        ApplicationEnvironment source = onDeploymentConfigCopyEvent.getSourceEnvironment();
        ApplicationEnvironment target = onDeploymentConfigCopyEvent.getTargetEnvironment();
        DeploymentInputs deploymentInputs = deploymentConfigurationDao.findById(DeploymentInputs.class,
                AbstractDeploymentConfig.generateId(source.getTopologyVersion(), source.getId()));

        if (deploymentInputs == null || MapUtils.isEmpty(deploymentInputs.getInputArtifacts())) {
            return; // Nothing to copy
        }

        Topology topology = topologyServiceCore.getOrFail(Csar.createId(target.getApplicationId(), target.getTopologyVersion()));

        if (MapUtils.isNotEmpty(topology.getInputArtifacts())) {
            Map<String, DeploymentArtifact> inputsArtifactsDefinitions = topology.getInputArtifacts();
            // Copy only artifacts which exists in the new topology's definition
            Map<String, DeploymentArtifact> inputsArtifactsToCopy = deploymentInputs.getInputArtifacts().entrySet().stream()
                    .filter(inputEntry -> inputsArtifactsDefinitions.containsKey(inputEntry.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            if (MapUtils.isNotEmpty(inputsArtifactsToCopy)) {
                // There's something to copy
                DeploymentInputs targetDeploymentInputs = deploymentConfigurationDao.findById(DeploymentInputs.class,
                        AbstractDeploymentConfig.generateId(target.getTopologyVersion(), target.getId()));
                if (targetDeploymentInputs == null) {
                    targetDeploymentInputs = new DeploymentInputs(target.getTopologyVersion(), target.getId());
                }

                targetDeploymentInputs.setInputArtifacts(inputsArtifactsToCopy);
                deploymentConfigurationDao.save(targetDeploymentInputs);
            }
        }
    }
}